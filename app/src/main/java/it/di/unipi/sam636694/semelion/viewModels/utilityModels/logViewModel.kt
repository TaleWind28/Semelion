package it.di.unipi.sam636694.semelion.viewModels.utilityModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.utilities.SharedRepository
import it.di.unipi.sam636694.semelion.utilities.toActionData
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
    fun translateAction(action: String): String {
        val data = action.toActionData()
        return when (data.type) {

            "reveal" -> {
                val card = data.outcome.first()
                "È stata rivelata la carta in posizione ${card.value}: ${cardName(card.name)}"
            }

            "covered" -> {
                val card = data.outcome.first()
                "La carta in posizione ${card.value} è stata coperta"
            }

            "swap" -> {
                val first = data.relevantCards[0]
                val second = data.relevantCards[1]
                val firstDesc = if (first.flag) cardName(first.name) else "posizione ${first.value}"
                val secondDesc = if (second.flag) cardName(second.name) else "posizione ${second.value}"
                "Sono state scambiate: $firstDesc con $secondDesc"
            }

            "King's Rule" -> {
                val first = data.relevantCards[0]
                val second = data.relevantCards[1]
                val firstDesc = if (first.flag) cardName(first.name) else "posizione ${first.value}"
                val secondDesc = if (second.flag) cardName(second.name) else "posizione ${second.value}"
                "Re: scambiate $firstDesc con $secondDesc"
            }

            "Queen'Swipe" -> {
                val first = data.relevantCards[0]
                val second = data.relevantCards[1]
                val firstDesc = if (first.flag) cardName(first.name) else "posizione ${first.value}"
                val secondDesc = if (second.flag) cardName(second.name) else "posizione ${second.value}"
                "Regina: scambiate $firstDesc con $secondDesc"
            }

            "Jack' chain" -> {
                val cards = data.relevantCards.joinToString(" -> ") { card ->
                    if (card.flag) cardName(card.name) else "posizione ${card.value}"
                }
                "Jack: catena di scambi $cards"
            }

            "addedFromUncover" -> {
                val card = data.outcome.first()
                "Aggiunta dalla pesca: ${cardName(card.name)} in posizione ${card.value}"
            }

            else -> "Azione sconosciuta: ${data.type}"
        }
    }

    fun cardName(raw: String): String {
        // Raw è nel formato "XY" dove X è il valore e Y è il seme (es. "7F", "joker_red")
        return when {
            raw.startsWith("joker_red") -> "Jolly Rosso"
            raw.startsWith("joker_black") -> "Jolly Nero"
            else -> {
                val value = raw.dropLast(1)
                val suit = raw.last().toString()
                "${mapValue(value)} di ${mapSuit(suit)}"
            }
        }
    }

    fun mapValue(value: String): String {
        return when (value) {
            "1"  -> "Asso"
            "11" -> "Fante"
            "12" -> "Regina"
            "13" -> "Re"
            else -> value
        }
    }

    fun mapSuit(suit: String): String {
        return when (suit) {
            "H" -> "Cuori"
            "D" -> "Quadri"
            "C" -> "Fiori"
            "S" -> "Picche"
            else -> suit
        }
    }
}

data class LogUIState(
    val actions:List<String> = emptyList()
)
