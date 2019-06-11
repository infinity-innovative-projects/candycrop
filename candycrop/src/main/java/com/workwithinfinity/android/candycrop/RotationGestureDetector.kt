package com.workwithinfinity.android.candycrop


import android.view.MotionEvent

/**
 * Gesture detector for rotations
 */
class RotationGestureDetector(private val mListener: OnRotationGestureListener) {
    private var fX : Float = 0f
    private var fY: Float = 0f
    private var sX: Float = 0f
    private var sY: Float = 0f
    private var ptrID1: Int = 0
    private var ptrID2: Int = 0
    private var angleBeforeUpdate : Float = 0f
    /** The angle difference between two onRotation calls */
    var angleSinceUpdate : Float = 0f
        private set
    /** The rotated angle from the start of the gesture */
    var angle: Float = 0f
        private set

    init {
        ptrID1 = INVALID_POINTER_ID
        ptrID2 = INVALID_POINTER_ID
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                ptrID1 = event.getPointerId(event.actionIndex)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                angleBeforeUpdate = 0f
                ptrID2 = event.getPointerId(event.actionIndex)
                sX = event.getX(event.findPointerIndex(ptrID1))
                sY = event.getY(event.findPointerIndex(ptrID1))
                fX = event.getX(event.findPointerIndex(ptrID2))
                fY = event.getY(event.findPointerIndex(ptrID2))
                return true
            }
            MotionEvent.ACTION_MOVE -> if (ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID) {
                val nfX = event.getX(event.findPointerIndex(ptrID2))
                val nfY = event.getY(event.findPointerIndex(ptrID2))
                val nsX = event.getX(event.findPointerIndex(ptrID1))
                val nsY = event.getY(event.findPointerIndex(ptrID1))

                angle = angleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY)
                //ignore small angles to prevent unwanted rotation
                if(angle < 5f && angle > -5f) angle=0f
                angleSinceUpdate = angleBeforeUpdate - angle
                mListener.onRotation(this)
                angleBeforeUpdate = angle

                return true
            }
            MotionEvent.ACTION_UP -> ptrID1 = INVALID_POINTER_ID
            MotionEvent.ACTION_POINTER_UP ->{
                ptrID2 = INVALID_POINTER_ID
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                ptrID1 = INVALID_POINTER_ID
                ptrID2 = INVALID_POINTER_ID
            }
        }
        return false
    }

    private fun angleBetweenLines(
        fX: Float,
        fY: Float,
        sX: Float,
        sY: Float,
        nfX: Float,
        nfY: Float,
        nsX: Float,
        nsY: Float
    ): Float {
        val angle1 = Math.atan2((fY - sY).toDouble(), (fX - sX).toDouble()).toFloat()
        val angle2 = Math.atan2((nfY - nsY).toDouble(), (nfX - nsX).toDouble()).toFloat()

        var angle = Math.toDegrees((angle1 - angle2).toDouble()).toFloat() % 360
        if (angle < -180f) angle += 360.0f
        if (angle > 180f) angle -= 360.0f
        return angle
    }

    interface OnRotationGestureListener {
        fun onRotation(rotationDetector: RotationGestureDetector)
    }

    companion object {
        private val INVALID_POINTER_ID = -1
    }
}