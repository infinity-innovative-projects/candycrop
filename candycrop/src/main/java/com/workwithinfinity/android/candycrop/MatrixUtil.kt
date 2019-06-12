package com.workwithinfinity.android.candycrop

import android.graphics.Matrix
import android.graphics.Matrix.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.round


/**Calculates the actual rotation of a Matrix in degree */
fun Matrix.getRotation() = FloatArray(9)
    .apply {
    getValues(this)    }
    .let {-round(atan2(it[MSKEW_X], it[MSCALE_X]) * (180 / PI)).toFloat()}

/** Calculates the scale factor of a Matrix*/
fun Matrix.getScale() = FloatArray(9)
    .apply {
        getValues(this) }
    .let {Math.sqrt((it[MSCALE_X] * it[MSCALE_X] + it[MSKEW_Y] * it[MSKEW_Y]).toDouble())}

/** Calculates the Translation of a Matrix */
fun Matrix.getTranslation() = FloatArray(9)
    .apply {
        getValues(this)
    }
    .let {Pair(it[MTRANS_X],it[MTRANS_Y])}


/**
 * Interpolates between two matrices
 * @receiver the first matrix
 * @param other second matrix
 * @param t between 0-1
 * @param out matrix that holds the interpolated result
 */
fun Matrix.interpolate(other : Matrix, t : Float, out : Matrix) {
    val fat = FloatArray(9).apply{getValues(this)}
    val fao = FloatArray(9).apply{other.getValues(this)}
    val fai = FloatArray(9)
    for (i in 0..8) {
        //naive linear interpolation works better than expected
        fai[i] = fat[i] + (fao[i]-fat[i])*t
    }
    out.setValues(fai)
}
