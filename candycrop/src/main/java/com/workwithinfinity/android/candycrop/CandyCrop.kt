package com.workwithinfinity.android.candycrop

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.ColorInt
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
        /** const name for the error in the bundle */
        const val CANDYCROP_ERROR_EXTRA = "CANDYCROP_ERROR_EXTRA"
        /** id for the activity request */
        const val CANDYCROP_ACTIVITY_REQUEST = 2104
        /** id for the read permission request */
        const val CANDYCROP_REQUEST_READ_PERMISSION = 3554


        /**
         * Checks if Read permission is required
         * @param context the context
         * @param uri uri to the file that should be read
         * @return true if the permission is requiered
         */
        fun checkReadPermissionRequired(context: Context, uri: Uri): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    && checkIfUriNeedsPermission(context, uri)
        }

        /**
         * Checks if accessing a specifig uri needs a permission
         * @param context the context
         * @param uri uri to the file that should be read
         * @return true if the permission is required
         */
        private fun checkIfUriNeedsPermission(context: Context, uri: Uri): Boolean {
            return try {
                val iS = context.contentResolver.openInputStream(uri)
                iS?.close()
                false
            } catch (ex: Exception) {
                true
            }
        }
    }

    object Builder {

        /**
         * Generates an ActivityBuilder
         * @param uri Uri of the source image
         */
        fun activity(uri: Uri): ActivityBuilder =
            ActivityBuilder(uri)

        /** Builder for the activity
         * @param sourceUri Uri of the source image
         */
        class ActivityBuilder(private val sourceUri: Uri) {
            /** Options used for the builder */
            private val mOptions = CandyCropOptions()

            /**
             * Generates an intent to launch the crop activity
             * @param context the context
             */
            private fun getIntent(context: Context): Intent {
                val intent = Intent()
                intent.setClass(context, CandyCropActivity::class.java)
                val bundle = Bundle().apply {
                    putParcelable(CANDYCROP_OPTIONS, mOptions)
                    putParcelable(CANDYCROP_SOURCE_URI, sourceUri)
                }
                intent.putExtra(CANDYCROP_BUNDLE, bundle)
                return intent
            }

            /**
             * starts the CandyCropActivity
             * @param activity the activity starting the CandyCropActivity
             */
            fun start(activity: Activity) {
                activity.startActivityForResult(
                    getIntent(activity),
                    CANDYCROP_ACTIVITY_REQUEST
                )
            }

            /**
             * starts the CandyCropActivity using Fragments
             * @param context the context
             * @param fragment the fragment
             */
            fun start(context: Context, fragment: androidx.fragment.app.Fragment) {
                fragment.startActivityForResult(getIntent(context), CANDYCROP_ACTIVITY_REQUEST)
            }

            /**
             * Sets the initial rotation of the loaded image
             * @param rotation the desired rotation
             * @return the builder itself
             */
            fun setRotation(rotation: Float): ActivityBuilder {
                mOptions.rotation = rotation
                return this
            }

            /**
             * Sets if the toolbar should be shown
             * @param useToolbar true shows the toolbar, false hides it
             * @return the builder itself
             */
            fun setUseToolbar(useToolbar: Boolean): ActivityBuilder {
                mOptions.useToolbar = useToolbar
                return this
            }

            /**
             * Sets the style of the overlay
             * possible styles are RECT and CIRCLE
             * @param style the desired style
             * @return the builder itself
             */
            fun setOverlayStyle(style: OverlayStyle): ActivityBuilder {
                mOptions.overlayStyle = style
                return this
            }

            /**
             * Sets the Uri where the result is saved to
             * @param resultUri the uri
             * @return the builder itself
             */
            fun setResultUri(resultUri: Uri): ActivityBuilder {
                mOptions.resultUri = resultUri
                return this
            }

            /**
             * Sets the desired aspect ratio of the crop window
             * @param ratioX Aspect ratio for the x dimension
             * @param ratioY Aspect ratio for the y dimension
             * @return the builder itself
             */
            fun setCropRatio(ratioX: Int, ratioY: Int): ActivityBuilder {
                mOptions.ratioX = ratioX
                mOptions.ratioY = ratioY
                return this
            }

            /**
             * Sets the desired color of the overlay
             * @param color the desired color
             * @return the builder itself
             */
            fun setOverlayColor(@ColorInt color: Int): ActivityBuilder {
                mOptions.overlayColor = color
                return this
            }

            /**
             * Sets the desired crop window size
             * @param size the size < 1
             * 1=full view
             * 0.5 = half the view
             * @return the builder itself
             */
            fun setCropWindowSize(size: Float): ActivityBuilder {
                mOptions.cropSize = size
                return this
            }

            /**
             * Sets the label of the positive button
             * @param text the text
             * @return the builder itself
             */
            fun setPositiveText(text: String): ActivityBuilder {
                mOptions.positiveText = text
                return this
            }

            /**
             * Sets the label of the negative button
             * @param text the text
             * @return the builder itself
             */
            fun setNegativeText(text: String): ActivityBuilder {
                mOptions.negativeText = text
                return this
            }

            /**
             * Sets the label
             * @param text the text
             * @return the builder itself
             */
            fun setLabelText(text: String): ActivityBuilder {
                mOptions.labelText = text
                return this
            }

            /**
             * Sets if the rect should be drawn
             * @param drawBorder if the rect should be drawn
             * @return the builder itself
             */
            fun setDrawBorder(drawBorder: Boolean): ActivityBuilder {
                mOptions.drawBorder = drawBorder
                return this
            }

            /**
             * Sets the desired format of the saved picture
             * @param format the format
             * @return the builder itself
             */
            fun setResultFormat(format: Bitmap.CompressFormat): ActivityBuilder {
                mOptions.format = format
                return this
            }

            /**
             * Sets if gesture rotation is enabled
             * @param allow true for enabled
             * @return the builder itself
             */
            fun setAllowGestureRotation(allow: Boolean): ActivityBuilder {
                mOptions.allowGestureRotation = allow
                return this
            }

            /**
             * Sets if animation should be used
             * @param useAnimation true if animation should be used
             * @return the builder itself
             */
            fun setUseAnimation(useAnimation: Boolean): ActivityBuilder {
                mOptions.useAnimation = useAnimation
                return this
            }

            /**
             * Sets the desired quality of the saved picture
             * @param quality the desired quality. Ignored if not between 0 and 100
             * @return the builder itself
             */
            fun setResultQuality(quality: Int): ActivityBuilder {
                if (quality !in 0..100) return this
                mOptions.quality = quality
                return this
            }


            /**
             * Sets whether the positive and negative buttons should be displayed
             * @param positive show positive button
             * @param negative show negative button
             * @return the builder itself
             */
            fun setButtonVisibility(positive: Boolean, negative: Boolean): ActivityBuilder {
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
            fun setResultSize(width: Int, height: Int): ActivityBuilder {
                mOptions.resultWidth = width
                mOptions.resultHeight = height
                return this
            }

            /**
             * Sets the desired background color of the view in the acivity
             * @param color the color as ColorInt
             */
            fun setBackgroundColor(@ColorInt color: Int): ActivityBuilder {
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
    data class CandyCropActivityResult(val resultUri: Uri?) : Parcelable
}