package com.workwithinfinity.candycrop

import android.graphics.Color
import android.net.Uri
import android.os.Parcelable
import android.support.annotation.ColorInt
import kotlinx.android.parcel.Parcelize


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
