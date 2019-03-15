package com.workwithinfinity.candycroptest

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.workwithinfinity.candycrop.CandyCrop
import com.workwithinfinity.candycrop.CandyCropView
import java.io.File

class MainActivity : AppCompatActivity(), CandyCropView.OnCropCompleteListener, CandyCropView.OnLoadUriImageCompleteListener {

    private val CHOOSE_IMAGE = 1
    private lateinit var cropView : CandyCropView
    private lateinit var cropButton : Button
    private lateinit var selectButton : Button
    private lateinit var activitybutton : Button
    private var mUri : Uri? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cropView = findViewById(R.id.candyCropView)
        cropView.setOnCropCompleteListener(this)
        cropView.setOnLoadUriImageCompleteListener(this)
        cropView.setResultUri(Uri.fromFile(File(cacheDir,"cropped")))
        cropView.setResultSize(1024,1024)
        cropView.setBackgroundColor(Color.WHITE)

        selectButton = findViewById(R.id.btn_select_src)
        selectButton.setOnClickListener {
            startBrowseImageIntent()
        }

        activitybutton = findViewById(R.id.btn_activity_demo)
        activitybutton.isEnabled = false
        activitybutton.setOnClickListener { startCropActivityDemo() }

        cropButton = findViewById(R.id.btn_crop)
        cropButton.isEnabled = false
        cropButton.setOnClickListener {
            cropView.getCroppedBitmapAsync()
        }

        if(savedInstanceState!=null) {
            val uri : Uri? = savedInstanceState.getParcelable("sourceUri")
            if(uri!=null) {
                cropView.setImageUriAsync(uri)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("sourceUri",mUri)
        super.onSaveInstanceState(outState)
    }

    override fun onCropComplete(result: CandyCropView.CropResult) {
        Log.d("CandyCropTest","Cropping complete: OgUri: ${result.originalUri?.toString()} CroppedSize: ${result.croppedBitmap?.width}/${result.croppedBitmap?.height}")
        if(result.croppedUri!=null)
            cropView.setImageUriAsync(result.croppedUri!!)
    }

    override fun onLoadUriImageComplete(result: Bitmap,uri : Uri) {
        mUri = uri
        cropButton.isEnabled = true
        activitybutton.isEnabled = true
    }

    private fun startBrowseImageIntent() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*")
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), CHOOSE_IMAGE)
    }

    private fun startCropActivityDemo() {
        val uri = mUri ?: return
        CandyCrop.Builder.activity(uri)
            .setButtonVisibility(positive=true,negative=true)
            .setResultUri(Uri.fromFile(File(cacheDir,"cropped")))
            .setBackgroundColor(Color.BLACK)
            .setResultSize(1000,1000)
            .setOverlayAlpha(100)
            .setUseToolbar(true)
            .setCropRatio(10,5)
            .setCropWindowSize(0.9f)
            .start(this)
    }

    /*
puts the uri of the selected image into bundle and navigates to the cropping step
 */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG,"OnActivityResult")
        if(requestCode==CHOOSE_IMAGE) {
            if(resultCode == Activity.RESULT_OK) {
                if(data!=null) {
                    val result = data.dataString
                    val uri = Uri.parse(result)
                    cropView.setImageUriAsync(uri)
                }
            } else {
                Log.d(this.javaClass.name,"Failed to choose picture")
            }
        } else if(requestCode == CandyCrop.CANDYCROP_ACTIVITY_REQUEST) {
            if(resultCode == Activity.RESULT_OK) {
                Log.d(TAG,"Got crop result")
                if(data!=null) {
                    Log.d(TAG,"Data not null")
                    val result = data.getParcelableExtra<CandyCrop.CandyCropActivityResult>(CandyCrop.CANDYCROP_RESULT_EXTRA)
                    if(result.resultUri!=null) {
                        Log.d(TAG,"Result uri is: ${result.resultUri}")
                        cropView.setImageUriAsync(result.resultUri!!)
                    }
                }
            } else {
                Log.d(TAG,"Failed to crop picture")
            }
        }
    }
}
