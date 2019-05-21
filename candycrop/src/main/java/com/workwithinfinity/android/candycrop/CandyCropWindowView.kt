package com.workwithinfinity.android.candycrop


import android.content.Context
import android.graphics.*
import androidx.annotation.ColorInt
import android.util.AttributeSet
import android.view.*
import kotlin.math.roundToInt


/**
 * View that provides functions for loading a image and cropping it
 */
internal class CandyCropWindowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyles: Int = 0
) : View(context, attrs, defStyles) {
    /** bitmap storing the source image */
    private var mBitmap: Bitmap? = null
    /** Rect for the cropping window */
    private val mCropRect: Rect = Rect(0, 0, 0, 0)
    /** Bitmap for the overlay */
    private var mOverlayBitmap: Bitmap? = null
    /** Paint used to draw the hole in the overlay */
    private val mPaintDelete: Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    /** Paint used to draw the cropping window  */
    private val mPaintCropRect: Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    /** stores the X position of the touch event */
    private var mXTouch: Float = 0f
    /** stores the Y position of the touch event */
    private var mYTouch: Float = 0f
    /** true if the image is beeing moved */
    private var mImageMoving: Boolean = false
    /**Aspect ratio of the cropping window for the x dimension */
    private var mAspectRatioX: Int = 1
    /** Aspect ratio of the cropping window for the y dimension */
    private var mAspectRatioY: Int = 1
    /**Background color of the view*/
    @ColorInt
    private var mBackgroundColor: Int = Color.TRANSPARENT
    /** Color of the overlay */
    @ColorInt
    private var mOverlayColor: Int = Color.argb(150, 0, 0, 0)
    /** size of the cropping window. 1f=Full View 0.5f=Half the view */
    private var mCropSize: Float = 0.9f
    /** stores if the view is working in the background */
    private var mIsLoading: Boolean = false
    /** stores if the border should be drawn */
    private var mDrawBorder: Boolean = true
    /** Matrix used to move and scale the picture */
    private val mMatrix: Matrix = Matrix()
    /** Maximum possible scale factor*/
    private val MAX_SCALE_FACTOR = 30f
    /** The shape of the overlay **/
    private var mOverlayStyle : OverlayStyle = OverlayStyle.RECT


    /**
     * ScaleGestureDetector used to detect scale gestures
     */
    private val scaleGestureDetector = ScaleGestureDetector(context, object :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {

        /**
         * Called on the beginning of the scaling process
         * @param detector the scaling data
         */
        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            mImageMoving = false
            return true
        }

        /**
         * Called when scaling the image with gesture
         * @param detector the scaling data
         */
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            if (detector == null)
                return false

            val f = FloatArray(9)
            mMatrix.getValues(f)
            val sF = f[Matrix.MSCALE_X]
            val newSF = sF * detector.scaleFactor
            if(newSF>MAX_SCALE_FACTOR) {
                return false
            }

            mMatrix.postScale(detector.scaleFactor, detector.scaleFactor, detector.focusX, detector.focusY)
            snapToCropRect()
            invalidate()
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
        if (bm != null) {
            canvas.drawBitmap(bm, mMatrix, null)
        }
        if (obm != null) {
            canvas.drawBitmap(obm, 0f, 0f, null)
            if (mDrawBorder) {
                when(mOverlayStyle) {
                    OverlayStyle.CIRCLE -> canvas.drawCircle(mCropRect.exactCenterX(),mCropRect.exactCenterY(),mCropRect.height().toFloat()/2f,mPaintCropRect)
                    OverlayStyle.RECT -> canvas.drawRect(mCropRect, mPaintCropRect)

                }

            }
        }
        if (mIsLoading) {
            //make the view gray during loading
            canvas.drawColor(Color.argb(100, 0, 0, 0))
        }
    }

    /**
     * Called when the view size is changed
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateCropRect()
        createOverlayBitmap()
    }

    /**
     * Sets if the rect should be drawn
     * @param drawBorder true for draw else false
     */
    fun setDrawBorder(drawBorder: Boolean) {
        mDrawBorder = drawBorder
    }

    /**
     * Sets the background color of the view
     * @param color the desired color as ColorInt
     */
    fun setBgColor(@ColorInt color: Int) {
        mBackgroundColor = color
    }

    /**
     * Gets the background color of the view
     * @return the background color
     */
    fun getBgColor() = mBackgroundColor

    /**
     * sets the alpha of the overlay
     * @param color the desired color
     */
    fun setOverlayColor(@ColorInt color: Int) {
        mOverlayColor = color
    }

    /**
     * sets the shape of the overlay
     * @param style the desired shape
     */
    fun setOverlayStyle(style : OverlayStyle) {
        mOverlayStyle = style
    }

    /**
     * Sets the crop window size
     * @param size the desired size as a float between 0.5 and 1
     * 1 -> use the whole screen
     * 0.5 -> use half of the screen
     */
    fun setCropSize(size: Float) {
        if (size > 1)
            return
        if (size < 0.5)
            return
        mCropSize = size
        updateCropRect()
    }

    /**
     * Sets the loading state of the view
     * @param isLoading true if the view should be displayed in loading state
     */
    fun setLoading(isLoading: Boolean) {
        mIsLoading = isLoading
        invalidate()
    }


    /**
     * Gets the rect representing the cropping window
     * @return the cropping rect
     */
    fun getCropRect() = mCropRect

    override fun getMatrix() = mMatrix

    /**
     * Gets the source bitmap
     * @return the source bitmap
     */
    fun getBitmap() = mBitmap

    /**
     * Generates the mCropRect using the set aspect ratio and crop size
     */
    private fun updateCropRect() {
        val screenRect = Rect(0, 0, width, height)
        val targetWidth = width * mCropSize
        val targetHeight = height * mCropSize
        val dw = targetWidth / mAspectRatioX.toFloat()
        val dh = targetHeight / mAspectRatioY.toFloat()
        if (dw < dh) {
            val cropWidth = (targetWidth / 2f).roundToInt()
            val cropHeight = (mAspectRatioY * dw / 2f).roundToInt()
            mCropRect.set(
                screenRect.centerX() - cropWidth,
                screenRect.centerY() - cropHeight,
                screenRect.centerX() + cropWidth,
                screenRect.centerY() + cropHeight
            )
        } else {
            val cropWidth = (mAspectRatioX * dh / 2f).roundToInt()
            val cropHeight = (targetHeight / 2f).roundToInt()
            mCropRect.set(
                screenRect.centerX() - cropWidth,
                screenRect.centerY() - cropHeight,
                screenRect.centerX() + cropWidth,
                screenRect.centerY() + cropHeight
            )
        }
    }

    /**
     * Generates the overlay bitmap
     */
    private fun createOverlayBitmap() {
        val conf = Bitmap.Config.ARGB_8888
        val bm = Bitmap.createBitmap(width, height, conf)
        val canvas = Canvas(bm)
        canvas.drawColor(mOverlayColor)
        when(mOverlayStyle) {
            OverlayStyle.RECT -> canvas.drawRect(mCropRect, mPaintDelete)
            OverlayStyle.CIRCLE -> {
                canvas.drawCircle(mCropRect.exactCenterX(),mCropRect.exactCenterY(),mCropRect.height().toFloat()/2f,mPaintDelete)
            }
        }
        mOverlayBitmap = bm
    }

    /**
     * Sets the AspectRatio of the cropping window
     * @param x AspectRatio of the x dimension
     * @param y AspectRatio of the y dimension
     * Does nothing if x or y <= 0
     */
    fun setAspectRatio(x: Int, y: Int) {
        if (x <= 0 || y <= 0)
            return
        mAspectRatioX = x
        mAspectRatioY = y
        updateCropRect()
    }

    /**
     * Sets the bitmap as the source to crop
     * @param bitmap the bitmap to use
     */
    fun setBitmap(bitmap: Bitmap) {
        mBitmap = bitmap
        mMatrix.reset()
        //fitBitmapToView(bitmap, true)
        fitBitmapToCrop(bitmap,true)
        snapToCropRect()
        invalidate()
    }

    /**
     * Alligns the picture to the cropping rect if it would be out of bounds
     */
    private fun snapToCropRect() {
        val bm = mBitmap ?: return

        val bmRect = RectF(0f, 0f, bm.width.toFloat(), bm.height.toFloat())
        val tempRect = RectF()
        mMatrix.mapRect(tempRect, bmRect)
        if (tempRect.width() < mCropRect.width().toFloat() || tempRect.height() < mCropRect.height().toFloat()) {
            val dW = bm.width.toFloat() / mCropRect.width().toFloat()
            val dH = bm.height.toFloat() / mCropRect.height().toFloat()
            val sF =
                if (dW < dH) mCropRect.width().toFloat() / tempRect.width() else mCropRect.height().toFloat() / tempRect.height()
            mMatrix.postScale(sF, sF)
            mMatrix.mapRect(tempRect, bmRect)
        }

        if (tempRect.top > mCropRect.top.toFloat()) {
            val dY = mCropRect.top.toFloat() - tempRect.top
            mMatrix.postTranslate(0f, dY)
            mMatrix.mapRect(tempRect, bmRect)
        }
        if (tempRect.left > mCropRect.left.toFloat()) {
            val dX = mCropRect.left.toFloat() - tempRect.left
            mMatrix.postTranslate(dX, 0f)
            mMatrix.mapRect(tempRect, bmRect)
        }
        if (tempRect.bottom < mCropRect.bottom.toFloat()) {
            val dY = mCropRect.bottom.toFloat() - tempRect.bottom
            mMatrix.postTranslate(0f, dY)
            mMatrix.mapRect(tempRect, bmRect)
        }
        if (tempRect.right < mCropRect.right.toFloat()) {
            val dX = mCropRect.right.toFloat() - tempRect.right
            mMatrix.postTranslate(dX, 0f)
            mMatrix.mapRect(tempRect, bmRect)
        }
    }


    /**
     * Sets the scale factor for the image to fit the screen
     * @param bm the bitmap to fit to the view
     * @param mode how to fit the view
     * mode=true -> cropfit
     * mode=false -> complete fit
     */
    private fun fitBitmapToView(bm: Bitmap, mode: Boolean = true) {
        val dh = height / bm.height.toFloat()
        val dw = width / bm.width.toFloat()
        val scaleFactor = if ((dh < dw) == mode) {
            width / bm.width.toFloat()
        } else {
            height / bm.height.toFloat()
        }
        mMatrix.setScale(scaleFactor, scaleFactor,0f,0f)
        val imgRect = RectF(0f,0f,bm.width.toFloat(),bm.height.toFloat())
        mMatrix.mapRect(imgRect)
        mMatrix.postTranslate(mCropRect.exactCenterX()-imgRect.centerX(),mCropRect.exactCenterY()-imgRect.centerY())
    }

    /** Scales the image to fit in the crop rect
     * @param bm the bitmap to fit
     * @param mode how to fit the view
     * mode=true _> cropfit
     * mode=false -> complete fit
     */
    private fun fitBitmapToCrop(bm : Bitmap, mode: Boolean = true) {
        val dh = mCropRect.height() / bm.height.toFloat()
        val dw = mCropRect.width() / bm.width.toFloat()
        val scaleFactor = if((dh < dw) == mode) {
            mCropRect.width() / bm.width.toFloat()
        } else {
            mCropRect.height() / bm.height.toFloat()
        }
        mMatrix.setScale(scaleFactor,scaleFactor,0f,0f)
        val imgRect = RectF(0f,0f,bm.width.toFloat(),bm.height.toFloat())
        mMatrix.mapRect(imgRect)
        mMatrix.postTranslate(mCropRect.exactCenterX()-imgRect.centerX(),mCropRect.exactCenterY()-imgRect.centerY())
    }


    /**
     * handels touchscreen events
     * @param event the touch event data
     * @return if the event has been handled
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {

        scaleGestureDetector.onTouchEvent(event)
        //if scaling is in progress, don't move the picture
        if (scaleGestureDetector.isInProgress) {
            return true
        }

        val x = event.rawX
        val y = event.rawY

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                //save where the user started to move the image
                mXTouch = x
                mYTouch = y
                mImageMoving = true //is set to false when scaling process starts
                true
            }
            MotionEvent.ACTION_MOVE -> {
                //move the image according to finger position
                if (mImageMoving) {
                    mMatrix.postTranslate(x - mXTouch, y - mYTouch)
                    mXTouch = x
                    mYTouch = y
                    snapToCropRect()
                    invalidate()
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