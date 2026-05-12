package it.di.unipi.sam636694.semelion.gameModels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.di.unipi.sam636694.semelion.AudioPlayer
import it.di.unipi.sam636694.semelion.RowOrder
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.Matches
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.Participations
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatistics
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.User
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale.getDefault

class SemelionGameViewModel(
    matchesDao: MatchesDao,
    participationsDao: ParticipationsDao,
    matchStatisticsDao: MatchStatisticsDao,
    playersStatisticsDao: PlayerStatisticsDao,
    userDao: UserDao,
    player: AudioPlayer,
    userID:String,
    secondPlayerId:String
) : BaseGameViewModel(matchesDao, participationsDao, matchStatisticsDao, playersStatisticsDao, userDao,player,userID,secondPlayerId) {

    init {
        setup()
    }

    override fun setup(){
        val decks = createDecks()
        _uiState.value = GameUIState(
            grid = decks.first,
            uncoverDeck = decks.second,
            phase = GamePhase.Loading
        )
        viewModelScope.launch {
            if (_uiState.value.phase is GamePhase.Loading){
                val suspendedMatch = matchesDao.getSuspendedMatch()
                if ( suspendedMatch == null) matchStart(GameModes.ScreenSharing)
                else resumeMatch(suspendedMatch)
                _uiState.update { it.copy(phase = GamePhase.PlayerTurn) }
            }
        }
    }

    fun resumeMatch(matches: Matches){
        val state = matches.gameState
        Log.d("pino","sospesa")
        _uiState.update { state }
    }

    override fun destroy() {

    }

    //DB FUNCTIONS


    companion object {
        fun factory(
            matchesDao: MatchesDao,
            participationsDao: ParticipationsDao,
            matchStatisticsDao: MatchStatisticsDao,
            playerStatisticsDao: PlayerStatisticsDao,
            userDao: UserDao,
            player: AudioPlayer,
            userID:String,
            secondPlayerId:String
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    SemelionGameViewModel(
                        matchesDao,
                        participationsDao,
                        matchStatisticsDao,
                        playersStatisticsDao = playerStatisticsDao,
                        userDao = userDao,
                        player= player,
                        userID= userID,
                        secondPlayerId= secondPlayerId
                        )
                }
            }
        }
    }

}


fun List<CardUIStates>.findPowerRow(): Int{
    val order = this.getPredominantOrder()
    return when(order.size){
        1 ->  {
            val (_,second,third)= order.first()
            if (second == 7 || third == 7) 1 else 0
        }
        else -> 0
    }
}

//usarlo per la freccia
fun List<CardUIStates>.getRowOrder(index:Int) : RowOrder {
    //considero solo le carte rivelate
    val revealed = filter { it.isRevealed }
    //se ho un solo elemento allora va bene
    if (revealed.size<2) return RowOrder.BOTH
    //trovo il seme dominante
    //usare funzione
    val order = this.getPredominantOrder()
    Log.d("ORDER","Riga:$index, Tripla:$order")
    return when {
        order.size == 2 -> RowOrder.BOTH
        order.isNotEmpty() && order.first().second > order.first().third && order.first().second > 1  -> RowOrder.CRESCENT
        order.isNotEmpty() && order.first().second < order.first().third && order.first().third > 1 -> RowOrder.DECRESCENT
        else -> RowOrder.BOTH
    }


}

fun houseRowOrder(house:String,cards:List<CardUIStates>):Triple<String,Int,Int>{
    val houseCards = cards.filter { it.house == house && it.isRevealed }
    Log.d("HRO","Carte Rivelate:$houseCards")
    val crescentOrder = houseCards.foldRight(0){ houseCard,acc ->
        //TROVO L'INDICE DELLA CARTA
        val index = cards.indexOfFirst {  card ->
            // Log.d("CRESCENTORDER","card:${card.isRevealed},houseCard:${houseCard.isRevealed}, ${houseCard.isRevealed && card.isRevealed && (houseCard.name == card.name)}")
            card.name == houseCard.name
        }
        Log.d("HRO","index crescent:$index")
        //Log.d("CRESCENTORDER","index:$index,houseCard:${houseCard.value} formulaCrescente:${houseCard.value==index+1},formulaDecrescente:${houseCard.value == 7 - index }")
        when{
            index == -1 -> acc
            houseCard.value == index+1 -> acc+1
            else -> acc
        }
    }

    val decrescentOrder = houseCards.foldRight(0){ houseCard,acc ->
        val index = cards.indexOfFirst {  card -> card.name == houseCard.name }
        Log.d("DECRESCENTORDER","index:$index,houseCard:${houseCard.value} formulaCrescente:${houseCard.value==index+1},formulaDecrescente:${houseCard.value == 7 - index }")
        when{
            index == -1 -> acc
            houseCard.value == 7 - index -> acc+1
            else -> acc
        }
    }
    Log.d("HRO","Result Triple:$house,$crescentOrder,$decrescentOrder")
    return  Triple(house,crescentOrder,decrescentOrder)
}

//aggiustare caso limite in cui hai due triple con stesso numero di ordinamenti crescenti/decrescenti es ("P",2,0) e ("F",2,0) -> in realtà potrei accettare che solo 1 dei due ordinamenti vale
fun findMax(triples:List<Triple<String,Int,Int>>,parameter:String):Triple<String,Int,Int>?{
    if (triples.isEmpty()) return null
    var bestTriple = triples.first()
    when (parameter){
        "second" -> triples.forEach { triple -> if (triple.second > bestTriple.second) bestTriple = triple }
        "third" -> triples.forEach { triple -> if (triple.third > bestTriple.third) bestTriple = triple }
    }
    return bestTriple
}

fun List<CardUIStates>.getPredominantOrder():List<Triple<String,Int,Int>>{
    //considero solo le carte rivelate
    val revealed = filter { it.isRevealed }
    val houses = mutableSetOf<String>()

    revealed.forEach { card -> houses.add(card.house) }

    val triples: MutableList<Triple<String,Int,Int>> = mutableListOf()

    houses.forEach { house -> triples.add(houseRowOrder(house,this)) }

    if (triples.isEmpty()) return emptyList()

    val maxCrescent = findMax(triples,"second") ?: return emptyList()

    val maxDecrescent = findMax(triples,"third") ?: return emptyList()

    return when {
        maxCrescent.second > maxDecrescent.third ->  listOf(maxCrescent)
        maxCrescent.second < maxDecrescent.third ->  listOf(maxDecrescent)
        maxDecrescent.third == 0 -> listOf(Triple("None",0,0))
        else -> {
            val order = mutableListOf(maxCrescent)
            if (maxCrescent.first != maxDecrescent.first) order.add(maxDecrescent)
            return order
        }
    }
}