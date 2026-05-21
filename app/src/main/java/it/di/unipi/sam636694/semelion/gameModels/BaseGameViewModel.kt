package it.di.unipi.sam636694.semelion.gameModels

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.AudioPlayer
import it.di.unipi.sam636694.semelion.DELAY_TIME
import it.di.unipi.sam636694.semelion.JOLLY_COLOR
import it.di.unipi.sam636694.semelion.POSITION_VALUES
import it.di.unipi.sam636694.semelion.R
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
import it.di.unipi.sam636694.semelion.toFunction
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import it.di.unipi.sam636694.semelion.utilities.LogScreen
import it.di.unipi.sam636694.semelion.utilities.SnackBarController
import it.di.unipi.sam636694.semelion.utilities.SnackBarEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale.getDefault
import kotlin.collections.chunked
import kotlin.collections.find
import kotlin.collections.plus
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

abstract class BaseGameViewModel(
    val matchesDao: MatchesDao,
    val participationsDao: ParticipationsDao,
    val matchStatisticsDao: MatchStatisticsDao,
    val playersStatisticsDao: PlayerStatisticsDao,
    val userDao: UserDao,
    val player: AudioPlayer,
    val userID: String,
    var secondPlayerId: String
) : ViewModel(){

    protected val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()

    val isDBOperationComplete = MutableStateFlow(true)

    val wantsToGoBack = MutableStateFlow(false)

    val validationQueue = Channel<String>(Channel.BUFFERED)

    private val startTime:Long = System.currentTimeMillis()

    protected val _matchSummary = MutableStateFlow(listOf(
        MatchStatistics(matchId=-1, userId= userID,outcome = "still playing...",  figureRevealed = 0, winner = null, date = startTime, wasFirstPLayer = true,totalActions = 0),
        MatchStatistics(matchId=-1,userId= secondPlayerId,outcome = "still playing...",  figureRevealed = 0,winner = null, date=startTime, wasFirstPLayer = false,totalActions = 0)
    ))
    val matchSummary = _matchSummary.asStateFlow()


    //serve per mettere i dao nel viewmodel
    open fun validation(){
        viewModelScope.launch {
            for (cardId in validationQueue) {
                delay(200)
                validateState(cardId, _uiState.value)
            }
        }
    }


    //HANDLER PER GESTIRE EVENTI RELATIVI ALLA UI
    protected fun handleCardClicked(cardId: String): Boolean {

        val needsDelay = _uiState.value.grid
            .find { it.name == cardId }
            ?.let { it.value >= 7 } ?: false
        //azione per log
        val pos = _uiState.value.grid.indexOfFirst { it.name == cardId }
        val relevantCards = listOf(Triple(cardId ,pos , findCard(cardId,_uiState.value)?.isRevealed ?: false))

        _uiState.update { state ->
            val revealedCards = state.revealedCards + cardId
            state.copy(
                grid = revealOnGrid(revealedCards, state),
                revealedCards = revealedCards,
                phase = GamePhase.Validation
            )
        }

        val outcome = listOf(Triple(cardId ,pos , findCard(cardId,_uiState.value)?.isRevealed ?: false))

        sendScreenMessage("reveal",relevantCards, outcome =outcome)

        viewModelScope.launch {
            if (needsDelay) delay(300)
            _uiState.update { validateState(cardId, it) }
        }
        return true
    }

    protected fun handleSwapCards(id1: String, id2: String): Boolean {

        val card1 = findCard(id1, _uiState.value) ?: return false
        val card2 = findCard(id2, _uiState.value) ?: return false
        val message = canSwap(card1,card2,_uiState.value)

        if (message != null){
            viewModelScope.launch {
                SnackBarController.sendEvent(
                    event = SnackBarEvent(
                        message = message
                    )
                )
            }
            return false
        }

        val relevantCards = listOf(
            Triple(id1,_uiState.value.grid.indexOfFirst { it.name == id1 }, findCard(id1,_uiState.value)?.isRevealed ?: false),
            Triple(id2,_uiState.value.grid.indexOfFirst { it.name == id2 }, findCard(id2,_uiState.value)?.isRevealed ?: false)
        )

        _uiState.update {
            it.copy(
                grid = it.grid.map { card ->
                    when (card.name) {
                        id1 -> card2.copy()
                        id2 -> card1.copy()
                        else -> card.copy()
                    }
                },
                phase = GamePhase.Validation
            )
        }

        val outcome = listOf(
            Triple(id1,_uiState.value.grid.indexOfFirst { it.name == id1 }, findCard(id1,_uiState.value)?.isRevealed ?: false),
            Triple(id2,_uiState.value.grid.indexOfFirst { it.name == id2 }, findCard(id2,_uiState.value)?.isRevealed ?: false)
        )

        sendScreenMessage("swap",relevantCards,outcome)

        viewModelScope.launch {
            val needsDelay = _uiState.value.grid.any { it.name in listOf(id1, id2) && it.value >= 7 }
            if (!needsDelay) delay(DELAY_TIME)
            _uiState.update { validateState(id2,validateState(id1, it)) }
        }

        return true

    }

    protected fun handleJackMadness(swaps:List<Int>):Boolean{
       if (_uiState.value.phase !is GamePhase.JackMadness) return false
        player.playFile(R.raw.jack)

        val cardId = _uiState.value.grid[swaps.first()].name
        val position = swaps.first()
        //droppo il primo elemento che è la posizione attuale del jack
        val jackSwaps = swaps.drop(1)
        //se non ho altre pozioni termino
        if (jackSwaps.isEmpty()){
            _uiState.update { validateState(cardId = cardId,_uiState.value.copy(phase = GamePhase.Validation))}
            return false
        }

        Log.d("jackSwap","passo")

        val swapped =
            swaps.fold(cardId to _uiState.value) { (currentId, currentState), nextPosition ->
                val nextCard = currentState.grid[nextPosition]

                val currentPos = currentState.grid.indexOfFirst { it.name == currentId }

                Log.d("jackSwap", "from: $currentPos card: $currentId, to: $nextPosition, nextCard: ${nextCard.name}")

                currentId to figureSwap(currentId, nextCard.name, currentState, "Jack' chain")
            }

        _uiState.update { validateState(cardId =swapped.second.grid[position].name ,state=swapped.second.copy(phase = GamePhase.Validation)) }

        return true
    }

    protected fun handleQueenDirection(direction: (Int, Int) -> Int): Boolean {
        if (_uiState.value.phase !is GamePhase.QueenPending) return false
        player.playFile(R.raw.queen)

        _uiState.update { state ->

            val modifiedState = state.copy(
                grid = (0 until 3).fold(state) { acc, i ->
                    val id1 = findCard(acc.grid[direction(i, 0)].name, state)?.name ?: "none"
                    val id2 = findCard(acc.grid[direction(i, 7)].name, state)?.name ?: "none"
                    figureSwap(id1, id2, acc,"Queen'Swipe")
                }.grid,
                phase = GamePhase.Validation          // transizione dentro lo stato
            )

            (0 until 3 ).fold(modifiedState){ acc, i ->
                validateState(acc.grid[direction(i,0)].name, acc)

            }
        }


        viewModelScope.launch {
            val needsDelay = _uiState.value.grid
                .find { it.name == _uiState.value.lastReplacedCard }
                ?.let { it.value >= 7 } ?: false
            if (needsDelay) delay(DELAY_TIME)
            _uiState.update { validateState(it.lastReplacedCard ?: "queen Landing", it) }
        }
        return true
    }

    protected fun handleKingDirection(rowIndex:Int, direction: (Int, Int) -> Int) : Boolean {
        //controllo di essere nello stato giusto
        if (_uiState.value.phase !is GamePhase.KingPending) return false
        player.playFile(R.raw.king)
        //controllo che il giocatore non abbia selezionato una riga potente
        if (_uiState.value.grid.chunked(7)[rowIndex].findPowerRow() == 1){
            viewModelScope.launch {
                SnackBarController.sendEvent(
                    event = SnackBarEvent(
                        message = "Non puoi spostare Righe Potenti"
                    )
                )
            }
            return false
        }

        //aggiorno lo stato shiftando la griglia
        _uiState.update { state ->
            var modifiedState = state.copy(
                grid = (0 until 6).fold(state) { acc, i ->
                    val id1 = findCard(acc.grid[direction(i, 0)].name, state)?.name ?: "none"
                    val id2 = findCard(acc.grid[direction(i, 1)].name, state)?.name ?: "none"
                    figureSwap(id1, id2, acc,"King's Rule")
                }.grid,
                phase = GamePhase.Validation          // transizione dentro lo stato
            )
            modifiedState = (0 until 6 ).fold(modifiedState){ acc, i ->
                validateState(acc.grid[(rowIndex * 7) + i].name, acc)
            }
            modifiedState

        }

        viewModelScope.launch {
            val needsDelay = _uiState.value.grid
                .find { it.name == _uiState.value.lastReplacedCard }
                ?.let { it.value >= 7 } ?: false
            if (needsDelay) delay(DELAY_TIME)
            _uiState.update { validateState(it.lastReplacedCard ?: "king's cross", it) }
        }

        return true
    }

    //FUNZIONI HELPER PER GLI HANDLER
    fun generateJackChain(state: GameUIState,jackHouse:String,swapCount:Int):List<Int>{
        val validPositions = (0..27).filter { colorHouse(state.grid[it].house) == colorHouse(jackHouse) }
        val positions = mutableListOf<Int>()
        Log.d("jackSwap","numeri: $swapCount")
        repeat(swapCount) {
            val last = positions.lastOrNull()
            positions.add(validPositions.filter { it != last }.random())
        }
        Log.d("jackSwap","posizioni: $positions")
        positions.forEach { Log.d("jackSwap","${state.grid[it]}") }
        return positions
    }

    fun canSwap(card1: CardUIStates,card2: CardUIStates,state: GameUIState): String?{
        val row = state.grid.chunked(7)
        val rows = Pair(row[0] + row[1],row[2] + row[3])

        val cardInfos = Pair(
            state.grid.indexOfFirst { it.name== card1.name }.let{it to it/7},
            state.grid.indexOfFirst { it.name== card2.name }.let { it to it/7 },
        )

        fun valueControl(rowId:Int,globalPosition:Int,value:Int): Boolean{
            return when (value) {
                POSITION_VALUES.first(rowId,globalPosition)-> true
                POSITION_VALUES.second(rowId,globalPosition) -> true
                else -> false
            }
        }

        fun fairnessControl(card1: CardUIStates,card2: CardUIStates,playerTurn: Boolean):Boolean{
            return when{
                //turno di p2 carte nelle sue righe
                !playerTurn && rows.first.find { it.name == card1.name } != null && rows.first.find { it.name == card2.name } != null ->  true
                //turno di p1 con carte solo sue
                playerTurn && rows.second.find { it.name == card1.name } != null && rows.second.find { it.name == card2.name } != null ->  true
                //il controllo su second o first è indifferente in quanto avere un null in uno dei find implica la presenza dell'altra carta nelle righe dell'oppo
                rows.second.find { it.name == card1.name } != null && rows.second.find { it.name == card2.name } == null ->  true
                rows.second.find { it.name == card1.name } == null && rows.second.find { it.name == card2.name } != null ->  true
                else -> false
            }
        }

        fun errorMessage(positionValid: Boolean,fairness: Boolean):String?{
            return if (!positionValid && !fairness){
                "Lo scambio deve consentire ad almeno una carta di essere in posizione corretta e rispettare le regole di correttezza!"
            }
            else if (!positionValid){
                "Lo scambio deve consentire ad almeno una carta di essere in posizione corretta!"

            }else if(!fairness) {
                "Lo scambio deve seguire le regole di correttezza"
            }
            else{
                null
            }
        }

        Log.d("swap","$cardInfos")

        if (row[cardInfos.first.second].findPowerRow() == 1 || row[cardInfos.second.second].findPowerRow() == 1) {
            return "Non puoi scambiare carte appartenenti ad una riga potente"
        }

        val c2SwapValid = !card2.isRevealed || card2.name.contains("joker") ||
                valueControl(rowId= cardInfos.first.second,globalPosition= cardInfos.first.first, value= card2.value)

        val c1SwapValid = !card1.isRevealed || card1.name.contains("joker") ||
                valueControl(rowId= cardInfos.second.second,globalPosition= cardInfos.second.first,value= card1.value)

        val positionValid = when{
            !card1.isRevealed && !card2.isRevealed -> true
            !card1.isRevealed -> c2SwapValid
            !card2.isRevealed -> c1SwapValid
            else -> c1SwapValid || c2SwapValid
        }

        return  errorMessage(positionValid,fairnessControl(card1,card2,state.p1Turn))
    }

    //FUNZIONE DI VALIDAZIONE
    open fun validateState(cardId: String, state: GameUIState): GameUIState {

        val card = findCard(cardId, state) ?: return state

        var modifiedState = state

        //controllo se la carta è rivelata
        if (card.isRevealed) {
            //controlla se la carta rivelata è una figura
            modifiedState = figureRevealed(cardId, modifiedState)
        }

        //cercare jolly sulla griglia -> actually vorrei usare uno stato diverso per le invocazioni però non so
        JOLLY_COLOR.forEach {
            modifiedState = jollyApplier(modifiedState, "joker_$it")
            modifiedState = substituteJolly(modifiedState, "joker_$it")
        }

        //controllo se la carta deve essere coperta
        modifiedState = coverCard(cardId,modifiedState)
        //se sono ancora in fase di validazione modifico il numero di azioni
        if (modifiedState.phase is GamePhase.Validation || modifiedState.incorrectSevenReveled){
            Log.d("actions", "calcolo azioni: $cardId, seven:${modifiedState.incorrectSevenReveled}")
            //aggiorna azioni
            val (p1Actions, p2Actions) = increaseUsedActions(modifiedState)
            modifiedState = modifiedState.copy(
                p1ActionsUsed = p1Actions,
                p2ActionsUsed = p2Actions
            )
            modifiedState = actionCounter(modifiedState, modifiedState.grid.chunked(7))
            _matchSummary.update { summary ->
                if(state.p1Turn)
                    listOf(summary.first().copy( totalActions = summary.first().totalActions +1),summary.last())
                else
                    listOf(summary.first(),summary.last().copy(totalActions = summary.last().totalActions + 1))
            }
        }

        //controllo se un giocatore ha vinto
        modifiedState = findWinner(modifiedState)

        //se la validazione ha avuto successo torna al turno del giocatore
        return if (modifiedState.phase == GamePhase.Validation) {
            Log.d("coinFlip","p1Turn:${modifiedState.p1Turn}")
            modifiedState.copy(
                phase = GamePhase.PlayerTurn
            )
        } else {
            //altrimenti maniteni lo stato attuale per risolvere gli effetti delle figure
            modifiedState
        }
    }

    //funzioni di utility
    fun createDecks(): Pair<List<CardUIStates>, List<CardUIStates>> {

        val allCards = createCards(figures = SEMELION_FIGURES, jolly = JOLLY_COLOR).shuffled()

        //testing filtro solo carte <7
        val noFiguresDeck = allCards.filter { it.value in 1..7 }

        val specialDeck = allCards.filter { it.value > 7 || it.value == 0 }

        val gridDeck = noFiguresDeck.drop(UNCOVER_DECK_SIZE) + specialDeck

        val uncoverDeck = noFiguresDeck.take(UNCOVER_DECK_SIZE).map { it.copy(isRevealed = true) }

        return Pair(gridDeck.shuffled(), uncoverDeck.shuffled())

    }

    fun createCards(figures: List<Pair<Int, String>>, jolly: List<String>): List<CardUIStates> {
        return buildList {

            for (i in 1..4) {
                val currentHouse = mapHouse(i)
                //aggiungi tutto
                for (j in 1..7) {
                    add(
                        CardUIStates(
                            name = "$j$currentHouse",
                            value = j,
                            house = currentHouse,
                            isRevealed = false
                        )
                    )
                }
            }

            //aggiungo jack, donne e re
            figures.forEach {
                add(
                    CardUIStates(
                        name = "${it.first}${it.second}",
                        value = it.first,
                        house = it.second,
                        isRevealed = false
                    )
                )
            }

            //aggiungo i jolly
            jolly.forEach {
                add(
                    CardUIStates(
                        name = "joker_$it",
                        value = 0,
                        house = it,
                        isRevealed = false
                    )
                )
            }
        }
    }

    fun revealOnGrid(revealedCards: List<String>, state: GameUIState): List<CardUIStates> {
        return state.grid.map { card ->
            card.copy(
                isRevealed = card.name in revealedCards,
            )
        }
    }

    fun coverCard(cardId: String, state: GameUIState): GameUIState {

        val selectedCard = findCard(cardId, state) ?: return state

        if (selectedCard.value != 7 || !selectedCard.isRevealed) return state
        Log.d("validate","cardToValidate: $cardId")
        val position = state.grid.indexOfFirst { card -> card.name == selectedCard.name }

        //controllo se il 7 può essere rivelato
        if (position % 7 == 0 || position % 7 == 6) return state

        val revealedCards = state.revealedCards - cardId

        val relevantCards = listOf(Triple(cardId,position,true))
        val outcome = listOf(Triple(cardId,position,false))

        sendScreenMessage("covered",relevantCards,outcome)

        player.playFile(R.raw.seven)

        return state.copy(
            grid = revealOnGrid(revealedCards, state),
            revealedCards = revealedCards,
            incorrectSevenReveled = true,
        )
    }

    fun figureSwap(id1: String, id2: String, state: GameUIState, type: String): GameUIState {
        val card1 = findCard(id1, state) ?: return state
        val card2 = findCard(id2, state) ?: return state

        val relevantCards = listOf(
            Triple(id1,state.grid.indexOfFirst { it.name == id1 }, findCard(id1,state)?.isRevealed ?: false),
            Triple(id2,state.grid.indexOfFirst { it.name == id2 }, findCard(id2,state)?.isRevealed ?: false)
        )

        val modifiedState = state.copy(
            grid = state.grid.map { card ->
                when (card.name) {
                    id1 -> card2.copy()
                    id2 -> card1.copy()
                    else -> card.copy()
                }
            },
        )

        val outcome = listOf(
            Triple(id1,modifiedState.grid.indexOfFirst { it.name == id1 }, findCard(id1,modifiedState)?.isRevealed ?: false),
            Triple(id2,modifiedState.grid.indexOfFirst { it.name == id2 }, findCard(id2,modifiedState)?.isRevealed ?: false)
        )

        sendScreenMessage(type,relevantCards,outcome)

        return modifiedState
    }

    fun increaseUsedActions(state: GameUIState): Pair<Int, Int> {
        return if (state.p1Turn) {
            Pair(state.p1ActionsUsed + 1, state.p2ActionsUsed)
        } else {
            Pair(state.p1ActionsUsed, state.p2ActionsUsed + 1)
        }
    }

    fun swapJolly(state: GameUIState, suit: String): GameUIState {
        val jolly = findCard(suit, state) ?: return state
        Log.d("jolly", "jolly: $jolly")
        val correctCard = findCard("${jolly.value}${jolly.house}", state) ?: return state
        Log.d("jolly", "carta corretta:${correctCard.name},jolly: $jolly")

        if (!correctCard.isRevealed || !jolly.isRevealed) return state

        return state.copy(
            grid = state.grid.map {
                when (it.name) {
                    suit -> correctCard.copy()
                    correctCard.name -> jolly.copy()
                    else -> it.copy()
                }
            },

            revealedCards = if (correctCard.value == 7) {
                state.revealedCards + correctCard.name
            } else state.revealedCards,
            incorrectSevenReveled = false
        )
    }

    fun substituteJolly(state: GameUIState, suit: String): GameUIState {
        val currentState = swapJolly(state, suit)
        return if (currentState == state) state
        else replaceCard(currentState, suit)
    }

    fun jollyApplier(state: GameUIState, cardId: String): GameUIState {
        //cerco il jolly
        val jolly = findCard(cardId, state) ?: return state
        //ha senso continuare solo se il jolly è rivelato
        if (!jolly.isRevealed) return state

        //posizione in griglia
        val pos = state.grid.indexOfFirst { it == jolly }

        //il diviso funge da modulo perchè sto usando gli interi
        val row = pos / 7

        val orders = state.grid.chunked(7)[row].getPredominantOrder()
        if (orders.isEmpty()) return state

        //trovo il colore della riga
        var dominantHouse = colorHouse(orders.first().first)

        //per adesso lo ignoro -> tripla d'esempio (F,2,0) (C,0,2) (D,0,2) -> ritestare ma credo funzioni
        var i = 0
        orders.forEach {
            val house = colorHouse(it.first)
            JOLLY_COLOR.forEach { color ->
                if (cardId.contains(color) && house == color) {
                    dominantHouse = house
                    i += 1
                    return@forEach
                }
            }
        }

        //ho due case del colore del jolly -> posso ignorarlo
        if (i != 1) {
            return state.copy(
                grid = state.grid.map { card ->
                    when (card.name) {
                        cardId -> CardUIStates(
                            name = jolly.name,
                            value = 0,
                            house = colorHouse(jolly.house).lowercase(),
                            isRevealed = true
                        )

                        else -> card.copy()
                    }
                }
            )
        }

        //viene fatto solo per orders.size = 2 ed è corretto
        val jollyOrder = orders.filter { colorHouse(it.first) == dominantHouse }

        val value =
            when (max(jollyOrder.first().second, jollyOrder.first().third)) {
                orders.first().second -> pos - 7 * row + 1
                orders.first().third -> 7 * (row + 1) - pos
                else -> 0
            }

        //assume il valore in base all'ordinamento predominante
        Log.d("validate", "valore calcolato:$value, seme:$dominantHouse")

        return state.copy(
            grid = state.grid.map { card ->
                when (card.name) {
                    cardId -> CardUIStates(
                        name = jolly.name,
                        value = value,
                        house = jollyOrder.first().first,
                        isRevealed = true
                    )

                    else -> card.copy()
                }
            }
        )
    }

    fun figureRevealed(cardId: String, state: GameUIState): GameUIState {
        val card = findCard(cardId, state) ?: return state
        if (card.value < 8) return state
        val position = _uiState.value.grid.indexOfFirst { it.name == card.name }

        var modifiedState = state

        when {
            cardId.contains("8") -> { //circular swap
                player.playFile(R.raw.jackannouncer)
                modifiedState = modifiedState.copy(
                    jackSwaps =  listOf(position) + generateJackChain(modifiedState,card.house,modifiedState.uncoverDeck.first().value-1),
                    phase = GamePhase.JackMadness
                )
            }

            cardId.contains("9") -> { //swipe column
                player.playFile(R.raw.queenannouncer)
                modifiedState = modifiedState.copy(
                    phase = GamePhase.QueenPending
                )
            }

            cardId.contains("10") ->{//swipe row
                player.playFile(R.raw.kingannouncer)
                modifiedState = modifiedState.copy(
                    phase = GamePhase.KingPending
                )
            }


        }

        _matchSummary.update { summary ->
            summary.first().figureRevealed
            if(state.p1Turn)
                listOf(summary.first().copy(figureRevealed= summary.first().figureRevealed + 1),summary.last())
            else
                listOf(summary.first(),summary.last().copy(figureRevealed= summary.last().figureRevealed + 1))
        }

        modifiedState = replaceCard(modifiedState, cardId)

        return modifiedState
    }

    fun replaceCard(state: GameUIState, cardID: String): GameUIState {
        val card = findCard(cardID, state) ?: return state
        if (!card.isRevealed) return state
        val newUncover = state.uncoverDeck
        val nextCard = state.uncoverDeck.first()
        val pos = state.grid.indexOfFirst { it.name==cardID }
        val relevantCards = listOf(Triple(cardID,pos, true))
        val outcome = listOf(Triple(nextCard.name,pos, true))

        sendScreenMessage("addedFromUncover",relevantCards,outcome)
        state.uncoverDeck.forEach { Log.d("uncover",it.name) }
        Log.d("uncover","carta:${nextCard.name}")
        return validateState(cardId=nextCard.name,state=state.copy(
            grid = state.grid.map {
                when (it.name) {
                    card.name -> nextCard.copy()
                    else -> it.copy()
                }
            },
            uncoverDeck = newUncover - nextCard,
            revealedCards = state.revealedCards + nextCard.name,
            lastReplacedCard = nextCard.name
        )
        )


    }

    fun actionCounter(state: GameUIState, rows: List<List<CardUIStates>>): GameUIState {
        val p1Actions = calcActions(listOf(rows[2], rows[3]))
        val p2Actions = calcActions(listOf(rows[0], rows[1]))
        //mi sembrava più elegante farlo così
        val isSecondPlayer = {player:Boolean, turn:Int-> (if((!player && turn == 0) || (player && turn == 1)) 1 else 0)}
        Log.d("coinFlip","turno:${state.turnsPlayed}\np1:${isSecondPlayer(state.p1Turn,state.turnsPlayed)}\np2:${isSecondPlayer(!state.p1Turn,state.turnsPlayed)}")

        return when{
            //se un 7 è stato coperto il turno termina
            state.incorrectSevenReveled ->
                state.copy(
                    p1Actions = p1Actions,
                    p2Actions = p2Actions,
                    p1ActionsUsed = 0,
                    p2ActionsUsed = 0,
                    p1Turn = !state.p1Turn,
                    turnsPlayed = state.turnsPlayed+1,
                    incorrectSevenReveled = false,
                    phase = GamePhase.Validation)
            //se il giocatore ha terminato le azioni il turno termina
            //fine turno p1
            p1Actions + isSecondPlayer(state.p1Turn,state.turnsPlayed) - state.p1ActionsUsed  <=0 ->
                state.copy(
                    p1Actions = p1Actions + isSecondPlayer(state.p1Turn,state.turnsPlayed+1),
                    p1ActionsUsed = 0,
                    p1Turn = false,
                    p2Actions = p2Actions + isSecondPlayer(state.p1Turn,state.turnsPlayed+1),
                    turnsPlayed = state.turnsPlayed+1
                )
            //fine turno p2
            p2Actions + isSecondPlayer(!state.p1Turn,state.turnsPlayed) - state.p2ActionsUsed  <=0 ->
                state.copy(
                    p1Actions = p1Actions + isSecondPlayer(!state.p1Turn,state.turnsPlayed+1),
                    p2ActionsUsed = 0,
                    p1Turn = true,
                    p2Actions = p2Actions + isSecondPlayer(!state.p1Turn,state.turnsPlayed+1),
                    turnsPlayed = state.turnsPlayed+1
                )

            else ->
                state.copy(
                    p1Actions=p1Actions+ isSecondPlayer(state.p1Turn,state.turnsPlayed),
                    p2Actions = p2Actions + isSecondPlayer(!state.p1Turn,state.turnsPlayed)
                )
        }
    }

    fun findCard(cardID: String, state: GameUIState): CardUIStates? {
        return state.grid.find { it.name == cardID }
    }

    fun calcActions(rows: List<List<CardUIStates>>): Int {
        return 1 + rows.sumOf { row ->
            val revealed = row.filter { it.isRevealed }
            if (revealed.size < 2) return@sumOf 0
            //regola alternativa -> abbiamo visto che porta al bullismo
            //val houseActions = row.getBonusActions()
            //houseActions.sumOf { triple ->  triple.second / 2 + triple.third / 2}
            row.getPredominantOrder().sumOf { triple -> max(triple.second, triple.third) / 2 }
        }
    }

    fun findWinner(state: GameUIState): GameUIState {
        val rows = state.grid.chunked(7)
        val p2r = listOf(rows[0], rows[1])
        val p1r = listOf(rows[2], rows[3])

        val predRows = Pair(
            first = p2r.fold(0) { acc:Int, row ->
                acc + row.findPowerRow()
            },
            second = p1r.fold(0){ acc, row ->
                acc + row.findPowerRow()
            }
        )
        Log.d("winner","p2:${predRows.first}, p1:${predRows.second} ")

        return when{
            (predRows.first == 2 && predRows.second == 2) -> state.copy( winner = "Pareggio", phase = GamePhase.GameOver)
            predRows.first == 2 -> state.copy( winner = "Vince p2", phase = GamePhase.GameOver)
            predRows.second == 2 -> state.copy( winner = "Vince p1", phase = GamePhase.GameOver)
            else -> state
        }
    }

    //messaggio di log
    fun sendScreenMessage(type:String, relevantCards:List<Triple<String,Int, Boolean>>, outcome:List<Triple<String,Int, Boolean>>){
        viewModelScope.launch {
            SharedRepository.send(
                actionTemplate(
                    type = type,
                    relevantCards = relevantCards,
                    outcome = outcome
                )
            )
        }

    }

    //potrebbe finire in NVM
    fun blankGrid(){
        _uiState.update { it.copy(grid = emptyList(), uncoverDeck = emptyList()) }
    }

    abstract fun setup()
    open fun processIntent(intent: GameIntent): Boolean {
        Log.d("MVI", "Intent: $intent | Phase: ${_uiState.value.phase}")
        return when (intent) {
            is GameIntent.CardClicked -> handleCardClicked(intent.cardId)
            is GameIntent.SwapCards -> handleSwapCards(intent.id1, intent.id2)
            is GameIntent.QueenDirectionChosen -> handleQueenDirection(intent.direction.toFunction(intent.colIndex))
            is GameIntent.KingDirectionChosen -> handleKingDirection(intent.rowIndex, intent.direction.toFunction(intent.rowIndex))
            is GameIntent.JackMadness -> handleJackMadness(swaps=intent.jackSwaps)
            else -> false
        }
    }

    open fun calculateOutcome(loser:String?,state: GameUIState):Pair<String,Boolean?>{
        val outcome = loser ?: state.winner ?: "interrotta"
        Log.d("outcome","$outcome, ${state.winner}")
        return when(loser){
            userID -> "vince $secondPlayerId" to false
            secondPlayerId -> "vince $userID" to true
            else -> {
                if (outcome.lowercase(getDefault()).contains("vince p1")) outcome to true
                else if (outcome.lowercase(getDefault()).contains("vince p2")) outcome to false
                else if (outcome == userID) outcome to true
                else if (outcome == secondPlayerId) outcome to false
                else outcome to null
            }
        }
    }

    open fun matchEnd(mode: GameModes,loser:String? = null,resumedMatchId:Long? = null){
        //calcolo l'outcome della partita
        val (outcome,winningUser) = calculateOutcome(loser,_uiState.value)

        isDBOperationComplete.value = false

        _matchSummary.update { lists -> lists.map { it.copy(outcome = outcome) } }

        viewModelScope.launch {
            val matchId = resumedMatchId ?: (matchesDao.getNextMatchId() - 1)
            //inserisco il match nel db
            matchesDao.update(Matches(matchId = matchId,gameMode= mode,gameState=_uiState.value, isCompleted = true))
            Log.d("DBMS","$matchId")
            //update dei matchSummary
            matchSummary.value.forEach { stats ->
                val stat = stats.copy(matchId = matchId,outcome = outcome, winner = winningUser)
                Log.d("DB","inserting data: $stat")
                matchStatisticsDao.upsert(stat)
            }

            //update dei playerSummary
            listOf(userID,secondPlayerId).forEach{ userId ->
                Log.d("DB","inserting data for user:$userId")
                //se non ha delle statistiche le creo

                //winninguser sarà true se il vincitore è l'utente locale, false se il vincitore è l'avversario, null altrimenti
                var stats = PlayerStatistics(
                    userId= userId,
                    matchesPlayed = 1,
                    matchesWon = if (winningUser == true) 1 else 0,
                    matchesLost=if (winningUser == false) 1 else 0,
                    matchesDrawn = if (winningUser == null) 1 else 0
                )

                //controllo la streak
                if (outcome.contains(userId)){
                    val currStr = stats.currentStreak + 1
                    //Log.d("DB","pre update:\nstreak:$currStr\nbest:${stats.bestStreak}")
                    if (currStr > stats.bestStreak){
                        //Log.d("DB","pre update:\nstreak:$currStr\nbest:${stats.bestStreak}")
                        stats = stats.copy(currentStreak = currStr, bestStreak = currStr)
                    }
                    //Log.d("DB","pre update:$stats\nstreak:$currStr")
                }
                else{
                    stats = stats.copy(currentStreak = 0)
                }

                //ottengo le statistiche del player dal db
                val playerStats: PlayerStatistics? = playersStatisticsDao.getStatsByUser(userId)

                //se non ho la riga allora la creo con le stats ottenute attualmente
                if (playerStats == null ){
                    playersStatisticsDao.insert(stats.copy(currentStreak = if (stats.matchesWon == 1) 1 else 0, bestStreak = if (stats.matchesWon == 1) 1 else 0))
                    return@forEach
                }
                //Log.d("DBMS","$playerStats")

                //aggiorno stats in base ai valori presi dal db
                stats = stats.copy(
                    matchesPlayed = playerStats.matchesPlayed + stats.matchesPlayed,
                    matchesWon = playerStats.matchesWon + stats.matchesWon,
                    matchesLost = playerStats.matchesLost + stats.matchesLost,
                    matchesDrawn = playerStats.matchesDrawn + stats.matchesDrawn,

                )

                //aggiorno la entry nel database
                playersStatisticsDao.update(stats)
            }
//            Log.d("DB","prima")
            //ritorna una lista di matchStatistics
//            val playerStats = matchStatisticsDao.getPlayerStatsFromMatch(matchId)
//            Log.d("DB","$playerStats")
            isDBOperationComplete.value = true
        }
    }

    open fun interruptMatch(mode: GameModes){
        isDBOperationComplete.value = false

        viewModelScope.launch {
            val matchId = matchesDao.getNextMatchId() - 1

            matchesDao.update(Matches(matchId = matchId,gameMode= mode,gameState=_uiState.value,isCompleted = false))
            Log.d("DB","$matchId")

            //controllo che non ci siano altre partite sospese
            val suspendedMatches = matchesDao.getSuspendedCount()
            if (suspendedMatches >1) matchesDao.deleteAllExceptLast()

            //salvataggio matchSummary in caso di ripresa della partita
            matchSummary.value.forEach { stats ->
                val stat = stats.copy(matchId = matchId,outcome = "interrupted", winner = null)
                Log.d("DB","inserting data: $stat")
                matchStatisticsDao.upsert(stat)
            }
            isDBOperationComplete.value = true
        }
    }

    suspend fun matchStart(mode: GameModes, nickname:String? = null){
        //imposto il primo giocatore
        setFirstPlayer()
        //devo metterlo da un'altra parte
        updateUsers(nickname=nickname)
        val matchID = matchesDao.getNextMatchId()
        //inserisco il match nel db
        matchesDao.insert(Matches(gameMode = mode, gameState = _uiState.value, isCompleted = false))
        //inserisco le partecipazioni nel db
        participationsDao.insert(Participations(matchId= matchID, userId = secondPlayerId, role = "Guest"))
        participationsDao.insert(Participations(matchId= matchID,userId = userID, role = "Host"))
        //flag per consentire modifiche in sicurezza nella UI
        isDBOperationComplete.value = true
    }

    open fun setFirstPlayer() {
        val coinFlip = Random.nextBoolean()
        if(coinFlip)
            this._uiState.update { it.copy(firstPlayer = "Host", p2Actions = it.p2Actions+1)}
        else
            this._uiState.update { it.copy(firstPlayer = "Guest", p1Turn = false, p1Actions=it.p1Actions + 1)}
        Log.d("coinFlip","coinFlip:$coinFlip\n${_uiState.value.firstPlayer},\np1Actions:${_uiState.value.p1Actions}\np2Actions:${_uiState.value.p2Actions}")
    }

    suspend fun updateUsers(nickname:String?){
        //controllo se devo creare l'utente nel db
        if (userDao.getUserById(userID)== null) userDao.insert(User(userID, nickName = "Semelion_User: $userID"))
        //controllo se esiste l'avversario nel db
        val opponent = userDao.getUserById(secondPlayerId)
        //controllo se in caso l'avversario esista il nickname sia diverso da quello in memoria, solo se il nickname non è null
        if (opponent == null) userDao.insert(User(secondPlayerId, nickName = nickname ?: "Sora"))
        else if (opponent.nickName != nickname && nickname!= null) userDao.update(User(secondPlayerId,nickname))
        Log.d("DB","secondPlayerID:$secondPlayerId")
    }

    protected fun updateSecondPlayer(secondPlayerId: String){
        this.secondPlayerId = secondPlayerId
        _matchSummary.update { listOf(it.first(),it.last().copy(userId=secondPlayerId)) }
    }

    abstract fun destroy()
}