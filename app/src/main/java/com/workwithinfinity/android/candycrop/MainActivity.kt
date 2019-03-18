package com.workwithinfinity.android.candycrop

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.workwithinfinity.android.R
import java.io.File

class MainActivity : AppCompatActivity(), CandyCropView.OnCropCompleteListener, CandyCropView.OnLoadUriImageCompleteListener {

    /** choose image intent request code */
    private val CHOOSE_IMAGE = 1598
    /**  the CandyCropView */
    private lateinit var cropView : CandyCropView
    /** the crop button */
    private lateinit var cropButton : Button
    /** the select source button */
    private lateinit var selectButton : Button
    /** the launch activity demo button */
    private lateinit var activitybutton : Button
    /** the selected source uri */
    private var mUri : Uri? = null

    /**
     * OnCreate of the Activity
     * @param savedInstanceState null if fresh, contains the source uri otherwise
     */
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

    /**
     * Save the instance state
     * @param outState bundle to save the state in
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("sourceUri",mUri)
        super.onSaveInstanceState(outState)
    }

    /**
     * Implementation of the onCropCompleteListener interface
     * @param result the result of the cropping process
     */
    override fun onCropComplete(result: CandyCropView.CropResult) {
        Log.d("CandyCropTest","Cropping complete: OgUri: ${result.originalUri?.toString()} CroppedSize: ${result.croppedBitmap?.width}/${result.croppedBitmap?.height}")
        if(result.croppedUri!=null)
            cropView.setImageUriAsync(result.croppedUri!!)
    }

    /**
     * Implementation of the onLoadUriImageCompleteListener
     * @param result the loaded image as bitmap
     * @param uri the source uri
     */
    override fun onLoadUriImageComplete(result: Bitmap,uri : Uri) {
        mUri = uri
        cropButton.isEnabled = true
        activitybutton.isEnabled = true
    }

    /**
     * Starts a choose image from gallery activity
     */
    private fun startBrowseImageIntent() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*")
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), CHOOSE_IMAGE)
    }

    /**
     * Starts a CandyCropActivity with the selected source
     */
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

    /**
     * Get results of launched activities
     * @param requestCode the request code. CHOOSE_IMAGE and CANDYCROP_ACTIVITY_REQUEST are relevant
     * @param resultCode the resultCode
     * @param data the result data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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
                if(data!=null) {
                    val result = data.getParcelableExtra<CandyCrop.CandyCropActivityResult>(
                        CandyCrop.CANDYCROP_RESULT_EXTRA)
                    if(result.resultUri!=null) {
                        cropView.setImageUriAsync(result.resultUri!!)
                    }
                }
            } else {
                Log.d(this.javaClass.name,"Failed to crop picture")
            }
        }
    }
}
