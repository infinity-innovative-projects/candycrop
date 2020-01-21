package com.workwithinfinity.android.candycrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.workwithinfinity.android.R
import java.lang.ref.WeakReference

class CandyCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    /** The CandyCropWindowView */
    private val mCropView: CandyCropWindowView
    /** The ProgressBar */
    private val mProgressBar: ProgressBar
    /** Listener that will be called after cropping the image */
    private var mOnCropCompleteListener: OnCropCompleteListener? = null
    /** Listener that will be called after loading the image */
    private var mOnLoadUriImageCompleteListener: OnLoadUriImageCompleteListener? = null
    /** Width of the final cropped image in pixel*/
    private var mResultWidth = 1024
    /** Height of the final cropped image in pixel */
    private var mResultHeight = 1024
    /** The initial rotation */
    private var mRotation = 0f
    /** Uri of the source image */
    private var mUri: Uri? = null
    /** Uri where the cropped image should be saved to */
    private var mResultUri: Uri? = null
    /** WorkerTask used to crop the image async */
    private var mCandyCropWorkerTask: WeakReference<CandyCropWorkerTask>? = null
    /** WorkerTask used to load the image async */
    private var mCandyUriLoadWorkerTask: WeakReference<CandyUriLoadWorkerTask>? = null
    /** Saves the desired quality of the saved picture */
    private var mResultQuality: Int = 95
    /** Saves the desired format of the saved picture */
    private var mResultFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG

    init {
        inflate(context, R.layout.crop_view_layout, this)
        mCropView = findViewById(R.id.candyCropWindowView)
        mProgressBar = findViewById(R.id.progressBar)
        mProgressBar.visibility = View.GONE
        if (attrs != null) {
            val at = context.obtainStyledAttributes(attrs, R.styleable.CandyCropView)
            mCropView.setBgColor(at.getColor(R.styleable.CandyCropView_bg_color, Color.TRANSPARENT))
            mCropView.setCropSize(at.getFloat(R.styleable.CandyCropView_crop_size, 0.9f))
            val aX = at.getInteger(R.styleable.CandyCropView_crop_aspect_ratio_x, 1)
            val aY = at.getInteger(R.styleable.CandyCropView_crop_aspect_ratio_y, 1)
            mCropView.setAspectRatio(aX, aY)
            mCropView.setOverlayColor(
                at.getColor(
                    R.styleable.CandyCropView_overlay_color,
                    Color.argb(150, 0, 0, 0)
                )
            )
            mCropView.setDrawBorder(at.getBoolean(R.styleable.CandyCropView_draw_border, true))
            mCropView.setUseAnimation(at.getBoolean(R.styleable.CandyCropView_use_animation, true))
            mCropView.setAllowGestureRotation(
                at.getBoolean(
                    R.styleable.CandyCropView_allow_gesture_rotation,
                    false
                )
            )
            val circleShape = at.getBoolean(R.styleable.CandyCropView_use_circle_overlay, false)
            mCropView.setOverlayStyle(if (circleShape) OverlayStyle.CIRCLE else OverlayStyle.RECT)
            at.recycle()
        }
        mCropView.setOnInvalidate { this.invalidate() }
    }

    /**
     * Sets the background color of the view
     * @param color the desired background color
     */
    fun setBgColor(@ColorInt color: Int) {
        mCropView.setBgColor(color)
    }

    /**
     * Rotates the image 90 degree clockwise
     */
    fun rotateForward() {
        mCropView.rotateForward()
    }

    /**
     * Rotates the image 90 degree counterclockwise
     */
    fun rotateBackward() {
        mCropView.rotateBackwards()
    }

    /**
     * Sets if gesture rotation is enabled
     * @param allow true for enabled
     */
    fun setAllowGestureRotation(allow: Boolean) {
        mCropView.setAllowGestureRotation(allow)
    }

    /**
     * Sets if animation should be used
     * @param useAnimation true if animation should be used
     */
    fun setUseAnimation(useAnimation: Boolean) {
        mCropView.setUseAnimation(useAnimation)
    }

    /**
     * sets the alpha of the overlay
     * @param color the desired color of the overlay
     */
    fun setOverlayColor(@ColorInt color: Int) {
        mCropView.setOverlayColor(color)
    }

    /**
     * Sets if the border should be drawn
     * @param drawBorder true for draw else false
     */
    fun setDrawBorder(drawBorder: Boolean) {
        mCropView.setDrawBorder(drawBorder)
    }

    /**
     * Sets the crop window size
     * @param size the desired size as a float between 0.5 and 1
     * 1 -> use the whole screen
     * 0.5 -> use half of the screen
     */
    fun setCropSize(size: Float) {
        mCropView.setCropSize(size)
    }

    /**
     * Sets the desired format of the saved picture
     * @param format the format
     */
    fun setFormat(format: Bitmap.CompressFormat) {
        mResultFormat = format
    }

    /**
     * Sets the style of the overlay
     * @param style the desired style (RECT | CIRCLE)
     */
    fun setOverlayStyle(style: OverlayStyle) {
        mCropView.setOverlayStyle(style);
    }

    /**
     * Sets the desired quality of the saved picture
     * @param quality the quality between 0 and 100. Defaults to 95 if invalid
     */
    fun setQuality(quality: Int) {
        mResultQuality = if (quality in 0..100) quality else 95
    }


    /**
     * Sets the initial rotation of the image
     * @param rotation the rotation
     */
    fun setInitialRotation(rotation: Float) {
        mRotation = rotation
    }

    /**
     * Sets the AspectRatio of the cropping window
     * @param x AspectRatio of the x dimension
     * @param y AspectRatio of the y dimension
     * Does nothing if x or y <= 0
     */
    fun setAspectRatio(x: Int, y: Int) {
        mCropView.setAspectRatio(x, y)
    }

    /**
     * Sets the bitmap as the source to crop
     * @param bitmap the bitmap to use
     */
    fun setBitmap(bm: Bitmap) {
        mCropView.setBitmap(bm)
    }

    /**
     * Sets the uri where the cropped result should be saved to
     * @param uri the desired uri
     */
    fun setResultUri(uri: Uri?) {
        mResultUri = uri
    }

    /**
     * Sets the size of the final cropped image
     * @param width the width of the image
     * @param height the height of the image
     */
    fun setResultSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            return
        }
        mResultWidth = width
        mResultHeight = height
    }

    /**
     * Shows the progress bar and sets the CropView to loading state
     */
    private fun startLoading() {
        mCropView.setLoading(true)
        mProgressBar.visibility = View.VISIBLE
    }

    /**
     * Hides the progress bar and sets the CropView to not loading state
     */
    private fun doneLoading() {
        mCropView.setLoading(false)
        mProgressBar.visibility = View.GONE
    }

    /**
     * called when the image is loaded
     * @result the loaded image
     */
    fun onLoadUriImageComplete(result: CandyUriLoadWorkerTask.UriLoadResult) {
        if (result.bitmap != null) {
            mOnLoadUriImageCompleteListener?.onLoadUriImageComplete(result.bitmap, result.uri)
            setBitmap(result.bitmap)
        } else {
            mOnLoadUriImageCompleteListener?.onLoadUriImageError(
                result.uri,
                result.error ?: Exception("Unknown Exception")
            )
        }

        doneLoading()


    }

    /**
     * sets the OnCropCompleteListener
     * will be called after the cropping is complete
     * @param listener the OnCropCompleteListener
     */
    fun setOnCropCompleteListener(listener: OnCropCompleteListener?) {
        mOnCropCompleteListener = listener
    }

    /**
     * Sets the OnLoadUriImageCompleteListener
     * Will be called after the image is loaded from the uri
     * @param listener the OnLoadUriImageCompleteListener
     */
    fun setOnLoadUriImageCompleteListener(listener: OnLoadUriImageCompleteListener?) {
        mOnLoadUriImageCompleteListener = listener
    }

    /**
     * Starts the cropping process.
     * Result will be communicated to the OnCropCompleteListener set with setOnCropCompleteListener
     */
    fun getCroppedBitmapAsync() {
        val bm = mCropView.getBitmap() ?: return
        val currentTask = mCandyCropWorkerTask?.get()
        currentTask?.cancel(true)
        mCandyCropWorkerTask = WeakReference(
            CandyCropWorkerTask(
                source = bm,
                sourceUri = mUri,
                destUri = mResultUri,
                cropRect = mCropView.getCropRect(),
                matrix = mCropView.matrix,
                useFilter = true,
                resultWidth = mResultWidth,
                resultHeight = mResultHeight,
                backgroundColor = mCropView.getBgColor(),
                view = WeakReference(this),
                quality = mResultQuality,
                format = mResultFormat
            )
        )
        startLoading()
        mCandyCropWorkerTask?.get()?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Loads the image from the given Uri and sets it as the cropping source
     * @param uri uri of the source image
     */
    fun setImageUriAsync(uri: Uri) {
        mUri = uri
        val currentTask = mCandyUriLoadWorkerTask?.get()
        currentTask?.cancel(true)
        mCandyUriLoadWorkerTask = WeakReference(
            CandyUriLoadWorkerTask(
                uri,
                WeakReference(this),
                mRotation
            )
        )
        startLoading()
        mCandyUriLoadWorkerTask?.get()?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * called after cropping the image
     * @param result the crop result
     */
    fun onCroppingBitmapComplete(result: CropResult) {
        doneLoading()
        if (result.croppedBitmap == null) {
            mOnCropCompleteListener?.onCropError(
                result.originalUri,
                result.error ?: Exception("Unknown Exception")
            )
        } else {
            mOnCropCompleteListener?.onCropComplete(result)
        }
    }

    /**
     * Interface for OnCropCompleteListener.
     * Classes that implement this interface can be used for setOnCropCompleteListener
     */
    interface OnCropCompleteListener {
        /**
         * Called when the cropping process is completed.
         * @param result the crop result
         */
        fun onCropComplete(result: CropResult)

        /**
         * Called when an error occured during the cropping process
         * @param uri the uri of the original image
         * @param error the exception that occured during the process
         */
        fun onCropError(uri: Uri?, error: Exception)
    }

    /**
     * Interface for OnLoadUriImageCompleteListener
     * Classes that implement this interface can be used for setOnLoadUriImageCompleteListener
     */
    interface OnLoadUriImageCompleteListener {
        /**
         * Called when the loading process is completed
         * @param result the loaded bitmap
         * @param uri the source uri of the loaded bitmap
         */
        fun onLoadUriImageComplete(result: Bitmap, uri: Uri)

        /**
         * Called when an error occured during the loading process
         * @param uri the source uri that should be loaded
         * @param error the exception that occured during the loading process
         */
        fun onLoadUriImageError(uri: Uri, error: Exception)
    }

    /**
     * Container class for the cropping result
     * @param originalBitmap - the source bitmap
     * @param originalUri - the uri of the source bitmap
     * @param croppedBitmap - the cropped bitmap
     * @param croppedUri - the uri of the cropped bitmap
     * @param error - the error if something went wrong
     */
    data class CropResult(
        val originalBitmap: Bitmap?,
        val originalUri: Uri?,
        val croppedBitmap: Bitmap?,
        val croppedUri: Uri?,
        val error: Exception?
    )


}