package com.workwithinfinity.android.candycrop

import android.graphics.Bitmap
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
 * @param overlayColor Alpha of the overlay
 * @param cropSize size of the cropping window
 * @param resultWidth Width of the final image
 * @param resultHeight Height of the final image
 * @param backgroundColor background color of the cropping view
 * @param showButtonPositive Show the positive button
 * @param showButtonNegative show the negative button
 * @param buttonTextColor color of the positive and negative button
 * @param rotation rotation of the loaded image in degree
 * @param positiveText the text of the confirm button
 * @param negativeText the text of the cancel button
 * @param labelText the text displayed on top
 * @param drawBorder if the border of the overlay should be drawn
 * @param overlayStyle the style of the overlay. Supports RECT and CIRCLE
 * @param quality the compression quality when saving as jpeg
 * @param format the compression format to save the picture
 */
@Parcelize
data class CandyCropOptions(var useToolbar : Boolean = true,
                            var resultUri : Uri? = null,
                            var ratioX : Int = 1,
                            var ratioY : Int = 1,
                            @ColorInt var overlayColor : Int = Color.argb(150,0,0,0),
                            var cropSize : Float = 0.9f,
                            var resultWidth : Int = 100,
                            var resultHeight : Int = 100,
                            @ColorInt var backgroundColor : Int = Color.TRANSPARENT,
                            var showButtonPositive : Boolean = true,
                            var showButtonNegative : Boolean = true,
                            @ColorInt var buttonTextColor : Int = Color.WHITE,
                            var rotation : Float = 0f,
                            var positiveText : String = "",
                            var negativeText : String = "",
                            var labelText : String = "",
                            var drawBorder : Boolean = true,
                            var overlayStyle : OverlayStyle = OverlayStyle.RECT,
                            var quality : Int = 95,
                            var format : Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG

) : Parcelable

enum class OverlayStyle { RECT, CIRCLE }
