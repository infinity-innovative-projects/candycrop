package com.workwithinfinity.android.candycrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.AsyncTask
import android.support.annotation.ColorInt
import android.util.Log
import java.io.OutputStream
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * Workertask used to crop the image
 * @param source the source bitmap
 * @param sourceUri Uri of the source bitmap.
 * @param destUri Uri where the cropped image will be saved to
 * @param cropRect The rect that will be used to crop the image
 * @param scaleFactor the scalefactor of the image
 * @param positionX the X position of the image
 * @param positionY the Y position of the image
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
                          private val scaleFactor : Float,
                          private val positionX : Int,
                          private val positionY : Int,
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
            return CandyCropView.CropResult(null, null, null, null)
        }

        //resize the cropping dimension with scaleFactor
        val cropPositionX = ((cropRect.left-positionX)/scaleFactor).roundToInt()
        val cropPositionY = ((cropRect.top-positionY)/scaleFactor).roundToInt()
        val cropWidth = (cropRect.width()/scaleFactor).roundToInt()
        val cropHeight = (cropRect.height()/scaleFactor).roundToInt()
        //the actual cropping
        val croppedBitmap = Bitmap.createBitmap(source,cropPositionX,cropPositionY,cropWidth,cropHeight)

        val finalBitmap = if(resultWidth > 0 && resultHeight > 0) {
            //resize the final image. Fills with background color if aspect ratios don't match
            val dW = resultWidth/croppedBitmap.width.toFloat()
            val dH = resultHeight/croppedBitmap.height.toFloat()
            when {
                dW<dH -> {
                    val tempBm = Bitmap.createBitmap(resultWidth,resultHeight,Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(tempBm)
                    canvas.drawColor(backgroundColor)
                    canvas.drawBitmap(
                        Bitmap.createScaledBitmap(croppedBitmap,resultWidth,(croppedBitmap.height*dW).roundToInt(),useFilter),
                        0f,(resultHeight-croppedBitmap.height*dW)/2f,null)
                    tempBm
                }
                dW>dH -> {
                    val tempBm = Bitmap.createBitmap(resultWidth,resultHeight,Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(tempBm)
                    canvas.drawColor(backgroundColor)
                    canvas.drawBitmap(
                        Bitmap.createScaledBitmap(croppedBitmap,(croppedBitmap.width*dH).roundToInt(),resultHeight,useFilter),
                        (resultWidth-croppedBitmap.width*dH)/2f,0f,null)
                    tempBm
                }
                else -> Bitmap.createScaledBitmap(croppedBitmap, resultWidth, resultHeight, useFilter)
            }

        } else {
            croppedBitmap
        }

        val context = view.get()?.context
        if(destUri!=null && context!=null) {
            saveBitmapToUri(finalBitmap,context,destUri,format,quality)
        }
        croppedBitmap.recycle()
        return CandyCropView.CropResult(source, sourceUri, finalBitmap, destUri)
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
        } finally {
            outputStream?.close()
        }
    }
}