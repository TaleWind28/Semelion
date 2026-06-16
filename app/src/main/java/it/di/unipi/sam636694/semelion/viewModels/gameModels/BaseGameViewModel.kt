package it.di.unipi.sam636694.semelion.viewModels.gameModels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import it.di.unipi.sam636694.semelion.utilities.DELAY_TIME
import it.di.unipi.sam636694.semelion.utilities.JOLLY_COLOR
import it.di.unipi.sam636694.semelion.utilities.POSITION_VALUES
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.utilities.SEMELION_FIGURES
import it.di.unipi.sam636694.semelion.utilities.SharedRepository
import it.di.unipi.sam636694.semelion.utilities.UNCOVER_DECK_SIZE
import it.di.unipi.sam636694.semelion.utilities.actionTemplate
import it.di.unipi.sam636694.semelion.utilities.colorHouse
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
import it.di.unipi.sam636694.semelion.utilities.mapHouse
import it.di.unipi.sam636694.semelion.utilities.toFunction
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import it.di.unipi.sam636694.semelion.utilities.Direction
import it.di.unipi.sam636694.semelion.ui.snackbar.SnackBarController
import it.di.unipi.sam636694.semelion.ui.snackbar.SnackBarEvent
import it.di.unipi.sam636694.semelion.utilities.findPowerRow
import it.di.unipi.sam636694.semelion.utilities.getPredominantOrder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Locale.getDefault
import kotlin.collections.chunked
import kotlin.collections.find
import kotlin.collections.plus
import kotlin.math.max

abstract class BaseGameViewModel(
    val matchesDao: MatchesDao,
    val participationsDao: ParticipationsDao,
    val matchStatisticsDao: MatchStatisticsDao,
    val playersStatisticsDao: PlayerStatisticsDao,
    val userDao: UserDao,
    val player: AudioPlayer,
    var userID: String,
    var secondPlayerId: String,
    val app: Application
) : AndroidViewModel(app) {

    var firstPlayerAvatar:Int? = null

    var secondPlayerAvatar: Int?= null

    protected val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()

    //nomi dei giocatori
    var playerName by mutableStateOf("Semelion User")
    var opponentName by mutableStateOf("Sora")

    //flag per proteggere le scritture sul db
    val isDBOperationComplete = MutableStateFlow(true)

    val wantsToGoBack = MutableStateFlow(false)

    private val startTime:Long = System.currentTimeMillis()

    //matchSummary per poter salvare nel database
    protected val _matchSummary = MutableStateFlow(Pair(
        MatchStatistics(matchId=-1, userId= userID,outcome = "still playing...",  figureRevealed = 0, winner = null, date = startTime, wasFirstPLayer = true,totalActions = 0),
        MatchStatistics(matchId=-1,userId= secondPlayerId,outcome = "still playing...",  figureRevealed = 0,winner = null, date=startTime, wasFirstPLayer = false,totalActions = 0)
    ))
    val matchSummary = _matchSummary.asStateFlow()



    //HANDLER PER GESTIRE EVENTI RELATIVI ALLA UI
    protected fun handleCardClicked(cardId: String): Boolean {

        //azione per log
        val pos = _uiState.value.grid.indexOfFirst { it.name == cardId } +1
        val relevantCards = listOf(Triple(cardId ,pos, findCard(cardId,_uiState.value)?.isRevealed ?: false))

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

        //controllo se applicare delay prima della validazione della carta
        val needsDelay = _uiState.value.grid
            .find { it.name == cardId }
            ?.let { it.value >= 7 } ?: false

        viewModelScope.launch {

            if (needsDelay) delay(300)
            _uiState.update { validateState(cardId, it) }

            when{
                //se ho rivelato un 7
                _uiState.value.lastReplacedCard?.contains("7") == true -> {
                   //do un delay per farlo vedere
                    delay(DELAY_TIME)
                    //se entro qui sicuramente lrc è non null, anzi è del tipo 7House
                    val cardId =_uiState.value.lastReplacedCard ?: "none"
                    //valido lo stato coprendo il 7
                    _uiState.update {
                        val modifiedState = validateState(cardId,it.copy(lastReplacedCard = null))
                        actionCounter(modifiedState,modifiedState.grid.chunked(7))
                    }
                }
                //se non ho rivelato un 7 continuo normalmente
                _uiState.value.phase == GamePhase.PlayerTurn -> {
                    _uiState.update { actionCounter(it, it.grid.chunked(7)) }
                    return@launch
                }
                //se non sono in nessuno dei casi precedenti, quindi ho rivelato una figura
                else ->{
                    handleFigureRevealed()
                }
            }
        }
        return true
    }

    open suspend fun handleFigureRevealed() {
        //quando viene rivelata una figura aggiorno l'actionCounter
        _uiState.update { actionCounter(it, it.grid.chunked(7)) }
        //poi in base alla figura comunico all'utente cosa deve fare
        when (_uiState.value.phase) {
            is GamePhase.QueenPending -> {
                SnackBarController.sendEvent(
                    event = SnackBarEvent(
                        message = app.getString(R.string.queenRevealed)
                    )
                )

            }
            is GamePhase.KingPending -> {
                SnackBarController.sendEvent(
                    event = SnackBarEvent(
                        message = app.getString(R.string.kingRevealed)
                    )
                )
            }

            else -> return
        }

    }

    protected fun handleSwapCards(id1: String, id2: String): Boolean {
        //cerco le carte da scambiare sulla griglia
        val card1 = findCard(id1, _uiState.value) ?: return false
        val card2 = findCard(id2, _uiState.value) ?: return false
        //genero un messaggio di errore in caso lo scambio non sia possibile
        val message = canSwap(card1,card2,_uiState.value)

        //se il messaggio è stato generato allora lo scambio non si può effettuare
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

        //azione per log
        val relevantCards = listOf(
            Triple(id1,_uiState.value.grid.indexOfFirst { it.name == id1 }+1, findCard(id1,_uiState.value)?.isRevealed ?: false),
            Triple(id2,_uiState.value.grid.indexOfFirst { it.name == id2 }+1, findCard(id2,_uiState.value)?.isRevealed ?: false)
        )

        //aggiorno lo stato
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

        //azione per log
        val outcome = listOf(
            Triple(id1,_uiState.value.grid.indexOfFirst { it.name == id1 }+1, findCard(id1,_uiState.value)?.isRevealed ?: false),
            Triple(id2,_uiState.value.grid.indexOfFirst { it.name == id2 }+1, findCard(id2,_uiState.value)?.isRevealed ?: false)
        )
        //mando azione al LogViewModel
        sendScreenMessage("swap",relevantCards,outcome)
        //valido lo stato
        viewModelScope.launch {
            //controllo se il delay serve
            val needsDelay = _uiState.value.grid.any { it.name in listOf(id1, id2) && it.value >= 7 }
            if (!needsDelay) delay(DELAY_TIME)
            //valido e aggiorno lo stato
            _uiState.update {
                val state = validateState(id2,validateState(id1, it))
                actionCounter(state,state.grid.chunked(7))
            }
        }

        return true

    }

    protected fun handleJackMadness(swaps:List<Int>):Boolean{
        //se non sto simulando l'azione oppure non ho rivelato il jack la funzione non va eseguita
        //-> questo succede quando per esempio viene rivelato un 7 dal jack e tale 7 non è in posizione corretta
        if (_uiState.value.phase !is GamePhase.JackMadness && _uiState.value.phase !is GamePhase.WaitingForOpponent) return false
        //riproduco il suono del jack
        player.playFile(R.raw.jack)

        //scelgo come primo id quello della carta rivelata in modo da gestire il 7
        val cardId = _uiState.value.grid[swaps.first()].name

        //droppo il primo elemento che è la posizione attuale del jack
        val jackSwaps = swaps.drop(1)
        //se non ho altre pozioni termino
        if (jackSwaps.isEmpty()){
            _uiState.update {
                val modifiedState = validateState(cardId = cardId,_uiState.value.copy(phase = GamePhase.Validation))
                actionCounter(modifiedState,modifiedState.grid.chunked(7))
            }
            return false
        }

        Log.d("jackSwap","passo")

        _uiState.update { state->
            val swappedState =
                jackSwaps.fold(cardId to state) { (currentId, currentState), nextPosition ->
                    val nextCard = currentState.grid[nextPosition]

                    val currentPos = currentState.grid.indexOfFirst { it.name == currentId }

                    Log.d("jackSwap", "from: $currentPos card: $currentId, to: $nextPosition, nextCard: ${nextCard.name}")

                    currentId to figureSwap(currentId, nextCard.name, currentState, "Jack' chain")
                }.second
            val modifiedState = applyAndValidate(swappedState,(0..27).toList())
            actionCounter(modifiedState,modifiedState.grid.chunked(7))
        }

        return true
    }

    protected fun handleQueenDirection(direction: (Int, Int) -> Int): Boolean {
        //se non sto simulando l'azione oppure non ho rivelato il jack la funzione non va eseguita
        //-> questo succede quando per esempio viene rivelato un 7 dalla donna e tale 7 non è in posizione corretta
        if (_uiState.value.phase !is GamePhase.QueenPending && _uiState.value.phase !is GamePhase.WaitingForOpponent) return false
        player.playFile(R.raw.queen)

        _uiState.update { state ->
            //uso una fold per aggiornare consecutivamente lo stato seguendo la direzione scelta dal giocatore
            var modifiedState = state.copy(
                grid = (0 until 3).fold(state) { acc, i ->
                    val id1 = findCard(acc.grid[direction(i, 0)].name, state)?.name ?: "none"
                    val id2 = findCard(acc.grid[direction(i, 7)].name, state)?.name ?: "none"
                    figureSwap(id1, id2, acc,"Queen'Swipe")
                }.grid,
                phase = GamePhase.Validation          // transizione dentro lo stato
            )
            //ricalcolo il colId
            val colId = direction(0,0)
            if (direction(0,7) == Direction.UP.toFunction(colId)(0,7))
                sendScreenMessage("Queen'Swipe",listOf(Triple("Alto",colId,true)),listOf(Triple("",colId,true)))
            else
                sendScreenMessage("Queen'Swipe",listOf(Triple("Basso",colId,false)),listOf(Triple("",colId,false)))
            //dopo aver spostato la colonna valido lo stato
            modifiedState = applyAndValidate(modifiedState,(0 until 3).map { direction(it,0) })
            //ed aggiorno l'actionCounter
            actionCounter(modifiedState,modifiedState.grid.chunked(7))
        }

        return true
    }

    protected fun handleKingDirection(rowIndex:Int, direction: (Int, Int) -> Int) : Boolean {
        //se non sto simulando l'azione oppure non ho rivelato il Re la funzione non va eseguita
        //-> questo succede quando per esempio viene rivelato un 7 dal Re e tale 7 non è in posizione corretta
        if (_uiState.value.phase !is GamePhase.KingPending && _uiState.value.phase !is GamePhase.WaitingForOpponent) return false
        player.playFile(R.raw.king)

        //controllo che il giocatore non abbia selezionato una riga potente
        if (_uiState.value.grid.chunked(7)[rowIndex].findPowerRow() == 1){
            viewModelScope.launch {
                SnackBarController.sendEvent(
                    event = SnackBarEvent(
                        message = app.getString(R.string.kingEffectNotAllowedOnPowerRows)
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
                phase = GamePhase.Validation,
            )

            if (direction(1,0) == Direction.RIGHT.toFunction(rowIndex)(1,0))
                sendScreenMessage("King's Rule",listOf(Triple("Destra",rowIndex,true)),listOf(Triple("",rowIndex,true)))
            else
                sendScreenMessage("King's Rule",listOf(Triple("Sinistra",rowIndex,false)),listOf(Triple("",rowIndex,false)))


            //validazione postuma
            modifiedState = applyAndValidate(modifiedState,(0 until 6).map { (rowIndex * 7) + it })
            actionCounter(modifiedState,modifiedState.grid.chunked(7))
        }

        return true
    }

    //la funzione applica più volte validate tramite una fold
    protected fun applyAndValidate(
        state: GameUIState,
        positions: List<Int>
    ): GameUIState {
        return positions.fold(state.copy(phase = GamePhase.Validation)) { currState, pos ->
            validateState(currState.grid[pos].name,currState)
        }
    }

    //FUNZIONI HELPER PER GLI HANDLER
    fun generateJackChain(state: GameUIState,jackHouse:String,swapCount:Int):List<Int>{
        //posizioni valide per gli scambi del jack
        val validPositions = (0..27).filter { colorHouse(state.grid[it].house) == colorHouse(jackHouse) }
        val positions = mutableListOf<Int>()
        //popolo la lista positions
        repeat(swapCount) {
            val last = positions.lastOrNull()
            positions.add(validPositions.filter { it != last }.random())
        }
        //ritorno le posizoni da scambiare
        return positions
    }

    //applico le regole per scambiare carte -> queste regole non valgono per le figure
    fun canSwap(card1: CardUIStates,card2: CardUIStates,state: GameUIState): String?{
        val row = state.grid.chunked(7)
        val rows = Pair(row[0] + row[1],row[2] + row[3])
        //prendo in considerazione le righe delle carte
        val cardInfos = Pair(
            state.grid.indexOfFirst { it.name== card1.name }.let{it to it/7},
            state.grid.indexOfFirst { it.name== card2.name }.let { it to it/7 },
        )
        //controllo che almeno una delle due carte sia in pozione corretta dopo lo scambio
        fun valueControl(rowId:Int,globalPosition:Int,value:Int): Boolean{
            return when (value) {
                POSITION_VALUES.first(rowId,globalPosition)-> true
                POSITION_VALUES.second(rowId,globalPosition) -> true
                else -> false
            }
        }

        //controllo che lo scambio avvenga con al più una carta dell'avversario
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
        //messaggio di errore da generare
        fun errorMessage(positionValid: Boolean,fairness: Boolean):String?{
            return if (!positionValid && !fairness){
                app.getString(R.string.swapViolatesAll)
            }
            else if (!positionValid){
                app.getString(R.string.swapViolatesPosition)

            }else if(!fairness) {
                app.getString(R.string.swapViolatesFairness)
            }
            else{
                null
            }
        }

        //controllo che le carte non siano in un ariga potente
        if (row[cardInfos.first.second].findPowerRow() == 1 || row[cardInfos.second.second].findPowerRow() == 1) {
            return app.getString(R.string.swapViolatesPowerRow)
        }

        //controllo se posso scambiare la seconda carta secondo la posizione corretta
        val c2SwapValid = !card2.isRevealed || card2.name.contains("joker") ||
                valueControl(rowId= cardInfos.first.second,globalPosition= cardInfos.first.first, value= card2.value)

        //controllo se posso scambiare la prima carta secondo la posizione corretta
        val c1SwapValid = !card1.isRevealed || card1.name.contains("joker") ||
                valueControl(rowId= cardInfos.second.second,globalPosition= cardInfos.second.first,value= card1.value)

        //validità globale dello scambio in base alle posizioni
        val positionValid = when{
            !card1.isRevealed && !card2.isRevealed -> true
            !card1.isRevealed -> c2SwapValid
            !card2.isRevealed -> c1SwapValid
            else -> c1SwapValid || c2SwapValid
        }
        //ritorno il risultato di errorMessage
        return  errorMessage(positionValid,fairnessControl(card1,card2,state.p1Turn))
    }

    //FUNZIONE DI VALIDAZIONE
    open fun validateState(cardId: String, state: GameUIState): GameUIState {
        //carta da validare
        val card = findCard(cardId, state) ?: return state
        //stato da modificare
        var modifiedState = state

        //controllo se la carta è rivelata
        if (card.isRevealed) {
            //controlla se la carta rivelata è una figura
            modifiedState = figureRevealed(cardId, modifiedState)
            if (modifiedState.lastReplacedCard?.contains("7") == true) return modifiedState
        }


        //cercare jolly sulla griglia -> actually vorrei usare uno stato diverso per le invocazioni però non so
        JOLLY_COLOR.forEach {
            modifiedState = jollyApplier(modifiedState, "joker_$it")
            modifiedState = substituteJolly(modifiedState, "joker_$it")
        }

        //controllo se la carta deve essere coperta
        modifiedState = coverCard(cardId,modifiedState)

        //se sono ancora in fase di validazione modifico il numero di azioni
        if (modifiedState.phase is GamePhase.Validation){
                //aggiorna azioni
                val (p1Actions, p2Actions) = increaseUsedActions(modifiedState)
                modifiedState = modifiedState.copy(
                    p1ActionsUsed = p1Actions,
                    p2ActionsUsed = p2Actions
                )
            Log.d("pippo","$cardId, phase:${modifiedState.phase}")
            //update dei summary
            _matchSummary.update { summary ->
                if(state.p1Turn)
                    Pair(summary.first.copy( totalActions = summary.first.totalActions +1),summary.second)
                else
                    Pair(summary.first,summary.second.copy(totalActions = summary.second.totalActions + 1))
            }
        }

        //divido la griglia in parti per assegnarle ai giocatori e fare i controlli adeguati
        val rows = modifiedState.grid.chunked(7)
        val upperHalf = listOf(rows[0], rows[1])
        val bottomHalf = listOf(rows[2], rows[3])

        //controllo se un giocatore ha vinto
        modifiedState = findWinner(upperHalf,bottomHalf,modifiedState)

        //se la validazione ha avuto successo torna al turno del giocatore
        return if (modifiedState.phase == GamePhase.Validation) {
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
        //genero tutte le carte e le mescolo
        val allCards = createCards(figures = SEMELION_FIGURES, jolly = JOLLY_COLOR).shuffled()

        //creo una lista con solo numeri
        val noFiguresDeck = allCards.filter { it.value in 1..7 }
        //creo il mazzo con figure e jolly
        val specialDeck = allCards.filter { it.value > 7 || it.value == 0 }
        //droppo le prime UNCOVER_DECK_SIZE carte dal mazzo senza figure e aggiungo le figure
        val gridDeck = noFiguresDeck.drop(UNCOVER_DECK_SIZE) + specialDeck
        //droppo le prime UNCOVER_DECK_SIZE carte dal mazzo senza figure per creare il mazzo scoperta
        val uncoverDeck = noFiguresDeck.take(UNCOVER_DECK_SIZE).map { it.copy(isRevealed = true) }
        //ritorno i mazzi
        return Pair(gridDeck.shuffled(), uncoverDeck.shuffled())

    }

    //funzione per testing
    fun createTestDecks(): Pair<List<CardUIStates>, List<CardUIStates>>{
        val allCards = createCards(figures = SEMELION_FIGURES, jolly = JOLLY_COLOR).shuffled()

        //testing filtro solo carte <7
        val noFiguresDeck = allCards.filter { it.value in 2..7 }

        val specialDeck = allCards.filter { it.value > 7 || it.value == 0 }

        val gridDeck = noFiguresDeck.drop(UNCOVER_DECK_SIZE - 4) + specialDeck

        //val uncoverDeck = noFiguresDeck.take(UNCOVER_DECK_SIZE).map { it.copy(isRevealed = true) }
        val uncoverDeck = allCards.filter { it.value == 1 }
        return Pair(gridDeck.shuffled(), uncoverDeck.map{it.copy(isRevealed = true)}.shuffled())
    }

    fun createCards(figures: List<Pair<Int, String>>, jolly: List<String>): List<CardUIStates> {
        return buildList {
            for (i in 1..4) {
                //aggiungo una carta per seme per ogni valore
                val currentHouse = mapHouse(i)
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

    //rivelo una carta sulla griglia
    fun revealOnGrid(revealedCards: List<String>, state: GameUIState): List<CardUIStates> {
        return state.grid.map { card ->
            card.copy(
                isRevealed = card.name in revealedCards,
            )
        }
    }

    //controllo se una carta deve essere coperta
    fun coverCard(cardId: String, state: GameUIState): GameUIState {
        //cerco la carta sulla griglia
        val selectedCard = findCard(cardId, state) ?: return state
        //l'unica carta da coprire è il 7, quindi se la carta ha valore diverso oppure è coperta termino
        if (selectedCard.value != 7 || !selectedCard.isRevealed) return state
        //controllo la posizione della carta
        val position = state.grid.indexOfFirst { card -> (card.name == selectedCard.name)}

        //controllo se il 7 può essere rivelato
        if (position % 7 == 0 || position % 7 == 6) return state
        //il 7 deve essere coperto
        val revealedCards = state.revealedCards - cardId
        //azioni per log
        val relevantCards = listOf(Triple(cardId,position,true))
        val outcome = listOf(Triple(cardId,position,false))
        sendScreenMessage("covered",relevantCards,outcome)

        //riproduco suono per far capire che il 7 è stato coperto
        player.playFile(R.raw.seven)

        //ritorno una copia dello stato con la carta coperta
        return state.copy(
            grid = revealOnGrid(revealedCards, state),
            revealedCards = revealedCards,
            incorrectSevenReveled = true,
        )
    }

    //swap usato dalle figure per evitare le regole di scambio
    fun figureSwap(id1: String, id2: String, state: GameUIState, type: String): GameUIState {
        val card1 = findCard(id1, state) ?: return state
        val card2 = findCard(id2, state) ?: return state

        val relevantCards = listOf(
            Triple(id1,state.grid.indexOfFirst { it.name == id1 }+1, findCard(id1,state)?.isRevealed ?: false),
            Triple(id2,state.grid.indexOfFirst { it.name == id2 }+1, findCard(id2,state)?.isRevealed ?: false)
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
            Triple(id1,modifiedState.grid.indexOfFirst { it.name == id1 }+1 , findCard(id1,modifiedState)?.isRevealed ?: false),
            Triple(id2,modifiedState.grid.indexOfFirst { it.name == id2 }+1, findCard(id2,modifiedState)?.isRevealed ?: false)
        )

        if (type=="Jack' chain") sendScreenMessage(type,relevantCards,outcome)

        return modifiedState
    }

    //aumento il numero di azioni usate
    fun increaseUsedActions(state: GameUIState): Pair<Int, Int> {
        return if (state.p1Turn) {
            Pair(state.p1ActionsUsed + 1, state.p2ActionsUsed)
        } else {
            Pair(state.p1ActionsUsed, state.p2ActionsUsed + 1)
        }
    }

    //scambio il jolly con la carta che sta sostituendo
    fun swapJolly(state: GameUIState, suit: String): GameUIState {
        val jolly = findCard(suit, state) ?: return state
        val correctCard = findCard("${jolly.value}${jolly.house}", state) ?: return state
        //controllo se la carta è rivelata e se lo è anche il jolly
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

    //controllo se il jolly deve essere sostituito e ritorno lo stato risultante
    fun substituteJolly(state: GameUIState, suit: String): GameUIState {
        val position = state.grid.indexOfFirst { it.name == suit }
        var currentState = swapJolly(state, suit)
        return if (currentState == state) state
        else{
            currentState = replaceCard(currentState, suit)
            validateState(currentState.grid[position].name,currentState)
        }
    }

    //funzione per far assumere al jolly il valore relativo alla sua posizione
    fun jollyApplier(state: GameUIState, cardId: String): GameUIState {
        //cerco il jolly
        val jolly = findCard(cardId, state) ?: return state
        //ha senso continuare solo se il jolly è rivelato
        if (!jolly.isRevealed) return state

        //posizione in griglia
        val pos = state.grid.indexOfFirst { it == jolly }

        //il diviso funge da modulo perchè sto usando gli interi
        val row = pos / 7
        //seguo l'ordinamento predominante
        val orders = state.grid.chunked(7)[row].getPredominantOrder()
        //se non è presente il jolly non fa niente
        if (orders.isEmpty()) return state

        //trovo il colore della riga
        var dominantHouse = colorHouse(orders.first().first)

        //
        // per adesso lo ignoro ->
        // tripla d'esempio (F,2,0) (C,0,2) (D,0,2)
        // -> ritestare ma credo funzioni
        // -> questa tripla dovrebbe dare 1 azione o forse 2 non mi ricordo alla fine che regola abbiamo scelto ma so che è corretto
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

    //applico uno stato diverso in base alla figura rivelata
    fun figureRevealed(cardId: String, state: GameUIState): GameUIState {
        val card = findCard(cardId, state) ?: return state
        //controllo che sia effettivamente una figura
        if (card.value < 8) return state
        //trovo la posizione
        val position = _uiState.value.grid.indexOfFirst { it.name == card.name }

        var modifiedState = state

        //applico la fase corretta in base alla figura
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

        //aggiorno i matchSummary
        _matchSummary.update { summary ->
            if(state.p1Turn)
                Pair(summary.first.copy(figureRevealed= summary.first.figureRevealed + 1),summary.second)
            else
                Pair(summary.first,summary.second.copy(figureRevealed= summary.second.figureRevealed + 1))
        }

        //aggiorno lo stato aggiungendo la carta dal mazzo scoperta e togliendo la figura
        modifiedState = replaceCard(modifiedState, cardId)

        //ritorno lo stato modificato
        return modifiedState
    }

    //rimpiazzo una carta sulla griglia
    fun replaceCard(state: GameUIState, cardID: String): GameUIState {
        //trovo la carta da sostituire
        val card = findCard(cardID, state) ?: return state
        //sostituisco solo se è rivelata
        if (!card.isRevealed) return state
        //memorizzo il mazzo scoperta
        val newUncover = state.uncoverDeck
        //memorizzo la carta da aggiungere alla griglia
        val nextCard = state.uncoverDeck.first()
        //cerco la posizione della carta da sostituire sulla griglia
        val pos = state.grid.indexOfFirst { it.name==cardID }

        //azioni per log
        val relevantCards = listOf(Triple(cardID,pos+1, true))
        val outcome = listOf(Triple(nextCard.name,pos+1, true))
        sendScreenMessage("addedFromUncover",relevantCards,outcome)

        //aggiorno lo stato
        val currState = state.copy(
            grid = state.grid.map {
                when (it.name) {
                    card.name -> nextCard.copy()
                    else -> it.copy()
                }
            },
            uncoverDeck = newUncover - nextCard,
            revealedCards = state.revealedCards + nextCard.name,
            lastReplacedCard = nextCard.name,
        )
        //se la carta aggiunta è un 7 allora ritorno senza validare per lasciare che sia handleCardRevealed a gestirlo
        return if (nextCard.value == 7) currState
        else validateState(nextCard.name,currState) //altrimenti lo valido lo stato
    }

    //controllo se il giocatore di turno ha usato tutte le azioni
    open fun actionCounter(state: GameUIState, rows: List<List<CardUIStates>>): GameUIState {
        //azioni massime dei giocatori
        val p1Actions = calcActions(listOf(rows[2], rows[3]))
        val p2Actions = calcActions(listOf(rows[0], rows[1]))

        //mi sembrava più elegante farlo così
        val isSecondPlayer = {player:Boolean, turn:Int-> (if((!player && turn == 0) || (player && turn == 1)) 1 else 0)}

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
                    phase = GamePhase.PlayerTurn
                )
            //se il giocatore ha terminato le azioni il turno termina
            //fine turno p1
            p1Actions + isSecondPlayer(state.p1Turn,state.turnsPlayed) - state.p1ActionsUsed  <=0 ->
                state.copy(
                    p1Actions = p1Actions,
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
                    p2Actions = p2Actions,
                    turnsPlayed = state.turnsPlayed+1
                )

            else ->
                //se non ho finito le azioni e non ho rivelato un 7 allora posso far continuare il turno
                state.copy(
                    p1Actions= p1Actions+ isSecondPlayer(state.p1Turn,state.turnsPlayed),
                    p2Actions= p2Actions + isSecondPlayer(!state.p1Turn,state.turnsPlayed)
                )
        }
    }

    //helper per trovare le carte sulla griglia
    fun findCard(cardID: String, state: GameUIState): CardUIStates? {
        return state.grid.find { it.name == cardID }
    }

    //calcolo delle azioni disponibili
    fun calcActions(rows: List<List<CardUIStates>>): Int {
        //numero di azioni pari a 1 + carte in posizione corretta / 2
        return 1 + rows.sumOf { row ->
            val revealed = row.filter { it.isRevealed }
            if (revealed.size < 2) return@sumOf 0
            //regola alternativa -> abbiamo visto che porta al bullismo senza avvantaggiare troppo il bullo
            //val houseActions = row.getBonusActions()
            //houseActions.sumOf { triple ->  triple.second / 2 + triple.third / 2}
            row.getPredominantOrder().sumOf { triple -> max(triple.second, triple.third) / 2 }
        }
    }

    //controllo se la partita deve finire
    open fun findWinner(upperHalf:List<List<CardUIStates>>, bottomHalf:List<List<CardUIStates>>, state: GameUIState): GameUIState {
        //righe potenti dei giocatori
        val predRows = Pair(
            first = upperHalf.fold(0) { acc:Int, row ->
                acc + row.findPowerRow()
            },
            second = bottomHalf.fold(0){ acc, row ->
                acc + row.findPowerRow()
            }
        )
        //quando un giocatore ha 2 righe potenti allora quel giocatore ha vinto
        return when{
            (predRows.first == 2 && predRows.second == 2) -> state.copy( winner = "Pareggio", phase = GamePhase.GameOver)
            predRows.first == 2 -> state.copy( winner = secondPlayerId, phase = GamePhase.GameOver)
            predRows.second == 2 -> state.copy( winner = userID, phase = GamePhase.GameOver)
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
    //Paradigma MVI per gestire le azioni dell'utente
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

    //trovare il vincitore e comunicarlo per il db
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

    //procedura di chiusura del match
    open fun matchEnd(mode: GameModes,loser:String? = null,resumedMatchId:Long? = null){
        //messaggio di flush del logViewModel per averlo pulito alla prossima partita
        sendScreenMessage("matchEnded",emptyList(),emptyList())
        //calcolo l'outcome della partita
        val (outcome,winningUser) = calculateOutcome(loser,_uiState.value)

        _uiState.update { it.copy(winner = outcome) }

        //flag per proteggere le operazioni del db
        isDBOperationComplete.value = false

        _matchSummary.update {
            it.copy(
                first = it.first.copy(outcome=outcome, winner = winningUser),
                second = it.second.copy(outcome=outcome,winner = if (winningUser != null) !winningUser else null)
            )
        }

        //db operations
        viewModelScope.launch {
            //aggiorno la tabella Partite
            updateMatch(resumedMatchId,mode,outcome,winningUser)

            //update dei playerSummary
            listOf(userID,secondPlayerId).forEach{ userId ->
                when{
                    winningUser == null -> updatePlayerStats(userId,winningUser)
                    userId == userID -> updatePlayerStats(userId,winningUser)
                    userId != userID -> updatePlayerStats(userId,!winningUser)
                }
            }
            isDBOperationComplete.value = true
        }
    }

    //aggiorno la tabella Partite del db
    suspend fun updateMatch(resumedMatchId: Long?,mode: GameModes,outcome:String,winningUser: Boolean?){
        val matchId = resumedMatchId ?: (matchesDao.getNextMatchId() - 1)
        //inserisco il match nel db
        matchesDao.update(Matches(matchId = matchId,gameMode= mode,gameState=_uiState.value, isCompleted = true))
        Log.d("DBMS","$matchId")

        //update dei matchSummary
        val p1Stats = matchSummary.value.first.copy(matchId = matchId,outcome = outcome, winner = winningUser)
        val p2Stats = matchSummary.value.second.copy(matchId = matchId,outcome = outcome, winner = winningUser?.not())

        matchStatisticsDao.upsert(p1Stats)
        matchStatisticsDao.upsert(p2Stats)
    }

    //aggiorno le statistiche dei giocatori
    suspend fun updatePlayerStats(userId:String,winningUser: Boolean?){
        //se non ha delle statistiche le creo
        //winninguser sarà true se il vincitore è l'utente locale, false se il vincitore è l'avversario, null altrimenti
        var stats = PlayerStatistics(
            userId= userId,
            matchesPlayed = 1,
            matchesWon = if (winningUser == true) 1 else 0,
            matchesLost=if (winningUser == false) 1 else 0,
            matchesDrawn = if (winningUser == null) 1 else 0
        )

        //ottengo le statistiche del player dal db
        val playerStats: PlayerStatistics? = playersStatisticsDao.getStatsByUser(userId)

        //se non ho la riga allora la creo con le stats ottenute attualmente
        if (playerStats == null ){
            playersStatisticsDao.insert(stats.copy(currentStreak = if (stats.matchesWon == 1) 1 else 0, bestStreak = if (stats.matchesWon == 1) 1 else 0))
            return
        }


        //controllo la streak
        if (winningUser == true){
            //se ho vinto incremento la streak
            val currStr = playerStats.currentStreak + 1
            //controllo se ho superato la migliore
            if (currStr > playerStats.bestStreak){
                //aggiorno la streak migliore oltre a quella attuale
                stats = stats.copy(currentStreak = currStr, bestStreak = currStr)
            }
        }
        else{
            //annullo la streak se ho perso
            stats = stats.copy(currentStreak = 0, bestStreak = playerStats.bestStreak)
        }

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

    //interrompo un mathc
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
            matchSummary.value.toList().forEach { stats ->
                val stat = stats.copy(matchId = matchId,outcome = "interrupted", winner = null)
                Log.d("DB","inserting data: $stat")
                matchStatisticsDao.upsert(stat)
            }

            isDBOperationComplete.value = true
        }
    }
    //inizio una partita
    suspend fun matchStart(mode: GameModes, nickname:String? = null){


        //devo metterlo da un'altra parte -> no alla fine qui sta bene
        updateUsers(nickname=nickname)

        if (nickname!=null){
            this.opponentName = nickname
        }
        val matchID = matchesDao.getNextMatchId()
        //inserisco il match nel db
        matchesDao.insert(Matches(gameMode = mode, gameState = _uiState.value, isCompleted = false))
        //inserisco le partecipazioni nel db
        participationsDao.insert(Participations(matchId= matchID, userId = secondPlayerId, role = "Guest"))
        participationsDao.insert(Participations(matchId= matchID,userId = userID, role = "Host"))
        //flag per consentire modifiche in sicurezza nella UI
        isDBOperationComplete.value = true
    }

    //decido il primo giocatore
    open fun setFirstPlayer() {

        val coinFlip = SecureRandom().nextBoolean()

        if(coinFlip)
            this._uiState.update { it.copy(firstPlayer = "Host", p2Actions = it.p2Actions+1)}
        else
            this._uiState.update { it.copy(firstPlayer = "Guest", p1Turn = false, p1Actions=it.p1Actions + 1)}
    }

    //aggiorno la tabella utenti
    suspend fun updateUsers(nickname:String?){
        //controllo se devo creare l'utente nel db
        var localUser = userDao.getUserById(userID)

        if (localUser == null) userDao.insert(User(userID, nickName = "Semelion_User: $userID", avatar = R.drawable.avatar_1))

        //per essere sicuro di avere un utente
        localUser = userDao.getUserById(userID)

        this.playerName = localUser?.nickName ?: "Semelion User"

        //imposto l'avatar del primo player
        if (firstPlayerAvatar!=null) userDao.update(User(userID,playerName,firstPlayerAvatar!!))

        //controllo se esiste l'avversario nel db
        val opponent = userDao.getUserById(secondPlayerId)

        //controllo se in caso l'avversario esista il nickname sia diverso da quello in memoria, solo se il nickname non è null
        if (opponent == null) userDao.insert(User(secondPlayerId, nickName = nickname ?: "Sora", avatar = R.drawable.avatar_1))
        else  userDao.update(User(userId=secondPlayerId,nickName=nickname?:opponent.nickName,avatar=secondPlayerAvatar?:opponent.avatar))
    }

    //aggiorno il secondo giocatore
    protected fun updateSecondPlayer(secondPlayerId: String){
        this.secondPlayerId = secondPlayerId
        _matchSummary.update { Pair(it.first,it.second.copy(userId=secondPlayerId)) }
    }

    //riproduco il suono in base all'outcome della partita
    fun playEndSound() {
        val winner = _uiState.value.winner?.substringAfterLast(" ")
        val sound = when (winner) {
            userID -> R.raw.victory_fanfare
            secondPlayerId -> R.raw.gameover
            else -> R.raw.there
        }
        player.playFile(sound)
    }

    //metodo per distruggere il vm da implementare
    abstract fun destroy()
}