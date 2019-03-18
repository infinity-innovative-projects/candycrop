package com.workwithinfinity.android.candycrop

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
        /** Const name for the bundle */
        const val CANDYCROP_BUNDLE = "CANDYCROP_BUNDLE"
        /** Const name for the options in the bundle */
        const val CANDYCROP_OPTIONS = "CANDYCROP_OPTIONS"
        /** const name for the source uri in the boundle */
        const val CANDYCROP_SOURCE_URI = "CANDYCROP_SOURCE_URI"
        /** const name for the result in the bundle */
        const val CANDYCROP_RESULT_EXTRA = "CANDYCROP_RESULT_EXTRA"
        /** id for the activity request */
        const val CANDYCROP_ACTIVITY_REQUEST = 2104

    }

    object Builder {

        /**
         * Generates an ActivityBuilder
         * @param uri Uri of the source image
         */
        fun activity(uri : Uri) : ActivityBuilder =
            ActivityBuilder(uri)

        /** Builder for the activity
         * @param sourceUri Uri of the source image
         */
        class ActivityBuilder(private val sourceUri : Uri) {
            /** Options used for the builder */
            private val mOptions = CandyCropOptions()

            /**
             * Generates an intent to launch the crop activity
             * @param context the context
             */
            private fun getIntent(context : Context) : Intent {
                val intent = Intent()
                intent.setClass(context, CandyCropActivity::class.java)
                val bundle = Bundle().apply {
                    putParcelable(CANDYCROP_OPTIONS, mOptions)
                    putParcelable(CANDYCROP_SOURCE_URI, sourceUri)
                }
                intent.putExtra(CANDYCROP_BUNDLE,bundle)
                return intent
            }

            /**
             * starts the CandyCropActivity
             * @param activity the activity starting the CandyCropActivity
             */
            fun start(activity : Activity) {
                activity.startActivityForResult(getIntent(activity),
                    CANDYCROP_ACTIVITY_REQUEST
                )
            }

            /**
             * Sets if the toolbar should be shown
             * @param useToolbar true shows the toolbar, false hides it
             * @return the builder itself
             */
            fun setUseToolbar(useToolbar : Boolean) : ActivityBuilder {
                mOptions.useToolbar = useToolbar
                return this
            }

            /**
             * Sets the Uri where the result is saved to
             * @param resultUri the uri
             * @return the builder itself
             */
            fun setResultUri(resultUri : Uri) : ActivityBuilder {
                mOptions.resultUri = resultUri
                return this
            }

            /**
             * Sets the desired aspect ratio of the crop window
             * @param ratioX Aspect ratio for the x dimension
             * @param ratioY Aspect ratio for the y dimension
             * @return the builder itself
             */
            fun setCropRatio(ratioX : Int, ratioY : Int) : ActivityBuilder {
                mOptions.ratioX = ratioX
                mOptions.ratioY = ratioY
                return this
            }

            /**
             * Sets the desired alpah of the overlay
             * @param alpha the desired alpha ranging from 0 to 255
             * @return the builder itself
             */
            fun setOverlayAlpha(alpha : Int) : ActivityBuilder {
                mOptions.overlayAlpha = alpha
                return this
            }

            /**
             * Sets the desired crop window size
             * @param size the size < 1
             * 1=full view
             * 0.5 = half the view
             * @return the builder itself
             */
            fun setCropWindowSize(size : Float) : ActivityBuilder {
                mOptions.cropSize = size
                return this
            }

            /**
             * Sets whether the positive and negative buttons should be displayed
             * @param positive show positive button
             * @param negative show negative button
             * @return the builder itself
             */
            fun setButtonVisibility(positive : Boolean, negative : Boolean) : ActivityBuilder {
                mOptions.showButtonPositive = positive
                mOptions.showButtonNegative = negative
                return this
            }

            /**
             * Sets the desired size of the cropped image
             * @param width the width
             * @param height the height
             * @return the builder itself
             */
            fun setResultSize(width : Int, height : Int) : ActivityBuilder {
                mOptions.resultWidth = width
                mOptions.resultHeight = height
                return this
            }

            /**
             * Sets the desired background color of the view in the acivity
             * @param color the color as ColorInt
             */
            fun setBackgroundColor(@ColorInt color : Int) : ActivityBuilder {
                mOptions.backgroundColor = color
                return this
            }
        }
    }

    /**
     * Class used to pass the result to the launching activity
     * get this in the onActivityResult method with the request code CANDYCROP_ACTIVITY_REQUEST
     */
    @Parcelize
    data class CandyCropActivityResult(val resultUri : Uri?) : Parcelable
}