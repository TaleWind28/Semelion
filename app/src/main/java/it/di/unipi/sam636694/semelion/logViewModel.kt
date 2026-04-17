package it.di.unipi.sam636694.semelion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LogViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(LogUIState())

    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch{
            SharedRepository.channel.collect{ message ->
                registerAction(action = message, state = _uiState.value)

            }
        }
    }

    fun registerAction(action:String,state: LogUIState){
        _uiState.update {
            state.copy(
                actions = state.actions + action
            )
        }
    }

}

data class LogUIState(
    val actions:List<String> = emptyList()
)
