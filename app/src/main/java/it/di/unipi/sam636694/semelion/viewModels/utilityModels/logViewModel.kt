package it.di.unipi.sam636694.semelion.viewModels.utilityModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.utilities.SharedRepository
import it.di.unipi.sam636694.semelion.utilities.toActionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.text.dropLast

class LogViewModel(private val app: Application): AndroidViewModel(application = app) {
    private val _uiState = MutableStateFlow(LogUIState())

    val uiState = _uiState.asStateFlow()

    init {
        //creo la sharedRepo per poter registrare tutte le azioni e vivrà assieme al viewmodel
        viewModelScope.launch{
            SharedRepository.channel.collect{ message ->
                registerAction(action = message, state = _uiState.value)
            }
        }
    }

    //registro un'azione aggiungendola allo stato
    fun registerAction(action:String,state: LogUIState){
        //pulisco i log a fine partita
        if (action.toActionData().type=="matchEnded") _uiState.update { LogUIState() }
        else _uiState.update {
            state.copy(
                actions = state.actions + action
            )
        }
    }
    //traduco le azioni dello stato
    fun translateAction(action: String): String {
        val data = action.toActionData()
        return when (data.type) {
            "reveal" -> {
                val card = data.outcome.first()
                if (card.name.dropLast(1) == "7") app.getString(R.string.semelionLog7Revealed,cardName(card.name))
                else app.getString(R.string.semelionLogNot7Revealed,cardName(card.name),card.value)
            }

            "covered" -> {
                val card = data.outcome.first()
                app.getString(R.string.semelionLogCardCovered,cardName(card.name))
            }

            "swap" -> {
                val first = data.relevantCards[0]
                val second = data.relevantCards[1]
                val firstDesc = if (first.flag) cardName(first.name) else app.getString(R.string.semelionLogCardPosition,first.value)
                val secondDesc = if (second.flag) cardName(second.name) else app.getString(R.string.semelionLogCardPosition,second.value)
                app.getString(R.string.semelionLogSwap,firstDesc,secondDesc)
            }

            "King's Rule" -> {
                val direction = data.relevantCards[0].name
                val riga = data.relevantCards[0].value
                app.getString(R.string.semelionLogKingRule,riga+1,direction)
            }

            "Queen'Swipe" -> {
                val direction = data.relevantCards[0].name
                val colonna = data.relevantCards[0].value
                app.getString(R.string.semelionLogQueenSwipe,colonna+1,direction)
            }

            "Jack' chain" -> {
                val cards = data.relevantCards.joinToString(" -> ") { card ->
                    if (card.flag) cardName(card.name) else app.getString(R.string.semelionLogCardPosition,card.value)
                }
                app.getString(R.string.semelionLogJackMadness,cards)
            }

            "addedFromUncover" -> {
                val card = data.outcome.first()
                if (card.name.dropLast(1) == "7") app.getString(R.string.semelionLogUncover7Added, cardName(card.name))
                else app.getString(R.string.semelionLogUncoverNot7Added, cardName(card.name), card.value)
            }

            else -> app.getString(R.string.semelionUnrecognizedAction,data.type)
        }
    }

    //FUNZIONI DI TRADUZIONI
    fun cardName(raw: String): String {
        // Raw è nel formato "XY" dove X è il valore e Y è il seme (es. "7F", "joker_red")
        return when {
            raw.startsWith("joker_red") -> app.getString(R.string.semelionCardJokerRed)
            raw.startsWith("joker_black") -> app.getString(R.string.semelionCardJokerBlack)
            else -> {
                val value = raw.dropLast(1)
                val suit = raw.last().toString()
                "${mapValue(value)} di ${mapSuit(suit)}"
            }
        }
    }

    fun mapValue(value: String): String {
        return when (value) {
            "1"  -> app.getString(R.string.semelionCardAce)
            "8" -> app.getString(R.string.semelionCardJack)
            "9" -> app.getString(R.string.semelionCardQueen)
            "10" -> app.getString(R.string.semelionCardKing)
            else -> value
        }
    }

    fun mapSuit(suit: String): String {
        return when (suit) {
            "C" -> app.getString(R.string.semelionCardSuitHearts)
            "D" -> app.getString(R.string.semelionCardSuitDiamonds)
            "F" -> app.getString(R.string.semelionCardSuitClover)
            "P" -> app.getString(R.string.semelionCardSuitClubs)
            else -> suit
        }
    }
}

data class LogUIState(
    val actions:List<String> = emptyList()
)
