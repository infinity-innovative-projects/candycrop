package com.workwithinfinity.candycrop

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.opengl.GLES10
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.lang.ref.WeakReference
import kotlin.math.roundToInt


/**
 * View that provides functions for loading a image and cropping it
 */
class CandyCropView(context : Context,attrs : AttributeSet? = null) : View(context,attrs) {
    /** bitmap storing the source image */
    private var mBitmap : Bitmap? = null
    /** Rect used to draw the image to the view */
    private val mDestinationRect = Rect(0,0,0,0)
    /** Rect used to draw the image to the view */
    private val mSourceRect : Rect = Rect(0,0,0,0)
    /** X Position of the image */
    private var mRenderPositionX : Int = 0
    /** Y Position of the image */
    private var mRenderPositionY : Int = 0
    /** Scalefactor of the image */
    private var mScaleFactor : Float = 1f
    /** Rect for the cropping window */
    private val mCropRect : Rect = Rect(0,0,0,0)
    /** Bitmap for the overlay */
    private var mOverlayBitmap : Bitmap? = null
    /** Paint used to draw the hole in the overlay */
    private val mPaintDelete : Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    /** Paint used to draw the cropping window  */
    private val mPaintCropRect : Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    /** stores the X position of the touch event */
    private var mXTouch : Int = 0
    /** stores the Y position of the touch event */
    private var mYTouch : Int = 0
    /** true if the image is beeing moved */
    private var mImageMoving : Boolean = false
    /** Uri of the source image */
    private var mUri : Uri? = null
    /** Listener that will be called after cropping the image */
    private var mOnCropCompleteListener : OnCropCompleteListener? = null
    /** Listener that will be called after loading the image */
    private var mOnLoadUriImageCompleteListener : OnLoadUriImageCompleteListener? = null
    /** Uri where the cropped image should be saved to */
    private var mResultUri : Uri? = null
    /** WorkerTask used to crop the image async */
    private var mCandyCropWorkerTask : WeakReference<CandyCropWorkerTask>? = null
    /** Width of the final cropped image in pixel*/
    private var mResultWidth = 1024
    /** Height of the final cropped image in pixel */
    private var mResultHeight = 1024
    /** WorkerTask used to load the image async */
    private var mCandyUriLoadWorkerTask : WeakReference<CandyUriLoadWorkerTask>? = null
    /**Aspect ratio of the cropping window for the x dimension */
    private var mAspectRatioX : Int = 1
    /** Aspect ratio of the cropping window for the y dimension */
    private var mAspectRatioY : Int = 1
    /**Background color of the view*/
    @ColorInt private var mBackgroundColor : Int = Color.TRANSPARENT
    /** Alpha of the overlay. 0-255 */
    private var mOverlayAlpha : Int = 150
    /** size of the cropping window. 1f=Full View 0.5f=Half the view */
    private var mCropSize : Float = 0.9f

    /**
     * ScaleGestureDetector used to detect scale gestures
     */
    private val scaleGestureDetector = ScaleGestureDetector(context,object :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {

        /**
         * Called on the beginning of the scaling process
         * @param detector the scaling data
         */
        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            mImageMoving=false
            return true
        }

        /**
         * Called when scaling the image with gesture
         * @param detector the scaling data
         */
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            if(detector==null)
                return false
            setScaleFactor(mScaleFactor * detector.scaleFactor)
            return true
        }
    })

    /**
     * Draws the view
     */
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(mBackgroundColor)
        val bm = mBitmap ?: return
        val obm = mOverlayBitmap ?: return
        canvas.drawBitmap(bm,mSourceRect,mDestinationRect,null)
        canvas.drawBitmap(obm,0f,0f,null)
        canvas.drawRect(mCropRect,mPaintCropRect)
    }

    /**
     * Called when the view size is changed
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateCropRect()
        createOverlayBitmap()
        updateRects()
    }

    /**
     * Sets the backgroundcolor of the view
     * @param color the desired color as ColorInt
     */
    override fun setBackgroundColor(@ColorInt color : Int) {
        mBackgroundColor = color
    }

    /**
     * sets the alpha of the overlay
     * @param alpha the desired alpha ranging from 0 to 255
     */
    fun setOverlayAlpha(alpha : Int) {
        if(alpha<0 || alpha > 255)
            return
        mOverlayAlpha = alpha
    }

    /**
     * Sets the crop window size
     * @param size the desired size as a float between 0.5 and 1
     * 1 -> use the whole screen
     * 0.5 -> use half of the screen
     */
    fun setCropSize(size : Float) {
        if(size>1)
            return
        if(size<0.5)
            return
        mCropSize = size
        updateCropRect()
    }

    /**
     * Generates the mCropRect using the set aspect ratio and crop size
     */
    private fun updateCropRect() {
        val screenRect = Rect(0,0,width,height)
        val targetWidth = width*mCropSize
        val targetHeight = height*mCropSize
        val dw = targetWidth/mAspectRatioX.toFloat()
        val dh = targetHeight/mAspectRatioY.toFloat()
        if(dw<dh) {
            val cropWidth = (targetWidth/2f).roundToInt()
            val cropHeight = (mAspectRatioY * dw/2f).roundToInt()
            mCropRect.set(screenRect.centerX()-cropWidth,screenRect.centerY()-cropHeight,screenRect.centerX()+cropWidth,screenRect.centerY()+cropHeight)
        } else {
            val cropWidth = (mAspectRatioX * dh/2f).roundToInt()
            val cropHeight = (targetHeight/2f).roundToInt()
            mCropRect.set(screenRect.centerX()-cropWidth,screenRect.centerY()-cropHeight,screenRect.centerX()+cropWidth,screenRect.centerY()+cropHeight)
        }
    }

    /**
     * Generates the overlay bitmap
     */
    private fun createOverlayBitmap() {
        val conf = Bitmap.Config.ARGB_8888
        val bm = Bitmap.createBitmap(width,height,conf)
        val canvas = Canvas(bm)
        val c = Color.argb(mOverlayAlpha,0,0,0)
        canvas.drawColor(c)
        canvas.drawRect(mCropRect,mPaintDelete)
        mOverlayBitmap = bm
    }

    /**
     * Sets the AspectRatio of the cropping window
     * @param x AspectRatio of the x dimension
     * @param y AspectRatio of the y dimension
     * Does nothing if x or y <= 0
     */
    fun setAspectRatio(x : Int, y : Int) {
        if(x<=0 || y<=0)
            return
        mAspectRatioX = x
        mAspectRatioY = y
        updateCropRect()
    }

    /**
     * Sets the bitmap as the source to crop
     * @param bitmap the bitmap to use
     */
    fun setBitmap(bitmap : Bitmap) {
        mBitmap = bitmap
        mRenderPositionX=0
        mRenderPositionY=0
        fitBitmapToView(bitmap,true)
        snapToCropRect()
        updateRects()
    }

    /**
     * Calculates values for mSourceRect and mDestinationRect used to draw the view
     */
    private fun updateRects() {
        val bm = mBitmap ?: return
        val scaledWidth = (bm.width * mScaleFactor).roundToInt()
        val scaledHeight = (bm.height * mScaleFactor).roundToInt()
        mSourceRect.set(0,0,bm.width,bm.height)
        mDestinationRect.set(mRenderPositionX,mRenderPositionY,scaledWidth+mRenderPositionX,scaledHeight+mRenderPositionY)
        this.invalidate()
    }

    /**
     * Alligns the picture to the cropping rect if it would be out of bounds
     */
    private fun snapToCropRect() {
        val bm = mBitmap ?: return

        val bmScaledWidth = bm.width * mScaleFactor
        val bmScaledHeight = bm.height * mScaleFactor

        if(mRenderPositionX+bmScaledWidth<mCropRect.right) {
            mRenderPositionX=(mCropRect.right-bmScaledWidth).toInt()
        }

        if(mRenderPositionY+bmScaledHeight<mCropRect.bottom) {
            mRenderPositionY=(mCropRect.bottom-bmScaledHeight).toInt()
        }

        if(mRenderPositionX>mCropRect.left) {
            mRenderPositionX=mCropRect.left
        }

        if(mRenderPositionY>mCropRect.top) {
            mRenderPositionY=mCropRect.top
        }
    }

    /**
     * Sets the scalefactor of the image
     * @param factor the desirec scalefactor.
     * 1 -> No Scaling
     * >1 -> Zoom in
     * <1 -> Zoom out
     * ignored if the factor is < 0.01
     */
    private fun setScaleFactor(factor : Float) {
        if(factor<0.01f)
            return
        val bm = mBitmap ?: return

        val oldWidth = bm.width * mScaleFactor
        val oldHeight = bm.height * mScaleFactor

        mScaleFactor = when {
            bm.width * factor < mCropRect.width() -> mCropRect.width()/bm.width.toFloat()
            bm.height * factor < mCropRect.height() -> mCropRect.height()/bm.height.toFloat()
            else -> factor
        }

        val newWidth = bm.width *mScaleFactor
        val newHeight = bm.height*mScaleFactor

        mRenderPositionX+=((oldWidth-newWidth)/2f).roundToInt()
        mRenderPositionY+=((oldHeight-newHeight)/2f).roundToInt()

        snapToCropRect()
        updateRects()
    }

    /**
     * Sets the scale factor for the image to fit the screen
     * @param bm the bitmap to fit to the view
     * @param mode how to fit the view
     * mode=true -> cropfit
     * mode=false -> complete fit
     */
    private fun fitBitmapToView(bm : Bitmap,mode : Boolean = true) {
        val dh = height/bm.height.toFloat()
        val dw = width/bm.width.toFloat()
        mScaleFactor = if((dh<dw)==mode) {
            width/bm.width.toFloat()
        } else {
            height/bm.height.toFloat()
        }
    }

    /**
     * Sets the size of the final cropped image
     * @param width the width of the image
     * @param height the height of the image
     */
    fun setResultSize(width : Int, height: Int) {
        if(width<=0 || height <=0) {
            return
        }
        mResultWidth = width
        mResultHeight = height
    }

    /**
     * Loads the image from the given Uri and sets it as the cropping source
     * @param uri uri of the source image
     */
    fun setImageUriAsync(uri : Uri) {
        mUri = uri
        val currentTask = mCandyUriLoadWorkerTask?.get()
        currentTask?.cancel(true)
        mCandyUriLoadWorkerTask = WeakReference(CandyUriLoadWorkerTask(uri,WeakReference(this)))
        mCandyUriLoadWorkerTask?.get()?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Sets the OnLoadUriImageCompleteListener
     * Will be called after the image is loaded from the uri
     * @param listener the OnLoadUriImageCompleteListener
     */
    fun setOnLoadUriImageCompleteListener(listener : OnLoadUriImageCompleteListener?) {
        mOnLoadUriImageCompleteListener = listener
    }

    /**
     * called after cropping the image
     * @param result the crop result
     */
    fun onCroppingBitmapComplete(result : CropResult) {
        mOnCropCompleteListener?.onCropComplete(result)
    }

    /**
     * sets the OnCropCompleteListener
     * will be called after the cropping is complete
     * @param listener the OnCropCompleteListener
     */
    fun setOnCropCompleteListener(listener : OnCropCompleteListener?) {
        mOnCropCompleteListener = listener
    }

    /**
     * Starts the cropping process.
     * Result will be communicated to the OnCropCompleteListener set with setOnCropCompleteListener
     */
    fun getCroppedBitmapAsync() {
        val bm = mBitmap ?: return
        val currentTask = mCandyCropWorkerTask?.get()
        currentTask?.cancel(true)
        mCandyCropWorkerTask = WeakReference(CandyCropWorkerTask(
            source =bm,
            sourceUri = mUri,
            destUri = mResultUri,
            cropRect = mCropRect,
            scaleFactor = mScaleFactor,
            positionX = mRenderPositionX,
            positionY = mRenderPositionY,
            useFilter = true,
            resultWidth = mResultWidth,
            resultHeight = mResultHeight,
            backgroundColor = mBackgroundColor,
            view = WeakReference(this)))
        mCandyCropWorkerTask?.get()?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * sets the uri where the cropped result should be saved to
     * @param uri the uri to save to
     */
    fun setResultUri(uri : Uri?) {
        mResultUri = uri
    }

    /**
     * called when the image is loaded
     * @result the loaded image
     */
    fun onLoadUriImageComplete(result : CandyUriLoadWorkerTask.UriLoadResult) {
        mOnLoadUriImageCompleteListener?.onLoadUriImageComplete(result.bitmap,result.uri)
        setBitmap(result.bitmap)
    }

    /**
     * handels touchscreen events
     * @param event the touch event data
     * @return if the event has been handled
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {

        scaleGestureDetector.onTouchEvent(event)
        //if scaling is in progress, don't move the picture
        if(scaleGestureDetector.isInProgress) {
            return true
        }

        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        return when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                //save where the user started to move the image
                mXTouch = x
                mYTouch = y
                mImageMoving = true //is set to false when scaling process starts
                true
            }
            MotionEvent.ACTION_MOVE -> {
                //move the image according to finger position
                if(mImageMoving) {
                    mRenderPositionX += x - mXTouch
                    mRenderPositionY += y - mYTouch
                    mXTouch = x
                    mYTouch = y
                    snapToCropRect()
                    updateRects()

                } else {
                    mXTouch = x
                    mYTouch = y
                    mImageMoving = true
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                mImageMoving = false
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    /**
     * Interface for OnCropCompleteListener.
     * Classes that implement this interface can be used for setOnCropCompleteListener
     */
    interface OnCropCompleteListener {
        fun onCropComplete(result : CropResult)
    }

    /**
     * Interface for OnLoadUriImageCompleteListener
     * Classes that implement this interface can be used for setOnLoadUriImageCompleteListener
     */
    interface OnLoadUriImageCompleteListener {
        fun onLoadUriImageComplete(result : Bitmap, uri: Uri)
    }

    /**
     * Containerclass for the cropping result
     * originalBitmap - the source bitmap
     * originalUri - the uri of the source bitmap
     * croppedBitmap - the cropped bitmap
     * croppedUri - the uri of the cropped bitmap
     */
    data class CropResult(val originalBitmap : Bitmap?,
                          val originalUri : Uri?,
                          val croppedBitmap : Bitmap?,
                          val croppedUri : Uri?)

}