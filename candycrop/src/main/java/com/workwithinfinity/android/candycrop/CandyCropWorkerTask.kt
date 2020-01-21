package com.workwithinfinity.android.candycrop

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import androidx.annotation.ColorInt
import java.io.OutputStream
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * Workertask used to crop the image
 * @param source the source bitmap
 * @param sourceUri Uri of the source bitmap.
 * @param destUri Uri where the cropped image will be saved to
 * @param cropRect The rect that will be used to crop the image
 * @param matrix matrix defining position, scale and rotation
 * @param useFilter whether the filter should be used when resizing bitmap
 * @param resultWidth the width of the final image in pixel
 * @param resultHeight the height of the final image in pixel
 * @param backgroundColor the background color used when the result size has not the same aspect ratio as the image
 * @param view the view launching the task
 */
class CandyCropWorkerTask(private val source : Bitmap,
                          private val sourceUri : Uri?,
                          private val destUri : Uri?,
                          private val cropRect : Rect,
                          private val matrix : Matrix,
                          private val useFilter : Boolean = true,
                          private val resultWidth : Int,
                          private val resultHeight : Int,
                          @ColorInt private val backgroundColor : Int,
                          private val view : WeakReference<CandyCropView>,
                          private val quality : Int = 95,
                          private val format : Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
) : AsyncTask<Unit,Unit, CandyCropView.CropResult>() {

    /**
     * Crops the image and saves it to destUri (if destUri != null)
     * @param params ignored
     */
    override fun doInBackground(vararg params: Unit?): CandyCropView.CropResult {
        if(isCancelled) {
            return CandyCropView.CropResult(null, sourceUri, null, null,Exception("Process Canceled"))
        }

        try {


            val matrixInverted = Matrix()
            matrix.invert(matrixInverted)
            val cropRectFloat = RectF(cropRect)
            matrixInverted.mapRect(cropRectFloat)
            val rotationMatrix = Matrix().apply {
                setRotate(matrix.getRotation())
            }

            //the actual cropping
            val left = cropRectFloat.left.toInt()
            val top = cropRectFloat.top.toInt()
            //force round down if rounding up would lead to a crop bigger than the image
            val width =
                if (left + cropRectFloat.width().roundToInt() > source.width) cropRectFloat.width().toInt() else cropRectFloat.width().roundToInt()
            val height =
                if (top + cropRectFloat.height().roundToInt() > source.height) cropRectFloat.height().toInt() else cropRectFloat.height().roundToInt()
            var croppedBitmap =
                Bitmap.createBitmap(source, left, top, width, height, null, useFilter)
            croppedBitmap = Bitmap.createBitmap(
                croppedBitmap,
                0,
                0,
                croppedBitmap.width,
                croppedBitmap.height,
                rotationMatrix,
                useFilter
            )

            val finalBitmap = if (resultWidth > 0 && resultHeight > 0) {
                //resize the final image. Fills with background color if aspect ratios don't match
                val dW = resultWidth / croppedBitmap.width.toFloat()
                val dH = resultHeight / croppedBitmap.height.toFloat()
                val dif = dW - dH
                //use an error margin to compensate for rounding
                when {
                    dif < -0.001f -> {
                        val tempBm =
                            Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(tempBm)
                        canvas.drawColor(backgroundColor)
                        canvas.drawBitmap(
                            Bitmap.createScaledBitmap(
                                croppedBitmap,
                                resultWidth,
                                (croppedBitmap.height * dW).roundToInt(),
                                useFilter
                            ),
                            0f, (resultHeight - croppedBitmap.height * dW) / 2f, null
                        )
                        tempBm
                    }
                    dif > 0.001f -> {
                        val tempBm =
                            Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(tempBm)
                        canvas.drawColor(backgroundColor)
                        canvas.drawBitmap(
                            Bitmap.createScaledBitmap(
                                croppedBitmap,
                                (croppedBitmap.width * dH).roundToInt(),
                                resultHeight,
                                useFilter
                            ),
                            (resultWidth - croppedBitmap.width * dH) / 2f, 0f, null
                        )
                        tempBm
                    }
                    else -> {
                        Bitmap.createScaledBitmap(
                            croppedBitmap,
                            resultWidth,
                            resultHeight,
                            useFilter
                        )
                    }
                }

            } else {
                croppedBitmap
            }
            if (finalBitmap != croppedBitmap) {
                croppedBitmap.recycle()
            }

            val context = view.get()?.context
            if (destUri != null && context != null) {
                saveBitmapToUri(finalBitmap, context, destUri, format, quality)
            }

            return CandyCropView.CropResult(source, sourceUri, finalBitmap, destUri, null)
        } catch(ex : Exception) {
            return CandyCropView.CropResult(source,sourceUri,null,destUri,ex)
        }
    }

    /**
     * Executed after cropping the image.
     * @param result the result of the cropping
     */
    override fun onPostExecute(result: CandyCropView.CropResult) {
            view.get()?.onCroppingBitmapComplete(result)
    }

    /**
     * Saves the bitmap the the given uri
     * @param bm the bitmap to be saved
     * @param context the context
     * @param uri the uri to be saved to
     */
    private fun saveBitmapToUri(bm : Bitmap, context : Context, uri : Uri, format : Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality : Int = 95) {
        val q = if(quality in 0..100) quality else 95
        var outputStream : OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(uri)
            bm.compress(format,q,outputStream)
        }catch(ex : Exception) {
            throw ex
        } finally {
            outputStream?.close()
        }
    }
}