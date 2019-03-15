package com.workwithinfinity.candycrop

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
                          private val view : WeakReference<CandyCropView>
) : AsyncTask<Unit,Unit,CandyCropView.CropResult>() {

    override fun doInBackground(vararg params: Unit?): CandyCropView.CropResult {
        if(isCancelled) {
            return CandyCropView.CropResult(null,null,null,null)
        }

        val cropPositionX = ((cropRect.left-positionX)/scaleFactor).roundToInt()
        val cropPositionY = ((cropRect.top-positionY)/scaleFactor).roundToInt()
        val cropWidth = (cropRect.width()/scaleFactor).roundToInt()
        val cropHeight = (cropRect.height()/scaleFactor).roundToInt()

        val croppedBitmap = Bitmap.createBitmap(source,cropPositionX,cropPositionY,cropWidth,cropHeight)
        val finalBitmap = if(resultWidth > 0 && resultHeight > 0) {
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

            //Bitmap.createScaledBitmap(croppedBitmap, resultWidth, resultHeight, useFilter)
        } else {
            croppedBitmap
        }
        val context = view.get()?.context
        if(destUri!=null && context!=null) {
            saveBitmapToUri(finalBitmap,context,destUri)
        }
        return CandyCropView.CropResult(source,sourceUri,finalBitmap,destUri)
    }

    override fun onPostExecute(result: CandyCropView.CropResult) {
        view.get()?.onCroppingBitmapComplete(result)
    }

    private fun saveBitmapToUri(bm : Bitmap, context : Context, uri : Uri) {

        var outputStream : OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(uri)
            bm.compress(Bitmap.CompressFormat.PNG,100,outputStream)
            Log.d("CandyCropWorkerTask","saving bitmap to ${uri.path}")
        } finally {
            outputStream?.close()
        }
    }
}