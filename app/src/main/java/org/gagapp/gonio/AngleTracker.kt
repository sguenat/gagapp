package org.gagapp.gonio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Measures how far the phone has rotated since [zero] was called, in degrees (0..180).
 *
 * Two modes:
 *  - fusion (gyroscope + accelerometer): angle of the rotation between the zero orientation and
 *    the current one. Works in every plane, whatever way the phone is strapped on the limb.
 *  - accelerometer only: angle between the gravity direction at zero and now, seen from the
 *    phone. Works whenever the movement tilts the phone relative to vertical; it cannot see
 *    a rotation around the vertical axis, and needs slow movements (acceleration adds noise).
 */
class AngleTracker(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val rotationSensor: Sensor? =
        sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val hasFusion get() = rotationSensor != null
    val hasAccel get() = accelSensor != null

    /** Called on the main thread with the current angle once a zero has been set. */
    var onAngle: (Float) -> Unit = {}

    private var fusion = false
    private val q = FloatArray(4)
    private var q0: FloatArray? = null
    private val g = FloatArray(3)
    private var g0: FloatArray? = null
    private var hasSample = false
    private var zeroPending = false

    fun start(useFusion: Boolean) {
        stop()
        fusion = (useFusion && rotationSensor != null) || accelSensor == null
        val sensor = (if (fusion) rotationSensor else accelSensor) ?: return
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sm.unregisterListener(this)
        hasSample = false
        clearZero()
    }

    /** Takes the current orientation as 0°. If no sample arrived yet, the first one is used. */
    fun zero() {
        if (!hasSample) {
            zeroPending = true
            return
        }
        zeroPending = false
        if (fusion) q0 = q.clone() else g0 = g.clone()
    }

    fun clearZero() {
        q0 = null
        g0 = null
        zeroPending = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (fusion) {
            val v = if (event.values.size > 4) event.values.copyOf(4) else event.values
            SensorManager.getQuaternionFromVector(q, v)
        } else {
            // Low-pass filter to keep gravity and drop hand tremor / movement acceleration.
            val a = if (hasSample) 0.15f else 1f
            for (i in 0..2) g[i] += a * (event.values[i] - g[i])
        }
        hasSample = true
        if (zeroPending) zero()

        val angle = if (fusion) {
            val s = q0 ?: return
            val dot = abs(s[0] * q[0] + s[1] * q[1] + s[2] * q[2] + s[3] * q[3])
            2f * acos(dot.coerceAtMost(1f))
        } else {
            val s = g0 ?: return
            val n = norm(s) * norm(g)
            if (n == 0f) return
            acos(((s[0] * g[0] + s[1] * g[1] + s[2] * g[2]) / n).coerceIn(-1f, 1f))
        }
        onAngle(Math.toDegrees(angle.toDouble()).toFloat())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun norm(v: FloatArray) = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
}
