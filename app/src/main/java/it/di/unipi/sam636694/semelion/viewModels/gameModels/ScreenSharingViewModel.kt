package it.di.unipi.sam636694.semelion.viewModels.gameModels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import it.di.unipi.sam636694.semelion.utilities.RowOrder
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private var wasResumed = false

    var suspendedFound by mutableStateOf(false)

    private var resumedMatchId:Long = -5

    private var resumedMatchState: GameUIState = GameUIState()

    override fun setup(){
        val decks = createTestDecks()
        Log.d("finder","${decks.first.indexOfFirst { it.value == 8 }}")
//        Log.d("finder","${decks.second.indexOfFirst { it.value == 7 }}")
        _uiState.value = GameUIState(
            grid = decks.first,
            uncoverDeck = decks.second,
            phase = GamePhase.Loading
        )
        //imposto il primo giocatore
        setFirstPlayer()
        Log.d("coinFlip","turno dopo coinflip:${_uiState.value.p1Turn}")

        Log.d("decks","sevenPosition: ${_uiState.value.uncoverDeck.indexOfFirst { it.value == 7 }}\n")

        viewModelScope.launch {
            if (_uiState.value.phase is GamePhase.Loading){
                super.playerName = userDao.getUserById(userID)?.nickName ?: "Player 1"
                firstPlayerAvatar = userDao.getUserById(userID)?.avatar
                secondPlayerAvatar = R.drawable.sora_avatar
                val suspendedMatch = matchesDao.getSuspendedMatch()
                Log.d("Suspended","$suspendedMatch")
                if ( suspendedMatch == null) {
                    matchStart(GameModes.ScreenSharing)
                    Log.d("coinFlip","turno dopo coinflip:${_uiState.value.p1Turn}")
                    _uiState.update { it.copy(phase = GamePhase.PlayerTurn) }
                }
                else{
                    resumedMatchId = suspendedMatch.matchId
                    resumedMatchState = suspendedMatch.gameState
                    suspendedFound = true
                }
            }
        }
    }

    fun resumeMatch(){
        wasResumed = true
        _uiState.update { resumedMatchState }
    }

    fun newMatch(){
        viewModelScope.launch {
            matchStart(GameModes.ScreenSharing)
            Log.d("coinFlip","turno dopo coinflip:${_uiState.value.p1Turn}")
            _uiState.update { it.copy(phase = GamePhase.PlayerTurn) }
        }
    }

    override fun matchEnd(mode: GameModes,loser:String?,resumedMatchId:Long?) {
        Log.d("DBMS","resumed:$wasResumed, $resumedMatchId")
        super.matchEnd(mode, loser, resumedMatchId = if (wasResumed) this.resumedMatchId else null)
    }

    override fun destroy() {
    }

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

//trova le righe potenti
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

    houses.forEach { house ->
        Log.d("HRO","seme:$house")
        triples.add(houseRowOrder(house,this))
    }

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

fun List<CardUIStates>.getBonusActions():List<Triple<String,Int,Int>>{
    //considero solo le carte rivelate
    val revealed = filter { it.isRevealed }
    val houses = mutableSetOf<String>()

    revealed.forEach { card -> houses.add(card.house) }

    val triples: MutableList<Triple<String,Int,Int>> = mutableListOf()

    houses.forEach { house ->
        Log.d("HRO","seme:$house")
        triples.add(houseRowOrder(house,this))
    }
    return triples
}