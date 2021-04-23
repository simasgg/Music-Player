package si.uni_lj.fri.pbd.miniapp2

import android.app.*
import android.content.*
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.IOException
import kotlin.system.exitProcess


open class MediaPlayerService:Service() {
    private var globalPlayer : MediaPlayer? = null
    private var pause : Boolean = false
    private var filename : String = "Track name"
    var accelerationService : AccelerationService? = null
    var serviceBound : Boolean = false
    private lateinit var state : String

    companion object {
        const val ACTION_STOP = "stop_service"
    }

    // working with service connection (AccelerationService)
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.d("TAG", "Service bound")
            val binder = iBinder as AccelerationService.RunServiceBinder
            accelerationService = binder.service
            serviceBound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d("TAG", "Service disconnect")
            serviceBound = false
        }
    }

    inner class RunServiceBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }
    private val serviceBinder = RunServiceBinder()

    // is the music currently playing
    fun isPlaying(): MediaPlayer? {
        return globalPlayer
    }

    // is the music currently paused
    fun isPaused(): Boolean? {
        return pause
    }

    // to get the file name
    fun getFileName(): String{
        return filename
    }

    // MediaPlayer implementation to play music
    fun playMusic() {
        // if not playing, then start to play
        if(!pause) {
            val assetManager = assets
            val files = assetManager.list("music")
            val myMusicList = files?.toList()
            //Toast.makeText(this, myMusicList?.size.toString(), Toast.LENGTH_LONG).show();
            if (myMusicList != null) {
                // choosing random track
                val random = (myMusicList.indices).random()
                for (i in myMusicList.indices) {
                    if(i == random) {
                        filename = myMusicList[i]
                        break
                    }
                }
            }else{
                exitProcess(1)
            }

            val afd: AssetFileDescriptor?
            try {
                afd = resources.assets.openFd("music/$filename")
                globalPlayer = MediaPlayer()
                globalPlayer!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                globalPlayer!!.prepare()
                globalPlayer!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }else{
            globalPlayer?.seekTo(globalPlayer!!.currentPosition)
            globalPlayer?.start()
            pause = false
        }
    }

    // MediaPlayer implementation to pause music
    fun pauseMusic(){
        globalPlayer?.pause()
        pause = true
    }

    // MediaPlayer implementation to stop music
    fun stopMusic(){
        pause = false
        globalPlayer?.stop()
        globalPlayer?.reset()
        globalPlayer?.release()
        globalPlayer = null
    }

    // MediaPlayer implementation to stop music and the exit
    fun toExit(){
        globalPlayer?.stop()
        globalPlayer?.reset()
        globalPlayer?.release()
        globalPlayer = null

    }


    override fun onCreate() {
        val i = Intent(this, AccelerationService::class.java )
        startService(i)
        bindService(i, mConnection, 0)
    }


    override fun onBind(intent: Intent): IBinder? {
        return serviceBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mConnection)
    }


    // gets the state of acceleration (VERTICAL, HORIZONTAL or IDLE)
    fun stateOfAcceleration() : String{
        state = accelerationService?.getState().toString()
        return state
    }

    // "bridge" method between MainActivity and AccelerationService
    fun bridgeBetweenMainAndAcceleration(input:String){
        if(input == "on"){
            accelerationService?.implementation()
        }else{
            accelerationService?.closeAcc()
        }
    }


}