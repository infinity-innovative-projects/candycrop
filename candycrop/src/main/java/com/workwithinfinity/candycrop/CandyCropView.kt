package com.workwithinfinity.candycrop

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.opengl.GLES10
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.lang.ref.WeakReference
import kotlin.math.roundToInt



class CandyCropView(context : Context,attrs : AttributeSet? = null) : View(context,attrs) {
    private val TAG = "CroppingView"
    private var mBitmap : Bitmap? = null
    private val mDestinationRect = Rect(0,0,0,0)
    private val mSourceRect : Rect = Rect(0,0,0,0)
    private var mRenderPositionX : Int = 0
    private var mRenderPositionY : Int = 0
    private var mScaleFactor : Float = 1f
    private val mCropRect : Rect = Rect(0,0,0,0)
    private var mOverlayBitmap : Bitmap? = null
    private val mPaintDelete : Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val mPaintCropRect : Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private var mXTouch : Int = 0
    private var mYTouch : Int = 0
    private var mImageMoving : Boolean = false
    private var mUri : Uri? = null
    private var mOnCropCompleteListener : OnCropCompleteListener? = null
    private var mOnLoadUriImageCompleteListener : OnLoadUriImageCompleteListener? = null
    private var mResultUri : Uri? = null
    private var mCandyCropWorkerTask : WeakReference<CandyCropWorkerTask>? = null
    private var mResultWidth = 100
    private var mResultHeight = 100
    private var mCandyUriLoadWorkerTask : WeakReference<CandyUriLoadWorkerTask>? = null
    private var mAspectRatioX : Int = 1
    private var mAspectRatioY : Int = 1
    @ColorInt private var mBackgroundColor : Int = Color.TRANSPARENT
    private var mOverlayAlpha : Int = 150
    private var mCropSize : Float = 0.9f

    private val scaleGestureDetector = ScaleGestureDetector(context,object :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            mImageMoving=false
            return true
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            if(detector==null)
                return false
            setScaleFactor(mScaleFactor * detector.scaleFactor)
            return true
        }
    })

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(mBackgroundColor)
        val bm = mBitmap ?: return
        val obm = mOverlayBitmap ?: return
        canvas.drawBitmap(bm,mSourceRect,mDestinationRect,null)
        canvas.drawBitmap(obm,0f,0f,null)
        canvas.drawRect(mCropRect,mPaintCropRect)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateCropRect()
        createOverlayBitmap()
        updateRects()
    }

    override fun setBackgroundColor(@ColorInt color : Int) {
        mBackgroundColor = color
    }

    fun setOverlayAlpha(alpha : Int) {
        if(alpha<0 || alpha > 255)
            return
        mOverlayAlpha = alpha
    }

    fun setCropSize(size : Float) {
        if(size>1)
            return
        if(size<0.5)
            return
        mCropSize = size
        updateCropRect()
    }

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

    private fun createOverlayBitmap() {
        val conf = Bitmap.Config.ARGB_8888
        mOverlayBitmap = Bitmap.createBitmap(width,height,conf)
        val canvas = Canvas(mOverlayBitmap)
        val c = Color.argb(mOverlayAlpha,0,0,0)
        canvas.drawColor(c)
        canvas.drawRect(mCropRect,mPaintDelete)
    }

    fun setAspectRatio(x : Int, y : Int) {
        if(x<=0 || y<=0)
            return
        mAspectRatioX = x
        mAspectRatioY = y
        updateCropRect()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d(TAG,"onMeasure: $widthMeasureSpec $heightMeasureSpec")
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setBitmap(bitmap : Bitmap) {
        mBitmap = bitmap
        mRenderPositionX=0
        mRenderPositionY=0
        fitBitmapToView(bitmap,true)
        snapToCropRect()
        updateRects()
    }

    private fun updateRects() {
        val bm = mBitmap ?: return
        val scaledWidth = (bm.width * mScaleFactor).roundToInt()
        val scaledHeight = (bm.height * mScaleFactor).roundToInt()
        mSourceRect.set(0,0,bm.width,bm.height)
        mDestinationRect.set(mRenderPositionX,mRenderPositionY,scaledWidth+mRenderPositionX,scaledHeight+mRenderPositionY)
        this.invalidate()
    }

    //scales the picture to fill the crop if its to small and move it if its out of bounds
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

    fun setResultSize(width : Int, height: Int) {
        if(width<=0 || height <=0) {
            return
        }
        mResultWidth = width
        mResultHeight = height
    }

    fun setImageUriAsync(uri : Uri) {
        mUri = uri
        val currentTask = mCandyUriLoadWorkerTask?.get()
        currentTask?.cancel(true)
        mCandyUriLoadWorkerTask = WeakReference(CandyUriLoadWorkerTask(uri,WeakReference(this)))
        mCandyUriLoadWorkerTask?.get()?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Sets the source Uri. Use setImageUriAsync instead
     */
    fun setImageUri(uri : Uri) {
        mUri = uri

        val bm = MediaStore.Images.Media.getBitmap(context.contentResolver,uri)
        val maxSize=GLES10.GL_MAX_TEXTURE_SIZE
        val resizedBm = if(bm.width > maxSize || bm.height > maxSize) {
            val dx = maxSize/bm.width.toFloat()
            val dy = maxSize/bm.height.toFloat()
            if(dx<dy) {
                Bitmap.createScaledBitmap(bm,maxSize,(bm.height*dx).roundToInt(),true)
            } else {
                Bitmap.createScaledBitmap(bm,(bm.width*dy).roundToInt(),maxSize,true)
            }
        } else {
            bm
        }

        setBitmap(resizedBm)
    }

    fun setOnLoadUriImageCompleteListener(listener : OnLoadUriImageCompleteListener?) {
        mOnLoadUriImageCompleteListener = listener
    }

    fun onCroppingBitmapComplete(result : CropResult) {
        mOnCropCompleteListener?.onCropComplete(result)
    }

    fun setOnCropCompleteListener(listener : OnCropCompleteListener?) {
        mOnCropCompleteListener = listener
    }

    fun getCroppedBitmapAsync() {
        val bm = mBitmap ?: return
        val currentTask = mCandyCropWorkerTask?.get()
        currentTask?.cancel(true)
        mCandyCropWorkerTask = WeakReference(CandyCropWorkerTask(bm,mUri,mResultUri,mCropRect,mScaleFactor,mRenderPositionX,mRenderPositionY,true,mResultWidth,mResultHeight,
            WeakReference(this)))
        mCandyCropWorkerTask?.get()?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun setResultUri(uri : Uri?) {
        mResultUri = uri
    }

    fun onLoadUriImageComplete(result : CandyUriLoadWorkerTask.UriLoadResult) {
        mOnLoadUriImageCompleteListener?.onLoadUriImageComplete(result.bitmap,result.uri)
        setBitmap(result.bitmap)
    }

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
                mImageMoving = true
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

    interface OnCropCompleteListener {
        fun onCropComplete(result : CropResult)
    }

    interface OnLoadUriImageCompleteListener {
        fun onLoadUriImageComplete(result : Bitmap, uri: Uri)
    }
    data class CropResult(val originalBitmap : Bitmap?,
                          val originalUri : Uri?,
                          val croppedBitmap : Bitmap?,
                          val croppedUri : Uri?)

}