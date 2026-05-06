package it.di.unipi.sam636694.semelion

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.core.net.toUri
import java.io.File

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