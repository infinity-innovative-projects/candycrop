package com.workwithinfinity.candycrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.AsyncTask
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
                          private val view : WeakReference<CandyCropView>
) : AsyncTask<Unit,Unit,CandyCropView.CropResult>() {

    override fun doInBackground(vararg params: Unit?): CandyCropView.CropResult {
        if(isCancelled) {
            return CandyCropView.CropResult(null,null,null,null)
        }
       //TODO improve scaling image to avoid losing quality
        val scaledBitmap = Bitmap.createScaledBitmap(source,(source.width*scaleFactor).roundToInt(),(source.height*scaleFactor).roundToInt(),useFilter)
        val croppedBitmap = Bitmap.createBitmap(scaledBitmap,cropRect.left-positionX,cropRect.top-positionY,cropRect.width(),cropRect.height())
        val finalBitmap = if(resultWidth > 0 && resultHeight > 0) {
            Bitmap.createScaledBitmap(croppedBitmap, resultWidth, resultHeight, useFilter)
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