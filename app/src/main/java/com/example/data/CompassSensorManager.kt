package com.example.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassOrientation(
    val azimuthDegrees: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isSensorAvailable: Boolean = true
)

class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _orientation = MutableStateFlow(CompassOrientation())
    val orientation: StateFlow<CompassOrientation> = _orientation.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var smoothedAzimuth = 0f

    fun startListening() {
        val hasRotation = rotationVectorSensor != null
        val hasFallback = accelerometerSensor != null && magneticSensor != null

        if (!hasRotation && !hasFallback) {
            _orientation.value = _orientation.value.copy(isSensorAvailable = false)
            return
        }

        _orientation.value = _orientation.value.copy(isSensorAvailable = true)

        if (hasRotation) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f

                updateSmoothedAzimuth(azimuthDeg, event.accuracy)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                lastAccelerometerSet = true
                computeOrientationFromFallback(event.accuracy)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                lastMagnetometerSet = true
                computeOrientationFromFallback(event.accuracy)
            }
        }
    }

    private fun computeOrientationFromFallback(accuracy: Int) {
        if (lastAccelerometerSet && lastMagnetometerSet) {
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f

                updateSmoothedAzimuth(azimuthDeg, accuracy)
            }
        }
    }

    private fun updateSmoothedAzimuth(newAzimuth: Float, accuracy: Int) {
        // Circular smoothing to prevent wrapping jumps at 0/360 boundary
        val diff = (newAzimuth - smoothedAzimuth + 180f) % 360f - 180f
        val alpha = 0.18f // Low-pass filter smoothing coefficient
        smoothedAzimuth = (smoothedAzimuth + diff * alpha + 360f) % 360f

        _orientation.value = CompassOrientation(
            azimuthDegrees = smoothedAzimuth,
            accuracy = accuracy,
            isSensorAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _orientation.value = _orientation.value.copy(accuracy = accuracy)
    }
}
