package it.di.unipi.sam636694.semelion

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object SharedRepository {
    private val _channel = Channel<String>(Channel.BUFFERED)
    val channel = _channel.receiveAsFlow()

    suspend fun send(message: String) {
        _channel.send(message)
    }
}