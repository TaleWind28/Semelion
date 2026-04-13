package it.di.unipi.sam636694.semelion

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

data class SnackBarEvent(
    val message:String,
    val action: SnackBarAction? = null
    )

data class SnackBarAction(
    val name: String,
    val action: () -> Unit
)

object SnackBarController{

    private val _events = Channel<SnackBarEvent>()
    val events = _events.receiveAsFlow()

    suspend fun sendEvent(event: SnackBarEvent){
        _events.send(event)
    }



}