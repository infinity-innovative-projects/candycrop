package com.workwithinfinity.android.candycrop

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.workwithinfinity.android.R
import java.io.File

class MainActivity : AppCompatActivity(), CandyCropView.OnCropCompleteListener, CandyCropView.OnLoadUriImageCompleteListener {

    /** choose image intent request code */
    private val CHOOSE_IMAGE = 1598
    /**  the CandyCropWindowView */
    private lateinit var cropView : CandyCropView
    /** the crop button */
    private lateinit var cropButton : Button
    /** the select source button */
    private lateinit var selectButton : Button
    /** the launch activity demo button */
    private lateinit var activitybutton : Button
    /** the selected source uri */
    private var mUri : Uri? = null
    /** the rotate right button */
    private lateinit var rotateRightButton : ImageButton
    /** the rotate left button */
    private lateinit var rotateLeftButton : ImageButton

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
        cropView.setResultSize(1000,1000)
        cropView.setBackgroundColor(Color.WHITE)
        cropView.setAllowGestureRotation(true)
        cropView.setUseAnimation(true)


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

        rotateRightButton = findViewById(R.id.buttonRotateRight)
        rotateRightButton.isEnabled = false
        rotateRightButton.setOnClickListener {
            cropView.rotateForward()
        }

        rotateLeftButton = findViewById(R.id.buttonRotateLeft)
        rotateLeftButton.isEnabled = false
        rotateLeftButton.setOnClickListener {
            cropView.rotateBackward()
        }

        if(savedInstanceState!=null) {
            val uri : Uri? = savedInstanceState.getParcelable("sourceUri")
            if(uri!=null) {
                checkAndLoad(uri)
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

    override fun onCropError(uri: Uri?, error: Exception) {
        Log.d("CandyCropTest","Failed to Crop Image for uri ${uri.toString()} with error ${error.message}")
        Toast.makeText(this,"Something went Wrong",Toast.LENGTH_SHORT).show()
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
        rotateRightButton.isEnabled = true
        rotateLeftButton.isEnabled = true
    }

    override fun onLoadUriImageError(uri: Uri, error: Exception) {
        Log.d("CandyCropTest","Failed to Crop Image for uri ${uri.toString()} with error ${error.message}")
        Toast.makeText(this,"Something went Wrong",Toast.LENGTH_SHORT).show()
    }

    /**
     * Starts a choose image from gallery activity
     */
    private fun startBrowseImageIntent() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*")
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), CHOOSE_IMAGE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == CandyCrop.CANDYCROP_REQUEST_READ_PERMISSION) {
            val uri = mUri
            if(uri!=null && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                cropView.setImageUriAsync(uri)
            } else {
                Toast.makeText(this,"Permission required",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkAndLoad(uri : Uri) {
        mUri = uri
        if(CandyCrop.checkReadPermissionRequired(this,uri)) {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),CandyCrop.CANDYCROP_REQUEST_READ_PERMISSION)
            return
        }
        cropView.setImageUriAsync(uri)
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
            .setResultSize(1024,1024)
            .setOverlayColor(Color.argb(150,0,0,0))
            .setUseToolbar(false)
            .setCropRatio(1,1)
            .setCropWindowSize(0.9f)
            .setPositiveText("Auswählen")
            .setNegativeText("Abbrechen")
            .setLabelText("Bewegen und Skalieren")
            .setDrawBorder(true)
            .setResultFormat(Bitmap.CompressFormat.JPEG)
            .setResultQuality(50)
            .setOverlayStyle(OverlayStyle.CIRCLE)
            .setAllowGestureRotation(true)
            .setUseAnimation(true)
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
                    checkAndLoad(uri)
                }
            } else {
                Log.d(this.javaClass.name,"Failed to choose picture")
            }
        } else if(requestCode == CandyCrop.CANDYCROP_ACTIVITY_REQUEST) {
            if(resultCode == Activity.RESULT_OK) {
                if(data!=null) {
                    val result = data.getParcelableExtra<CandyCrop.CandyCropActivityResult>(
                        CandyCrop.CANDYCROP_RESULT_EXTRA)?.resultUri
                    if(result!=null) {
                        checkAndLoad(result)
                    }
                }
            } else {
                val errorSerializable = data?.getSerializableExtra(CandyCrop.CANDYCROP_ERROR_EXTRA)
                if(errorSerializable!=null) {
                    val error = errorSerializable as Exception
                    Toast.makeText(this,"Something went Wrong",Toast.LENGTH_SHORT).show()
                    Log.d(this.javaClass.name,"Error while Cropping with exception ${error.message}")
                } else {
                    Log.d(this.javaClass.name,"Crop canceled")
                }
            }
        }
    }
}
