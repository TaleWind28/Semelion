package it.di.unipi.sam636694.semelion.gameModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.di.unipi.sam636694.semelion.DELAY_TIME
import it.di.unipi.sam636694.semelion.JOLLY_COLOR
import it.di.unipi.sam636694.semelion.POSITION_VALUES
import it.di.unipi.sam636694.semelion.RowOrder
import it.di.unipi.sam636694.semelion.SEMELION_FIGURES
import it.di.unipi.sam636694.semelion.SharedRepository
import it.di.unipi.sam636694.semelion.UNCOVER_DECK_SIZE
import it.di.unipi.sam636694.semelion.actionTemplate
import it.di.unipi.sam636694.semelion.colorHouse
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.database.MatchStatistics
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.Matches
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.Participations
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatistics
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.User
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.mapHouse
import it.di.unipi.sam636694.semelion.utilities.SnackBarController
import it.di.unipi.sam636694.semelion.utilities.SnackBarEvent
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale.getDefault
import kotlin.collections.chunked
import kotlin.math.max

class SemelionGameViewModel(
    matchesDao: MatchesDao,
    participationsDao: ParticipationsDao,
    matchStatisticsDao: MatchStatisticsDao,
    playersStatisticsDao: PlayerStatisticsDao,
    userDao: UserDao
) : BaseGameViewModel(matchesDao, participationsDao, matchStatisticsDao, playersStatisticsDao, userDao) {



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
                //devo metterlo da un'altra parte
                if (userDao.getUserById(123L) == null) userDao.insert(User(123L, nickName = "pino"))
                if (userDao.getUserById(124L) == null) userDao.insert(User(124L, nickName = "pippo"))
                //caso specifico di partita in ScreenSharing
                val matchID = matchesDao.getNextMatchId()
                Log.d("DB","nextId = $matchID")
                matchesDao.insert(Matches(gameMode = GameModes.ScreenSharing, gameState = _uiState.value))
                participationsDao.insert(Participations(matchId= matchID, userId = 124L, role = "Host"))
                participationsDao.insert(Participations(matchId= matchID,userId = 123L, role = "Guest"))
                Log.d("DB","Initializeds")
                _uiState.update { it.copy(phase = GamePhase.PlayerTurn) }
            }
        }
    }

    //DB FUNCTIONS
    override fun matchEnd(){
        val outcome = this._uiState.value.winner ?: "interrotta"

        val winningUser =
            if (outcome.lowercase(getDefault()).contains("p1 vince")) 123L
            else if (outcome.lowercase(getDefault()).contains("p2 vince")) 124L
                else null

        _matchSummary.update { lists -> lists.map { it.copy(outcome = outcome) } }
        viewModelScope.launch {
            val matchId = matchesDao.getNextMatchId() - 1
            matchesDao.update(Matches(matchId = matchId,gameMode= GameModes.ScreenSharing,gameState=_uiState.value))
            Log.d("DB","$matchId")
            //update dei matchSummary
            matchSummary.value.forEach { stats ->
                val stat = stats.copy(matchId = matchId,outcome = outcome, winner = winningUser)
                Log.d("DB","inserting data: $stat")
                matchStatisticsDao.insert(stat)
            }
            //update dei playerSummary
            listOf(123L,124L).fold(0){ acc,userId ->
                Log.d("DB","inserting data for user:$userId")
                //se non ha delle statistiche le creo
                val playerStats = playersStatisticsDao.getStatsByUser(userId) ?: PlayerStatistics(userId, matchesPlayed = 0, matchesWon = 0, matchesLost = 0)
                val wins = if (winningUser == userId) playerStats.matchesWon.plus(1) else playerStats.matchesWon
                val losses = if (winningUser != userId) playerStats.matchesLost.plus(1) else playerStats.matchesLost

                if (playerStats.matchesPlayed == 0) playersStatisticsDao.insert(playerStats.copy(matchesPlayed = 1, matchesWon = wins, matchesLost = losses))
                else playersStatisticsDao.update(playerStats.copy(matchesPlayed = playerStats.matchesPlayed + 1, matchesWon = wins, matchesLost = losses))
                Log.d("DB","data userted for user:$userId")
                acc
            }
        }
    }

    companion object {
        fun factory(matchesDao: MatchesDao, participationsDao: ParticipationsDao, matchStatisticsDao: MatchStatisticsDao, playerStatisticsDao: PlayerStatisticsDao,userDao: UserDao): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    SemelionGameViewModel(matchesDao, participationsDao, matchStatisticsDao, playersStatisticsDao = playerStatisticsDao, userDao = userDao)
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