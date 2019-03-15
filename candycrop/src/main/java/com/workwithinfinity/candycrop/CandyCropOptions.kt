package com.workwithinfinity.candycrop

import android.graphics.Color
import android.net.Uri
import android.os.Parcelable
import android.support.annotation.ColorInt
import kotlinx.android.parcel.Parcelize

/**
 * Data class to store the options for the CandyCrop Builder
 * @param useToolbar use the toolbar
 * @param resultUri Uri where the result will be saved
 * @param ratioX Aspect ratio of the x dimension
 * @param ratioY Aspect ratio of the y dimension
 * @param overlayAlpha Alpha of the overlay
 * @param cropSize size of the cropping window
 * @param resultWidth Width of the final image
 * @param resultHeight Height of the final image
 * @param backgroundColor background color of the cropping view
 * @param showButtonPositive Show the positive button
 * @param showButtonNegative show the negative button
 * @param buttonTextColor color of the positive and negative button
 */
@Parcelize
data class CandyCropOptions(var useToolbar : Boolean = true,
                            var resultUri : Uri? = null,
                            var ratioX : Int = 1,
                            var ratioY : Int = 1,
                            var overlayAlpha : Int = 125,
                            var cropSize : Float = 0.9f,
                            var resultWidth : Int = 100,
                            var resultHeight : Int = 100,
                            @ColorInt var backgroundColor : Int = Color.TRANSPARENT,
                            var showButtonPositive : Boolean = true,
                            var showButtonNegative : Boolean = true,
                            @ColorInt var buttonTextColor : Int = Color.WHITE
) : Parcelable
