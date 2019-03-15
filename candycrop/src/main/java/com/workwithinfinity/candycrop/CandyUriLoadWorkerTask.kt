package com.workwithinfinity.candycrop

import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES10
import android.os.AsyncTask
import android.provider.MediaStore
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

class CandyUriLoadWorkerTask(private val uri : Uri,private val view : WeakReference<CandyCropView>) : AsyncTask<Any, Any, CandyUriLoadWorkerTask.UriLoadResult>() {

    override fun doInBackground(vararg params: Any?): UriLoadResult {
        val bm = MediaStore.Images.Media.getBitmap(view.get()?.context?.contentResolver,uri)
        //TODO read exif and do stuff with it

        val maxSize = GLES10.GL_MAX_TEXTURE_SIZE
        val finalBm = if(bm.width > maxSize || bm.height > maxSize) {
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
        return UriLoadResult(finalBm,uri)
    }

    override fun onPostExecute(result: UriLoadResult) {
        view.get()?.onLoadUriImageComplete(result)
    }

    data class UriLoadResult(val bitmap : Bitmap, val uri : Uri)
}