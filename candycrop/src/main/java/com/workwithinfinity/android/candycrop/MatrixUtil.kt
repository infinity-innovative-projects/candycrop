package com.workwithinfinity.android.candycrop

import android.graphics.Matrix
import android.graphics.Matrix.MSCALE_X
import android.graphics.Matrix.MSKEW_X
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.round

fun Matrix.getRotation() = FloatArray(9)
    .apply {
    getValues(this)    }
    .let {-round(atan2(it[MSKEW_X], it[MSCALE_X]) * (180 / PI)).toFloat()}
