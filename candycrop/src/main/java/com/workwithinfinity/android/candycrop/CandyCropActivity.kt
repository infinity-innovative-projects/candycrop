package com.workwithinfinity.android.candycrop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.workwithinfinity.android.R

/**
 * Activity that can be launched to crop an image
 * Use the CandyCrop.Builder to use this activity
 */
class CandyCropActivity : AppCompatActivity(),
    CandyCropView.OnCropCompleteListener {

    /** The CandyCropView */
    private lateinit var mCropView : CandyCropView
    /** the Toolbar */
    private lateinit var mToolbar : Toolbar
    /** the options set by the builder */
    private lateinit var mOptions : CandyCropOptions
    /** the uri of the source image */
    private lateinit var mSourceUri : Uri
    /** the positive button */
    private lateinit var mTxtOk : TextView
    /** the negative button */
    private lateinit var mTxtCancel : TextView
    /** the label text */
    private lateinit var mTxtLabel : TextView

    /**
     * onCreate of the activity
     * @param savedInstanceState null if fresh activity, else the saved instance state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.candy_crop_activity) //inflate the view

        //save the different views
        mCropView = findViewById(R.id.cropping_view)
        mToolbar = findViewById(R.id.toolbar)
        mTxtOk = findViewById(R.id.txt_ok)
        mTxtCancel = findViewById(R.id.txt_cancel)
        mTxtLabel = findViewById(R.id.txt_label)
        setSupportActionBar(mToolbar)

        val bundle = intent.getBundleExtra(CandyCrop.CANDYCROP_BUNDLE)

        //read options
        mOptions = bundle.getParcelable(CandyCrop.CANDYCROP_OPTIONS) ?: CandyCropOptions()
        val sourceUri : Uri? = bundle.getParcelable(CandyCrop.CANDYCROP_SOURCE_URI)


        //apply options
        if(mOptions.useToolbar) {
            supportActionBar?.show()
        } else {
            supportActionBar?.hide()
        }

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(mOptions.positiveText.isNotBlank()) {
            mTxtOk.text = mOptions.positiveText
        }

        if(mOptions.negativeText.isNotBlank()) {
            mTxtCancel.text = mOptions.negativeText
        }

        if(mOptions.labelText.isNotBlank()) {
            mTxtLabel.text = mOptions.labelText
        }

        with(mCropView) {
            setAspectRatio(mOptions.ratioX,mOptions.ratioY)
            setResultUri(mOptions.resultUri)
            setOnCropCompleteListener(this@CandyCropActivity)
            setOverlayColor(mOptions.overlayColor)
            setResultSize(mOptions.resultWidth,mOptions.resultHeight)
            setCropSize(mOptions.cropSize)
            setBgColor(mOptions.backgroundColor)
            setInitialRotation(mOptions.rotation)
            setDrawRect(mOptions.drawRect)
            setQuality(mOptions.quality)
            setFormat(mOptions.format)
        }

        mTxtOk.visibility = when(mOptions.showButtonPositive) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        mTxtOk.setTextColor(mOptions.buttonTextColor)
        mTxtCancel.visibility = when(mOptions.showButtonNegative) {
            true -> View.VISIBLE
            false -> View.GONE
        }

        mTxtOk.setOnClickListener {
            onConfirm()
        }

        mTxtCancel.setOnClickListener {
            onCancel()
        }

        if(savedInstanceState == null) {
            if(sourceUri!= null && sourceUri !=(Uri.EMPTY)) {
                checkAndLoad(sourceUri)
            }
        } else {
            val uri : Uri? = savedInstanceState.getParcelable("sourceUri")
            if(uri!=null)
            {
                checkAndLoad(uri)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == CandyCrop.CANDYCROP_REQUEST_READ_PERMISSION) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                mCropView.setImageUriAsync(mSourceUri)
            } else {
                Toast.makeText(this,"Permission required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkAndLoad(uri : Uri) {
        mSourceUri = uri
        if(CandyCrop.checkReadPermissionRequired(this,uri)) {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),CandyCrop.CANDYCROP_REQUEST_READ_PERMISSION)
            return
        }
        mCropView.setImageUriAsync(uri)
    }

    /**
     * Called when positive button or crop button is pressed
     */
    private fun onConfirm() {
        mCropView.getCroppedBitmapAsync()
    }

    /**
     * called when negative button or up button is pressed
     */
    private fun onCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    /**
     * Save the instance state
     * @param outState bundle to save the state in
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("sourceUri",mSourceUri)
        super.onSaveInstanceState(outState)
    }

    /**
     * Called when button on the toolbar clicked
     * @param item the clicked item
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.candycrop_menu_crop -> {
                onConfirm()
                true
            }
            android.R.id.home -> {
                onCancel()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * populates the toolbar
     * @param menu the menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.candycrop_menu,menu)
        return true
    }

    /**
     * implementation of the OnCropCompleteListener
     * @param result the result of the cropping process
     */
    override fun onCropComplete(result: CandyCropView.CropResult) {
        val intent = Intent()
        intent.putExtras(getIntent())
        val res = CandyCrop.CandyCropActivityResult(result.croppedUri)
        intent.putExtra(CandyCrop.CANDYCROP_RESULT_EXTRA,res)
        setResult(RESULT_OK,intent)
        finish()
    }
}