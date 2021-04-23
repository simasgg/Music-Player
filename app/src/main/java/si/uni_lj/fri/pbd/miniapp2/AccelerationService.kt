package si.uni_lj.fri.pbd.miniapp2

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder

class AccelerationService: Service(), SensorEventListener{

    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor
    private val noiseThreshold = 5
    private var dX : Float = 0.0F
    private var dY : Float = 0.0F
    private var dZ : Float = 0.0F
    private var command : String = "IDLE"

    override fun onBind(intent: Intent?): IBinder? {
        return serviceBinder
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    // our main method to determine the state of sensor
    override fun onSensorChanged(event: SensorEvent?) {
        event?:return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        this.dX = kotlin.math.abs(dX - x)
        this.dY = kotlin.math.abs(dY - y)
        this.dZ = kotlin.math.abs(dZ - z)

        if(dX < noiseThreshold)
            dX = 0.0F
        if(dY < noiseThreshold)
            dY = 0.0F
        if(dZ < noiseThreshold)
            dZ = 0.0F

        command = "IDLE"

        if(dX > dZ)
            command = "HORIZONTAL"
        if(dZ > dX)
            command = "VERTICAL"

        // getState is responsible for update (updating every 1 second)

    }

    // returns status
    fun getState() : String{
        return command
    }

    inner class RunServiceBinder : Binder() {
        val service: AccelerationService
            get() = this@AccelerationService
    }
    private val serviceBinder = RunServiceBinder()

    // implementation of AccelerationService for MediaPlayerService
    fun implementation(){
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    // MediaPlayerService closes this
    fun closeAcc(){
        sensorManager.unregisterListener(this)
    }


}