package it.di.unipi.sam636694.semelion.ui.snackbar

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

//Preso dal tutorial di Philippe lackner :
//https://www.youtube.com/watch?v=KFazs62lIkE
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