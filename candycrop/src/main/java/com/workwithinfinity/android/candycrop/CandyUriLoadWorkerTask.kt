package com.workwithinfinity.android.candycrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.support.media.ExifInterface
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
class CandyUriLoadWorkerTask(private val uri : Uri,private val view : WeakReference<CandyCropView>, private val rotation : Float) : AsyncTask<Any, Any, CandyUriLoadWorkerTask.UriLoadResult>() {

    /**
     * Loads the image into a bitmap and resize it if it's to big
     * @param params ignored
     */
    override fun doInBackground(vararg params: Any?): UriLoadResult {
        val bm = MediaStore.Images.Media.getBitmap(view.get()?.context?.contentResolver,uri)
        val exif = getExifData(view.get()?.context,uri)
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)
        val exifRotation = when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val sumRotation = (exifRotation + rotation)%360
        val finalRotation = when {
            (sumRotation>0f && sumRotation<=90f) -> 90f
            (sumRotation>90 && sumRotation<=180) -> 180f
            (sumRotation>180 && sumRotation<=270) -> 270f
            else -> 0f
        }

        //images bigger than GL_MAX_TEXTURE_SIZE cant be rendered in the view
        val maxSize = GLES10.GL_MAX_TEXTURE_SIZE
        val sizedBm = if(bm.width > maxSize || bm.height > maxSize) {
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

        val finalBm = if(finalRotation==0f) {
            sizedBm
        } else {
            val matrix = Matrix()
            matrix.postRotate(finalRotation)
            Bitmap.createBitmap(sizedBm,0,0,sizedBm.width,sizedBm.height,matrix,true)
        }
        return UriLoadResult(finalBm, uri)
    }

    /**
     * Reads the ExifInterface of the given uri
     * @param context the context
     * @param uri the uri
     * @return the ExifInterface or null if it can't read it
     */
    private fun getExifData(context : Context?,uri : Uri) : ExifInterface?  {
        if(context==null) return null
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val exif = ExifInterface(inputStream)
        inputStream.close()
        return exif
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