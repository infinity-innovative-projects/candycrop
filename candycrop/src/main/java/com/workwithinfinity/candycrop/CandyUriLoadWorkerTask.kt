package com.workwithinfinity.candycrop

import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES10
import android.os.AsyncTask
import android.provider.MediaStore
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * WorkerTask to load an image from the given uri
 * @param uri Uri of the image to load
 * @param view WeakReference of the view starting the task
 */
class CandyUriLoadWorkerTask(private val uri : Uri,private val view : WeakReference<CandyCropView>) : AsyncTask<Any, Any, CandyUriLoadWorkerTask.UriLoadResult>() {

    /**
     * Loads the image into a bitmap and resize it if it's to big
     * @param params ignored
     */
    override fun doInBackground(vararg params: Any?): UriLoadResult {
        val bm = MediaStore.Images.Media.getBitmap(view.get()?.context?.contentResolver,uri)
        //TODO read exif and do stuff with it

        //images bigger than GL_MAX_TEXTURE_SIZE cant be rendered in the view
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
    data class UriLoadResult(val bitmap : Bitmap, val uri : Uri)
}