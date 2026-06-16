package it.di.unipi.sam636694.semelion.utilities

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
//repository usata per comunicare al logViewModel le azioni dei giocatori
object SharedRepository {
    private val _channel = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val channel = _channel.asSharedFlow()

    suspend fun send(message: String) {
        _channel.emit(message)
    }
}