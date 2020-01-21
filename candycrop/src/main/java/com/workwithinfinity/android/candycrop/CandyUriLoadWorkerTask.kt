package com.workwithinfinity.android.candycrop

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.opengl.GLES10
import android.os.AsyncTask
import java.lang.ref.WeakReference
import kotlin.math.*


/**
 * WorkerTask to load an image from the given uri
 * @param uri Uri of the image to load
 * @param view WeakReference of the view starting the task
 */
class CandyUriLoadWorkerTask(
    private val uri: Uri,
    private val view: WeakReference<CandyCropView>,
    private val rotation: Float
) : AsyncTask<Any, Any, CandyUriLoadWorkerTask.UriLoadResult>() {

    /**
     * Loads the image into a bitmap and resize it if it's to big
     * @param params ignored
     */
    override fun doInBackground(vararg params: Any?): UriLoadResult {
        var fileDescriptor: AssetFileDescriptor? = null
        try {
            val (width, height, type) = getImageDimensions(uri)
            val maxSize = GLES10.GL_MAX_TEXTURE_SIZE / 2
            //Downsample big pictures to preserve memory
            val sampleSize = calculateSampleSize(width, height, maxSize)
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            fileDescriptor =
                view.get()?.context?.contentResolver?.openAssetFileDescriptor(uri, "r")
            val bm =
                BitmapFactory.decodeFileDescriptor(fileDescriptor?.fileDescriptor, null, options)
            fileDescriptor?.close()
            fileDescriptor = null
            val matrix = Matrix()
            val exif = getExifData(view.get()?.context, uri)
            when (exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    matrix.postRotate(90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    matrix.postRotate(180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    matrix.postRotate(270f)
                }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.postScale(1f, -1f, bm.width / 2f, bm.height / 2f)
                }
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                    matrix.postScale(-1f, 1f, bm.width / 2f, bm.height / 2f)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postScale(-1f, 1f, bm.width / 2f, bm.height / 2f)
                    matrix.postRotate(270f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postScale(-1f, 1f, bm.width / 2f, bm.height / 2f)
                    matrix.postRotate(90f)
                }
            }

            when {
                (rotation > 0f && rotation <= 90f) -> matrix.postRotate(90f)
                (rotation > 90f && rotation <= 180f) -> matrix.postRotate(180f)
                (rotation > 180f && rotation <= 270f) -> matrix.postRotate(270f)
            }


            //images bigger than GL_MAX_TEXTURE_SIZE cant be rendered in the view
            //could be removed, since rescaling huge pictures is done on loading now
            //keeping it for now, to be safe
            val sizedBm = if (bm.width > maxSize || bm.height > maxSize) {
                val dx = maxSize / bm.width.toFloat()
                val dy = maxSize / bm.height.toFloat()
                if (dx < dy) {
                    Bitmap.createScaledBitmap(bm, maxSize, (bm.height * dx).roundToInt(), true)
                } else {
                    Bitmap.createScaledBitmap(bm, (bm.width * dy).roundToInt(), maxSize, true)
                }
            } else {
                bm
            }

            val finalBm = if (!matrix.isIdentity) {
                Bitmap.createBitmap(sizedBm, 0, 0, sizedBm.width, sizedBm.height, matrix, true)
            } else {
                sizedBm
            }

            if (finalBm != sizedBm) {
                sizedBm.recycle()
            }
            return UriLoadResult(finalBm, uri, null)
        } catch (ex: Exception) {
            return UriLoadResult(null, uri, ex)
        } finally {
            fileDescriptor?.close()
        }
    }

    /**
     * Reads the ExifInterface of the given uri
     * @param context the context
     * @param uri the uri
     * @return the ExifInterface or null if it can't read it
     */
    private fun getExifData(context: Context?, uri: Uri): ExifInterface? {
        if (context == null) return null
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val exif = ExifInterface(inputStream)
        inputStream.close()
        return exif
    }

    /**
     * Returns the Width, Height and MimeType of Image referenced by the uri without loading the image into a bitmap
     * @param uri the uri referencing the image
     * @return Triple containing Width, Height, Type (in that order)
     */
    private fun getImageDimensions(uri: Uri): Triple<Int, Int, String?> {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var fileDescriptor: AssetFileDescriptor? = null
        try {
            fileDescriptor = view.get()?.context?.contentResolver?.openAssetFileDescriptor(uri, "r")
            BitmapFactory.decodeFileDescriptor(fileDescriptor?.fileDescriptor, null, options)
        } catch (ex: Exception) {
            throw ex
        } finally {
            fileDescriptor?.close()
        }

        val width = options.outWidth
        val height = options.outHeight
        val type = options.outMimeType
        return Triple(width, height, type)
    }

    /**
     * Calculates the sampleSize
     * @param width The width of the image
     * @param height The height of the image
     * @param maxSize The maximal Height of the image
     * @return The needed sampleSize to have a image with original width/height smaller than maxSize when loading
     */
    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        if (height > maxSize || width > maxSize) {
            sampleSize = 2.0.pow(
                ceil(
                    ln(
                        maxSize / max(
                            height,
                            width
                        ).toDouble()
                    ) / ln(0.5)
                ).toInt().toDouble()
            ).toInt()
        }
        return sampleSize
    }

    /**
     * Passes the result to the view
     * @result The result of the loading task
     */
    override fun onPostExecute(result: UriLoadResult) {
        view.get()?.onLoadUriImageComplete(result)
    }

    /**
     * Dataclass for the result
     * @param bitmap the loaded bitmap
     * @param uri the source uri
     */
    data class UriLoadResult(val bitmap: Bitmap?, val uri: Uri, val error: Exception?)
}