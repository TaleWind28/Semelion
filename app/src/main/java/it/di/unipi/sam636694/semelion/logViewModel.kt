package it.di.unipi.sam636694.semelion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(LogUIState())

    val uiState = _uiState.asStateFlow()

    val logQueue = Channel<String>(Channel.BUFFERED)

    init {
        viewModelScope.launch{
            for (action in logQueue){
                registerAction(action,_uiState.value)
            }
        }
    }

    fun registerAction(action:String,state: LogUIState){

    }

}

data class LogUIState(
    val actions:List<String> = emptyList()
)