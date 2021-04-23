package si.uni_lj.fri.pbd.miniapp2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import si.uni_lj.fri.pbd.miniapp2.databinding.ActivityMainBinding
import kotlin.math.roundToInt
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private var progressStatus = 0.0F
    private var isStopped:Boolean = false
    private lateinit var binding :ActivityMainBinding
    var playerService : MediaPlayerService? = null
    var serviceBound : Boolean = false
    private var time : Double = 0.0
    private val notificationId = 101
    //private var builder: NotificationCompat.Builder? = null
    private lateinit var state : String
    private var btnOn: Button? = null
    private var btnOff: Button? = null

    companion object {
        const val ACTION_STOP = "stop_service"
        const val ACTION_START = "start_service"
        private const val channelID = "my_id"
    }


    // working with service connection (MediaPlayerService)
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.d("TAG", "Service bound")
            val binder = iBinder as MediaPlayerService.RunServiceBinder
            playerService = binder.service
            serviceBound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d("TAG", "Service disconnect")
            serviceBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // use binding instead of "findViewById"
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createNotification()

    }


    override fun onStart() {
        super.onStart()
        val i = Intent(this, MediaPlayerService::class.java )
        startService(i)
        bindService(i, mConnection, 0)
        i.action = ACTION_START
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            if (playerService?.isPlaying() == null){
                stopService(Intent(this, MediaPlayerService::class.java))
            }
            unbindService(mConnection)
            serviceBound = false
        }

    }


    // occurs when play button is pressed
    fun playButton(v: View?) {
        if(playerService?.isPlaying() == null || playerService?.isPaused() == true) {
            playerService?.playMusic()

            binding.songName.text=playerService?.getFileName()
            isStopped = false
            CoroutineScope(IO).launch {
                editProgressBar()
            }
            CoroutineScope(IO).launch {
                startStopTimer()
            }
        }else{
            Toast.makeText(this, "Track is already playing", Toast.LENGTH_SHORT).show()
        }
    }


    // occurs when pause button is pressed
    fun pauseButton(v: View?) {
        if(playerService?.isPlaying() != null) {
            if(playerService?.isPaused() == false){
                playerService?.pauseMusic()
            }else{
                Toast.makeText(this, "Track is already paused", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this, "Nothing is currently playing", Toast.LENGTH_SHORT).show()
        }
    }

    // occurs when stop button is pressed
    fun stopButton(v: View?) {
        if(playerService?.isPlaying() != null) {
            if(playerService?.isPaused() == false){
                playerService?.stopMusic()

                // reinitialize
                binding.songName.text = getString(R.string.pressPlay)
                binding.timeText.text = getString(R.string.time)
                updateNotification(getString(R.string.time), getString(R.string.pressPlay))
                isStopped = true
                time = 0.0
            }else{
                Toast.makeText(this, "Cannot stop while paused", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this, "Nothing to stop", Toast.LENGTH_SHORT).show()
        }
    }

    // occurs when exit button is pressed
    fun exitButton(v: View?) {
        playerService?.toExit()
        this.finishAffinity()
        exitProcess(0)
    }

    // occurs when gestureOn button is pressed, enables gestures
    fun gestureOn(v: View?) {
        btnOn = binding.on
        btnOff = binding.off
        Toast.makeText(this, "Gestures Enabled", Toast.LENGTH_SHORT).show()
        // let's work with accelerationServices as it is connected via MediaPlayerService
        playerService?.bridgeBetweenMainAndAcceleration("on")
        CoroutineScope(IO).launch {
            while(true){
                try {
                    state = playerService?.stateOfAcceleration().toString()
                    playOrPause(state)
                    // Sleep for 1000 milliseconds.
                    delay(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                if(!btnOff!!.isEnabled)
                    break
            }
        }
        btnOn!!.isEnabled = false
        btnOff!!.isEnabled = true
    }

    // calls MediaPlayer.play if VERTICAL, MediaPlayer.pause if HORIZONTAL, ignores IDLE
    private fun playOrPause(state:String){
        if(state == "HORIZONTAL"){
            if(playerService?.isPlaying() != null) {
                if(playerService?.isPaused() == false){
                    playerService?.pauseMusic()
                }
            }
        }else if(state == "VERTICAL"){
            if(playerService?.isPlaying() == null || playerService?.isPaused() == true) {
                playerService?.playMusic()

                this@MainActivity.runOnUiThread {
                    binding.songName.text=playerService?.getFileName()
                }
                isStopped = false
                CoroutineScope(IO).launch {
                    editProgressBar()
                }
                CoroutineScope(IO).launch {
                    startStopTimer()
                }
            }
        }else
            return
    }

    // occurs when gestureOff is pressed, disables gestures
    fun gestureOff(v: View?) {
        btnOn = binding.on
        btnOff = binding.off
        Toast.makeText(this, "Gestures Disabled", Toast.LENGTH_SHORT).show()
        playerService?.bridgeBetweenMainAndAcceleration("off")
        btnOn!!.isEnabled = true
        btnOff!!.isEnabled = false
    }

    // WORK WITH COROUTINES

    // progressBar editor, edits every 1000ms
    private suspend fun editProgressBar(){
        val lenOfSong = playerService!!.isPlaying()!!.duration
        //Log.d("TAG", len_of_song.toString())
        val lenInSeconds : Long = lenOfSong.toLong() / 1000
        //Log.d("TAG", len_in_seconds.toString())

        // quick math: if the length of song is 1min30sec (90sec) and delay is 1000ms,
        // then we can calculate my_calculation var with this formula: 90/(len_in_seconds*1).
        // my_calculation is responsible for the incrementation of progressStatus
        val myCalculation = 90/lenInSeconds.toFloat()

        progressBar = binding.progressBar

        // Start long running operation in a background thread
        while (progressStatus < 100) {
            progressStatus += myCalculation
            //Log.d("TAG", progressStatus.toString())
            // Update the progress bar and display the
            //current value in the text view
            progressBar.progress = progressStatus.toInt()
            try {
                // update every 1000ms
                delay(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // reinitialize if was stopped
            if(isStopped) {
                progressBar.progress = 0
                progressStatus = 0.0F
                return
            }

            // stop if was paused
            if(playerService?.isPaused() == true && !isStopped)
                return
        }

        // if it reached this stage, it means the song has ended, let's stop everything
        playerService?.stopMusic()

        this@MainActivity.runOnUiThread {
            binding.songName.text = getString(R.string.pressPlay)
            binding.timeText.text = getString(R.string.time)
        }
        updateNotification(getString(R.string.time), getString(R.string.pressPlay))
        isStopped = true
        time = 0.0

    }

    // timer editor, edits every 1000ms
    private suspend fun startStopTimer(){
        while(true) {
            try {
                time++
                // to avoid "Only the original thread that created a view hierarchy can touch its views." exception
                this@MainActivity.runOnUiThread {
                    binding.timeText.text = getTimerText()
                }
                updateNotification(getTimerText())
                delay(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // reinitialize if was stopped
            if(isStopped) {
                progressBar.progress = 0
                progressStatus = 0.0F
                break
            }

            // stop if was paused
            if(playerService?.isPaused() == true && !isStopped)
                break

        }
    }

    // method responsible for song current time, returns hh:mm:ss
    private fun getTimerText() : String{
        val rounded : Int = time.roundToInt()

        val seconds : Int = ((rounded % 86400) % 3600) % 60
        val minutes : Int = ((rounded % 86400) % 3600) / 60
        val hours : Int = ((rounded % 86400) / 3600)

        return formatTime(seconds, minutes, hours)
    }

    private fun formatTime(seconds:Int, minutes:Int, hours:Int) : String{
        return String.format("%02d", hours) + " : " + String.format("%02d", minutes) + " : " + String.format("%02d", seconds)
    }


    // initial creation of notification
    private fun createNotification() {
        if (Build.VERSION.SDK_INT < 26) {
            return
        } else {
            val channel = NotificationChannel(channelID, "notification", NotificationManager.IMPORTANCE_LOW)
            channel.description = "description"
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            channel.enableVibration(true)
            val managerCompat = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            managerCompat.createNotificationChannel(channel)
        }
    }

    // update notification every second
    private fun updateNotification(timerText: String, filenameText: String? = playerService?.getFileName()) {
        if (Build.VERSION.SDK_INT >= 26) {
            val intent = Intent(this, MediaPlayerService::class.java)
            intent.action = MediaPlayerService.ACTION_STOP
            val actionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_NO_CREATE)

            val builder = NotificationCompat.Builder(this, channelID)
                    .setContentTitle(filenameText)
                    .setContentText(timerText)
                    .setSmallIcon(R.drawable.ul100)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_pause, "Play", actionPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Pause", actionPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", actionPendingIntent)
                    .setWhen(0)
            with(NotificationManagerCompat.from(this)) {
                notify(notificationId, builder.build())
            }
        }
    }


}