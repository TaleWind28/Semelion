package it.di.unipi.sam636694.semelion.utilities

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

class AudioPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    fun playFile(@RawRes rawResId: Int) {

        MediaPlayer.create(context,rawResId).apply {
            player = this
            setOnCompletionListener {
                it.release()
                player = null
            }
            start()
        }
    }

    fun stop(){
        player?.stop()
        player?.release()
        player = null
    }
}