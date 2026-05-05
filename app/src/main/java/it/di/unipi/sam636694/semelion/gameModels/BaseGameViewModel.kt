package it.di.unipi.sam636694.semelion.gameModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.DELAY_TIME
import it.di.unipi.sam636694.semelion.JOLLY_COLOR
import it.di.unipi.sam636694.semelion.POSITION_VALUES
import it.di.unipi.sam636694.semelion.SEMELION_FIGURES
import it.di.unipi.sam636694.semelion.SharedRepository
import it.di.unipi.sam636694.semelion.UNCOVER_DECK_SIZE
import it.di.unipi.sam636694.semelion.actionTemplate
import it.di.unipi.sam636694.semelion.colorHouse
import it.di.unipi.sam636694.semelion.database.MatchStatistics
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.mapHouse
import it.di.unipi.sam636694.semelion.toFunction
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import it.di.unipi.sam636694.semelion.utilities.SnackBarController
import it.di.unipi.sam636694.semelion.utilities.SnackBarEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.chunked
import kotlin.collections.find
import kotlin.collections.plus
import kotlin.math.max

abstract class BaseGameViewModel(
    val matchesDao: MatchesDao,
    val participationsDao: ParticipationsDao,
    val matchStatisticsDao: MatchStatisticsDao,
    val playersStatisticsDao: PlayerStatisticsDao,
    val userDao: UserDao
) : ViewModel(){

    protected val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()

    val validationQueue = Channel<String>(Channel.BUFFERED)

    protected val _matchSummary = MutableStateFlow(listOf(
        MatchStatistics(matchId=3, userId= 123L,outcome = "still playing...",  figureRevealed = 0, winner = null,totalActions = 0),
        MatchStatistics(matchId=3,userId= 124L,outcome = "still playing...",  figureRevealed = 0,winner = null,totalActions = 0)
    ))
    val matchSummary = _matchSummary.asStateFlow()


    //serve per mettere i dao nel viewmodel

    fun validation(){
        viewModelScope.launch {
            for (cardId in validationQueue) {
                delay(200)
                validateState(cardId, _uiState.value)
            }
        }
    }


    protected fun handleCardClicked(cardId: String): Boolean {
        if (_uiState.value.phase !is GamePhase.PlayerTurn) {
            viewModelScope.launch {
                SnackBarController.sendEvent(
                    event = SnackBarEvent(
                        message = "Risolvi prima l'effetto della figura"
                    )
                )
            }
            return false
        }

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
        if (_uiState.value.phase !is GamePhase.PlayerTurn){
            viewModelScope.launch {
                SnackBarController.sendEvent(
                    event = SnackBarEvent(
                        message = "Risolvi prima l'effetto della figura"
                    )
                )
            }
            return false
        }

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
            _uiState.update { validateState(id1, it) }
        }

        return true

    }

    protected fun handleQueenDirection(direction: (Int, Int) -> Int): Boolean {
        if (_uiState.value.phase !is GamePhase.QueenPending) return false

        _uiState.update { state ->
            state.copy(
                grid = (0 until 3).fold(state) { acc, i ->
                    val id1 = findCard(acc.grid[direction(i, 0)].name, state)?.name ?: "none"
                    val id2 = findCard(acc.grid[direction(i, 7)].name, state)?.name ?: "none"
                    figureSwap(id1, id2, acc,"Queen'Swipe")
                }.grid,
                phase = GamePhase.Validation          // transizione dentro lo stato
            )
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
            state.copy(
                grid = (0 until 6).fold(state) { acc, i ->
                    val id1 = findCard(acc.grid[direction(i, 0)].name, state)?.name ?: "none"
                    val id2 = findCard(acc.grid[direction(i, 1)].name, state)?.name ?: "none"
                    figureSwap(id1, id2, acc,"King's Rule")
                }.grid,
                phase = GamePhase.Validation          // transizione dentro lo stato
            )
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

    fun validateState(cardId: String, state: GameUIState): GameUIState {
        Log.d("Validate", cardId)
        val card = findCard(cardId, state) ?: return state
        var modifiedState = state

        if (card.isRevealed) {
            //controlla se la carta rivelata
            // è una figura
            modifiedState = figureRevealed(cardId, modifiedState)
        }

        //cercare jolly sulla griglia -> actually vorrei usare uno stato diverso per le invocazioni però non so
        JOLLY_COLOR.forEach {
            modifiedState = jollyApplier(modifiedState, "joker_$it")
            Log.d(
                "jolly",
                "${findCard("joker_$it", modifiedState)?.name}:${
                    findCard(
                        "joker_$it",
                        modifiedState
                    )?.value
                }"
            )
            //controllo se il jolly può essere sostituito
            modifiedState = substituteJolly(modifiedState, "joker_$it")
        }

        //controllo se devo coprire delle carte
        modifiedState = modifiedState.grid.chunked(7).flatten().fold(modifiedState) { state, card ->
            coverCard(card.name, state)
        }

        if (modifiedState.phase is GamePhase.Validation){
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

        modifiedState = findWinner(modifiedState)

        Log.d("validate","fase: ${modifiedState.phase}")

        return if (modifiedState.phase == GamePhase.Validation) {
            modifiedState.copy(
                phase = GamePhase.PlayerTurn
            )
        } else {
            modifiedState  // mantiene QueenPending o KingPending
        }
    }

    //funzioni di utility
    fun createDecks(): Pair<List<CardUIStates>, List<CardUIStates>> {

        val allCards = createCards(figures = SEMELION_FIGURES, jolly = JOLLY_COLOR)

        //testing filtro solo carte <7
        val noFiguresDeck = allCards.filter { it.value in 1..7 }.shuffled()

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

        val position = state.grid.indexOfFirst { card -> card.name == selectedCard.name }

        if (position % 7 == 0 || position % 7 == 6) return state

        val revealedCards = state.revealedCards - cardId

        val relevantCards = listOf(Triple(cardId,position,true))
        val outcome = listOf(Triple(cardId,position,false))

        sendScreenMessage("covered",relevantCards,outcome)

        return state.copy(
            grid = revealOnGrid(revealedCards, state),
            revealedCards = revealedCards,
            incorrectSevenReveled = true
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

        var modifiedState = state

        when {
            cardId.contains("8") -> { //circular swap
                modifiedState = modifiedState.copy(
                    jackSwaps = generateJackChain(modifiedState,card.house,card.value-1),
                    phase = GamePhase.JackMadness
                )
                //modifiedState = jackSwap(cardId, modifiedState);
            }

            cardId.contains("9") -> //swipe column
                modifiedState = modifiedState.copy(
                    phase = GamePhase.QueenPending
                )

            cardId.contains("10") -> //swipe row
                modifiedState = modifiedState.copy(
                    phase = GamePhase.KingPending
                )
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

    fun jackSwap(cardId: String, state: GameUIState): Boolean {
        val swapCount = state.uncoverDeck.first().value - 1
        Log.d("jackSwap", "numero di swap: $swapCount")

        val swaps =
            _uiState.value.jackSwaps.fold(cardId to state) { (currentId, currentState), nextPosition ->
            jackSwapStep(currentId, currentState, nextPosition)
        }
        val state = swaps.second.copy(phase = GamePhase.Validation)

        _uiState.update { validateState(cardId =swaps.first ,state=state) }

        return true
    }

    fun generateJackChain(state: GameUIState,jackHouse:String,swapCount:Int):List<Int>{
        val validPositions = (0..27).filter { colorHouse(state.grid[it].house) == colorHouse(jackHouse) }

        val positions = mutableListOf<Int>()
        repeat(swapCount) {
            val last = positions.lastOrNull()
            positions.add(validPositions.filter { it != last }.random())
        }
        return positions
    }

    fun jackSwapStep(currentId: String, currentState: GameUIState, nextPosition: Int): Pair<String, GameUIState> {
        val nextCard = currentState.grid[nextPosition]

        val currentPos = currentState.grid.indexOfFirst { it.name == currentId }

        Log.d("jackSwap", "from: $currentPos card: $currentId, to: $nextPosition, nextCard: ${nextCard.name}")

        return nextCard.name to figureSwap(currentId, nextCard.name, currentState, "Jack' chain")
    }

    fun replaceCard(state: GameUIState, cardID: String): GameUIState {
        val card = findCard(cardID, state) ?: return state
        if (!card.isRevealed) return state
        val newUncover = state.uncoverDeck
        val nextCard = state.uncoverDeck.first()
        val pos = _uiState.value.grid.indexOfFirst { it.name==cardID }
        val relevantCards = listOf(Triple(cardID,pos, true))
        val outcome = listOf(Triple(nextCard.name,pos, true))

        sendScreenMessage("addedFromUncover",relevantCards,outcome)

        return state.copy(
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

    }

    fun actionCounter(state: GameUIState, rows: List<List<CardUIStates>>): GameUIState {
        val p1Actions = calcActions(listOf(rows[2], rows[3]))
        val p2Actions = calcActions(listOf(rows[0], rows[1])) + if(state.p2FirstTurn) 1 else 0

        //se p1 ha rivelato un 7 passa
        if (state.p1Turn && state.incorrectSevenReveled) {
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p1ActionsUsed = 0,
                p1Turn = false,
                incorrectSevenReveled = false
            )
        }

        //se p2 ha rivelato un 7 passa
        if (!state.p1Turn && state.incorrectSevenReveled) {
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p2ActionsUsed = 0,
                p1Turn = true,
                incorrectSevenReveled = false
            )
        }

        //fine turno p1
        if (p1Actions - state.p1ActionsUsed <= 0 && state.p1Turn) {
            sendScreenMessage(
                "Turn End",
                listOf(Triple("P1 aveva",state.p1Actions, true)),
                listOf(Triple("P1 ha usato",state.p1ActionsUsed,true))
            )
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p1ActionsUsed = 0,
                p1Turn = false
            )
        }

        //fine turno p2
        if (p2Actions - state.p2ActionsUsed <= 0) {
            sendScreenMessage(
                "Turn End",
                listOf(Triple("P2 aveva",state.p2Actions, true)),
                listOf(Triple("P2 ha usato",state.p2ActionsUsed,true))
            )
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions  - if (state.p2FirstTurn) 1 else 0 ,
                p2ActionsUsed = 0,
                p2FirstTurn = false,
                p1Turn = true
            )
        }

        //continuo il turno
        return state.copy(
            p1Actions = p1Actions,
            p2Actions = p2Actions,
        )
    }

    fun findCard(cardID: String, state: GameUIState): CardUIStates? {
        return state.grid.find { it.name == cardID }
    }

    fun calcActions(rows: List<List<CardUIStates>>): Int {
        return 1 + rows.sumOf { row ->
            val revealed = row.filter { it.isRevealed }
            if (revealed.size < 2) return@sumOf 0
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

    fun blankGrid(){
        _uiState.update { it.copy(grid = emptyList(), uncoverDeck = emptyList()) }
    }

    fun updateGrid(grid:List<CardUIStates>,uncoverDeck:List<CardUIStates>){
        _uiState.update { it.copy(grid = grid, uncoverDeck = uncoverDeck) }
    }

    // — astratti: ogni figlio decide come gestirli —
    abstract fun setup()
    open fun processIntent(intent: GameIntent): Boolean {
        Log.d("MVI", "Intent: $intent | Phase: ${_uiState.value.phase}")
        val c1 = intent is GameIntent.CardClicked
        val c2 = intent is GameIntent.SwapCards
        val c3 = intent is GameIntent.QueenDirectionChosen
        val c4 = intent is GameIntent.KingDirectionChosen

        Log.d("MVI","c1:$c1,c2:$c2,c3:$c3,c4:$c4")

        return when (intent) {
            is GameIntent.CardClicked -> handleCardClicked(intent.cardId)
            is GameIntent.SwapCards -> handleSwapCards(intent.id1, intent.id2)
            is GameIntent.QueenDirectionChosen -> handleQueenDirection(intent.direction.toFunction(intent.colIndex))
            is GameIntent.KingDirectionChosen -> handleKingDirection(intent.rowIndex, intent.direction.toFunction(intent.rowIndex))
            is GameIntent.JackMadness -> {
                val card = _uiState.value.grid[_uiState.value.jackSwaps.first()]
                jackSwap(cardId = card.name,state=_uiState.value)
                return true
            }
            else -> false
        }
    }

    abstract fun matchEnd()

}