package com.workwithinfinity.candycrop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.ColorInt
import kotlinx.android.parcel.Parcelize

class CandyCrop {

    companion object {
        const val CANDYCROP_BUNDLE = "CANDYCROP_BUNDLE"
        const val CANDYCROP_OPTIONS = "CANDYCROP_OPTIONS"
        const val CANDYCROP_SOURCE = "CANDYCROP_SOURCE"
        const val CANDYCROP_RESULT_EXTRA = "CANDYCROP_RESULT_EXTRA"
        const val CANDYCROP_ACTIVITY_REQUEST = 2104

    }

    object Builder {

        fun activity(uri : Uri) :ActivityBuilder = ActivityBuilder(uri)

        class ActivityBuilder(val sourceUri : Uri) {
            private val mOptions = CandyCropOptions()

            private fun getIntent(context : Context) : Intent {
                val intent = Intent()
                intent.setClass(context,CandyCropActivity::class.java)
                val bundle = Bundle().apply {
                    putParcelable(CANDYCROP_OPTIONS, mOptions)
                    putParcelable(CANDYCROP_SOURCE, sourceUri)
                }
                intent.putExtra(CANDYCROP_BUNDLE,bundle)
                return intent
            }

            fun start(activity : Activity) {
                activity.startActivityForResult(getIntent(activity), CANDYCROP_ACTIVITY_REQUEST)
            }

            fun setUseToolbar(useToolbar : Boolean) : ActivityBuilder {
                mOptions.useToolbar = useToolbar
                return this
            }

            fun setResultUri(resultUri : Uri) : ActivityBuilder {
                mOptions.resultUri = resultUri
                return this
            }

            fun setCropRatio(ratioX : Int, ratioY : Int) : ActivityBuilder {
                mOptions.ratioX = ratioX
                mOptions.ratioY = ratioY
                return this
            }

            fun setOverlayAlpha(alpha : Int) : ActivityBuilder {
                mOptions.overlayAlpha = alpha
                return this
            }

            fun setCropWindowSize(size : Float) : ActivityBuilder {
                mOptions.cropSize = size
                return this
            }

            fun setButtonVisibility(positive : Boolean, negative : Boolean) : ActivityBuilder{
                mOptions.showButtonPositive = positive
                mOptions.showButtonNegative = negative
                return this
            }

            fun setResultSize(width : Int, height : Int) : ActivityBuilder {
                mOptions.resultWidth = width
                mOptions.resultHeight = height
                return this
            }

            fun setBackgroundColor(@ColorInt color : Int) : ActivityBuilder {
                mOptions.backgroundColor = color
                return this
            }
        }
    }

    @Parcelize
    data class CandyCropActivityResult(val resultUri : Uri?) : Parcelable
}