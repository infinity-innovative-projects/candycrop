package com.workwithinfinity.android.candycrop


import android.content.Context
import android.graphics.*
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.view.*
import kotlin.math.roundToInt


/**
 * View that provides functions for loading a image and cropping it
 */
internal class CandyCropWindowView @JvmOverloads constructor(context : Context, attrs : AttributeSet? = null,defStyles : Int = 0) : View(context,attrs,defStyles) {
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
    /** stores if the view is working in the background */
    private var mIsLoading : Boolean = false


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
     * @param canvas the canvas to draw on
     */
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(mBackgroundColor)
        val bm = mBitmap
        val obm = mOverlayBitmap
        if(bm!=null) {
            canvas.drawBitmap(bm,mSourceRect,mDestinationRect,null)
        }
        if(obm!=null) {
            canvas.drawBitmap(obm,0f,0f,null)
            canvas.drawRect(mCropRect,mPaintCropRect)
        }
        if(mIsLoading) {
            //make the view gray during loading
            canvas.drawColor(Color.argb(100,0,0,0))
        }

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
     * Sets the background color of the view
     * @param color the desired color as ColorInt
     */
    fun setBgColor(@ColorInt color : Int) {
        mBackgroundColor = color
    }

    /**
     * Gets the background color of the view
     * @return the background color
     */
    fun getBgColor() = mBackgroundColor

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
     * Sets the loading state of the view
     * @param isLoading true if the view should be displayed in loading state
     */
    fun setLoading(isLoading : Boolean) {
        mIsLoading = isLoading
        invalidate()
    }

    /**
     * Gets the scale factor
     * @return the scale factor
     */
    fun getScaleFactor() = mScaleFactor

    /**
     * Gets the rect representing the cropping window
     * @return the cropping rect
     */
    fun getCropRect() = mCropRect

    /**
     * Gets the source bitmap
     * @return the source bitmap
     */
    fun getBitmap() = mBitmap

    /**
     * Gets the X position of the loaded image
     * @return the x position
     */
    fun getRenderPositionX() = mRenderPositionX

    /**
     * Gets the Y position of the loaded image
     * @return the y position
     */
    fun getRenderPositionY() = mRenderPositionY


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

}