// FILE: GestureSensorManager.kt

package com.yourcompany.partygameapp.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

enum class GameGesture { TILT_FRONT, TILT_BACK, SHAKE }

class GestureSensorManager(
    context: Context,
    private val onGestureDetected: (GameGesture) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val TILT_FORWARD_THRESHOLD = -5.0f
    private val TILT_BACK_THRESHOLD = 5.0f
    private val SHAKE_THRESHOLD = 1200f
    private var lastUpdate: Long = 0
    private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f

    fun start() {
        lastUpdate = System.currentTimeMillis()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastUpdate) > 100) { // Check every 100ms
            val diffTime = (currentTime - lastUpdate)
            lastUpdate = currentTime

            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]

            if (z < TILT_FORWARD_THRESHOLD) {
                onGestureDetected(GameGesture.TILT_FRONT)
                return
            }
            if (z > TILT_BACK_THRESHOLD) {
                onGestureDetected(GameGesture.TILT_BACK)
                return
            }

            val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
            if (speed > SHAKE_THRESHOLD) { onGestureDetected(GameGesture.SHAKE) }
            lastX = x; lastY = y; lastZ = z
        }
    }
}