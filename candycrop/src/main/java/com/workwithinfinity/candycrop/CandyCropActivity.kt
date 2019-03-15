package com.workwithinfinity.candycrop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView

class CandyCropActivity : AppCompatActivity(), CandyCropView.OnCropCompleteListener {

    private lateinit var mCropView : CandyCropView
    private lateinit var mToolbar : Toolbar
    private lateinit var mOptions : CandyCropOptions
    private lateinit var mSourceUri : Uri
    private lateinit var mTxtOk : TextView
    private lateinit var mTxtCancel : TextView
    private val TAG = "CandyCropActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.candy_crop_activity)
        mCropView = findViewById(R.id.cropping_view)
        mToolbar = findViewById(R.id.toolbar)
        mTxtOk = findViewById(R.id.txt_ok)
        mTxtCancel = findViewById(R.id.txt_cancel)
        setSupportActionBar(mToolbar)

        val bundle = intent.getBundleExtra(CandyCrop.CANDYCROP_BUNDLE)

        mOptions = bundle.getParcelable(CandyCrop.CANDYCROP_OPTIONS) ?: CandyCropOptions()
        val sourceUri : Uri? = bundle.getParcelable(CandyCrop.CANDYCROP_SOURCE)

        //apply options
        if(!mOptions.useToolbar) {
            supportActionBar?.hide()
        } else {
            supportActionBar?.show()
        }

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(mCropView) {
            setAspectRatio(mOptions.ratioX,mOptions.ratioY)
            setResultUri(mOptions.resultUri)
            setOnCropCompleteListener(this@CandyCropActivity)
            setOverlayAlpha(mOptions.overlayAlpha)
            setResultSize(mOptions.resultWidth,mOptions.resultHeight)
            setCropSize(mOptions.cropSize)
            setBackgroundColor(mOptions.backgroundColor)
        }

        mTxtOk.visibility = when(mOptions.showButtonPositive) {
            true -> View.VISIBLE
            false -> View.GONE
        }
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
                mSourceUri = sourceUri
                mCropView.setImageUriAsync(mSourceUri)
            }
        } else {
            val uri : Uri? = savedInstanceState.getParcelable("sourceUri")
            if(uri!=null)
            {
                mSourceUri = uri
                mCropView.setImageUriAsync(mSourceUri)
            }
        }
    }

    private fun onConfirm() {
        mCropView.getCroppedBitmapAsync()
    }

    private fun onCancel() {
        Log.d(TAG,"onCancel")
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("sourceUri",mSourceUri)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG,resources.getResourceName(item.itemId))
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.candycrop_menu,menu)
        return true
    }

    override fun onCropComplete(result: CandyCropView.CropResult) {
        Log.d(TAG,"OnCropComplete")
        val intent = Intent()
        //intent.putExtras(getIntent())
        val res = CandyCrop.CandyCropActivityResult(result.croppedUri)
        intent.putExtra(CandyCrop.CANDYCROP_RESULT_EXTRA,res)
        setResult(RESULT_OK,intent)

        finish()
        Log.d(TAG,"finished?")
    }
}