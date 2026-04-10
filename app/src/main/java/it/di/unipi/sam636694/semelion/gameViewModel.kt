package it.di.unipi.sam636694.semelion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.ui.theme.CardUIStates
import it.di.unipi.sam636694.semelion.ui.theme.GamePhase
import it.di.unipi.sam636694.semelion.ui.theme.GameUIState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.chunked
import kotlin.math.max

class SemelionGameViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()
    val validationQueue = Channel<String>(Channel.BUFFERED)

    init {
        val decks = createDecks()
        _uiState.value = GameUIState(
            grid = decks.first,
            uncoverDeck = decks.second,
            phase = GamePhase.PlayerTurn
        )

        viewModelScope.launch {
            for (cardId in validationQueue){
                delay(200)
                validateState(cardId,_uiState.value)
            }
        }
    }

    fun processIntent(intent: GameIntent){
        Log.d("MVI", "Intent: $intent | Phase: ${_uiState.value.phase}")
        when (intent) {
            is GameIntent.CardClicked         -> handleCardClicked(intent.cardId)
            is GameIntent.SwapCards           -> handleSwapCards(intent.id1, intent.id2)
            is GameIntent.QueenDirectionChosen -> handleQueenDirection(intent.direction)
            is GameIntent.KingDirectionChosen  -> handleKingDirection(intent.direction)
        }
    }

    private fun handleCardClicked(cardId: String) {
        if (_uiState.value.phase !is GamePhase.PlayerTurn) return

        val needsDelay = _uiState.value.grid
            .find { it.name == cardId }
            ?.let { it.value >= 7 } ?: false

        _uiState.update { state ->
            val revealedCards = state.revealedCards + cardId
            val (p1Actions, p2Actions) = increaseUsedActions(state)
            state.copy(
                grid          = revealOnGrid(revealedCards, state),
                revealedCards = revealedCards,
                p1ActionsUsed = p1Actions,
                p2ActionsUsed = p2Actions,
                phase = GamePhase.Validation
            )
        }

        viewModelScope.launch {
            if (needsDelay) delay(300)
            _uiState.update { validateState(cardId,it) }
        }

    }

    private fun handleSwapCards(id1: String, id2: String) {
        if (_uiState.value.phase !is GamePhase.PlayerTurn) return

        val card1 = findCard(id1,_uiState.value) ?: return
        val card2 = findCard(id2,_uiState.value) ?: return

        val (p1Actions, p2Actions) = increaseUsedActions(_uiState.value)

        _uiState.update {
            it.copy(
                grid = it.grid.map { card ->
                    when (card.name) {
                        id1  -> card2.copy()
                        id2  -> card1.copy()
                        else -> card.copy()
                    }
                },
                p1ActionsUsed = p1Actions,
                p2ActionsUsed = p2Actions,
                phase = GamePhase.Validation
            )
        }

        viewModelScope.launch {
            val needsDelay = _uiState.value.grid.any { it.name in listOf(id1,id2) && it.value >= 7 }
            if (!needsDelay) delay(DELAY_TIME)
            _uiState.update { validateState(id1,it) }
        }
    }

    private fun handleQueenDirection(direction: (Int, Int) -> Int) {
        if (_uiState.value.phase !is GamePhase.QueenPending) return

        _uiState.update { state ->
            state.copy(
                grid = (0 until 3).fold(state) { acc, i ->
                    val id1 = findCard(acc.grid[direction(i, 0)].name,state)?.name ?: "none"
                    val id2 = findCard(acc.grid[direction(i, 7)].name,state)?.name ?: "none"
                    figureSwap(id1, id2, acc)
                }.grid,
                phase = GamePhase.Validation          // transizione dentro lo stato
            )
        }

        viewModelScope.launch {
            val needsDelay = _uiState.value.grid
                .find { it.name == _uiState.value.lastReplacedCard }
                ?.let { it.value >= 7 } ?: false
            if (needsDelay) delay(DELAY_TIME)
            _uiState.update { validateState(it.lastReplacedCard?: "queen Landing",it)}
        }
    }

    private fun handleKingDirection(direction: (Int, Int) -> Int) {
        if (_uiState.value.phase !is GamePhase.KingPending) return
        _uiState.update { state ->
            state.copy(
                grid = (0 until 6).fold(state) { acc, i ->
                    val id1 = findCard(acc.grid[direction(i, 0)].name,state)?.name ?: "none"
                    val id2 = findCard(acc.grid[direction(i, 1)].name,state)?.name ?: "none"
                    figureSwap(id1, id2, acc)
                }.grid,
                phase = GamePhase.Validation          // transizione dentro lo stato
            )
        }
        viewModelScope.launch {
            val needsDelay = _uiState.value.grid
                .find { it.name == _uiState.value.lastReplacedCard }
                ?.let { it.value >= 7 } ?: false
            if (needsDelay) delay(DELAY_TIME)
            _uiState.update { validateState(it.lastReplacedCard ?: "king's cross",it) }
        }
    }

    fun validateState(cardId: String,state: GameUIState): GameUIState{
        Log.d("Validate",cardId)
        val card = findCard(cardId,state) ?: return state
        var modifiedState = state

        if (card.isRevealed){
            //controlla se la carta rivelata
            // è una figura
            modifiedState = figureRevealed(cardId,modifiedState)

            if (modifiedState.phase !is GamePhase.Validation){
                return modifiedState
            }
        }

        //cercare jolly sulla griglia -> actually vorrei usare uno stato diverso per le invocazioni però non so
        JOLLY_COLOR.forEach {
            modifiedState = jollyApplier(modifiedState,"joker_$it")
            Log.d("jolly","${findCard("joker_$it",modifiedState)?.name}:${findCard("joker_$it",modifiedState)?.value}")
            //controllo se il jolly può essere sostituito
            modifiedState = substituteJolly(modifiedState,"joker_$it")
        }

        //controllo se devo coprire delle carte
        modifiedState = modifiedState.grid.chunked(7).flatten().fold(modifiedState){state, card ->
            coverCard(card.name,state)
        }

        //aggiorna azioni
        modifiedState = actionCounter(modifiedState,modifiedState.grid.chunked(7))

        return if (modifiedState.phase == GamePhase.Validation) {
            modifiedState.copy(
                phase = GamePhase.PlayerTurn
            )
        } else {
            modifiedState  // mantiene QueenPending o KingPending
        }
    }

    //funzioni di utility
    fun createDecks(): Pair<List<CardUIStates>,List<CardUIStates>> {

        val allCards = createCards(figures = SEMELION_FIGURES,jolly = JOLLY_COLOR)

        //testing filtro solo carte <7
        val noFiguresDeck = allCards.filter { it.value in 1..7 }.shuffled()

        val specialDeck = allCards.filter { it.value > 7  || it.value == 0}

        val gridDeck = noFiguresDeck.drop(UNCOVER_DECK_SIZE) + specialDeck

        val uncoverDeck = noFiguresDeck.take(UNCOVER_DECK_SIZE).map { it.copy(isRevealed = true)  }

        return Pair(gridDeck.shuffled(),uncoverDeck.shuffled())

    }

    fun createCards(figures:List<Pair<Int,String>>,jolly:List<String>): List<CardUIStates>{
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
            figures.forEach{
                add(
                    CardUIStates(
                        name= "${it.first}${it.second}",
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

    fun coverCard(cardId: String, state: GameUIState): GameUIState{
        val selectedCard = findCard(cardId,state) ?: return state

        if (selectedCard.value != 7 || !selectedCard.isRevealed) return state

        val position = state.grid.indexOfFirst { card -> card.name == selectedCard.name }

        if (position % 7 == 0 || position % 7 == 6) return state

        val revealedCards = state.revealedCards - cardId

        return state.copy(
            grid = revealOnGrid(revealedCards, state),
            revealedCards = revealedCards,
            incorrectSevenReveled = true
        )
    }

    fun figureSwap(id1:String,id2:String,state: GameUIState): GameUIState{
        val card1 = findCard(id1,state) ?: return state
        val card2 = findCard(id2,state) ?: return state

        return state.copy(
            grid =  state.grid.map{ card ->
                when(card.name){
                    id1 -> card2.copy()
                    id2 -> card1.copy()
                    else -> card.copy()
                }
            },
        )

    }

    fun increaseUsedActions(state: GameUIState): Pair<Int, Int>{
        return if (state.p1Turn){
            Pair(state.p1ActionsUsed + 1, state.p2ActionsUsed)
        } else {
            Pair(state.p1ActionsUsed, state.p2ActionsUsed + 1)
        }
    }


    fun swapJolly(state: GameUIState,suit:String): GameUIState{
        val jolly = findCard(suit,state)?:return state
        Log.d("jolly","jolly: $jolly")
        val correctCard = findCard("${jolly.value}${jolly.house}",state) ?: return state
        Log.d("jolly","carta corretta:${correctCard.name},jolly: $jolly")

        if (!correctCard.isRevealed || !jolly.isRevealed) return state

        return state.copy(
            grid = state.grid.map {
                when(it.name){
                    suit -> correctCard.copy()
                    correctCard.name -> jolly.copy()
                    else -> it.copy()
                }
            },

            revealedCards = if (correctCard.value == 7){
                state.revealedCards + correctCard.name
            }

            else state.revealedCards,
            incorrectSevenReveled = false
        )
    }

    fun substituteJolly(state : GameUIState,suit: String): GameUIState{
        val currentState = swapJolly(state,suit)
        return if (currentState == state) state
        else replaceCard(currentState,suit)
    }

    fun jollyApplier(state: GameUIState, cardId: String): GameUIState{
        //cerco il jolly
        val jolly = findCard(cardId,state) ?: return state
        //ha senso continuare solo se il jolly è rivelato
        if (!jolly.isRevealed) return state

        //posizione in griglia
        val pos = state.grid.indexOfFirst { it == jolly }

        //il diviso funge da modulo perchè sto usando gli interi
        val row = pos/7

        val orders = state.grid.chunked(7)[row].getPredominantOrder()
        if (orders.isEmpty() ) return state

        //trovo il colore della riga
        var dominantHouse = colorHouse(orders.first().first)

        //per adesso lo ignoro -> tripla d'esempio (F,2,0) (C,0,2) (D,0,2) -> ritestare ma credo funzioni
        var i = 0
        orders.forEach {
            val house = colorHouse(it.first)
            JOLLY_COLOR.forEach{ color ->
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
                grid = state.grid.map{ card ->
                    when(card.name){
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
        val jollyOrder = orders.filter { colorHouse(it.first) == dominantHouse}

        val value =
            when (max(jollyOrder.first().second,jollyOrder.first().third)){
                orders.first().second -> pos - 7*row +1
                orders.first().third -> 7*(row+1) - pos
                else -> 0
            }

        //assume il valore in base all'ordinamento predominante


        Log.d("validate","valore calcolato:$value, seme:$dominantHouse")

        return state.copy(
            grid = state.grid.map{ card ->
                when(card.name){
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

     fun figureRevealed(cardId: String, state: GameUIState): GameUIState{
        val card = findCard(cardId,state) ?: return state
        if (card.value < 8) return state

        var modifiedState = state

        when{
            cardId.contains("8") ->{ //circular swap
                modifiedState = jackSwap(cardId,modifiedState);Log.d("Figure","jack")}

            cardId.contains("9") -> //swipe column
                modifiedState = modifiedState.copy(
                    phase = GamePhase.QueenPending
                    //isQueenRevealed = true
                )
            cardId.contains("10") -> //swipe row
                modifiedState = modifiedState.copy(
                    phase = GamePhase.KingPending
                    //isKingRevealed = true
                )
        }

        modifiedState = replaceCard(modifiedState,cardId)
        return modifiedState
    }

     fun jackSwap(cardId: String, state: GameUIState): GameUIState {
        val swapCount = state.uncoverDeck.first().value - 1
        val jackHouse = findCard(cardId,state)?.house ?: return state

        Log.d("jackSwap", "numero di swap: $swapCount")

        return (1..swapCount).fold(cardId to state) { (currentId, currentState), _ ->
            val currentPos = currentState.grid.indexOfFirst { it.name == currentId }
            val nextPosition = (0..27)
                .filter {
                    it !=currentPos && colorHouse(state.grid[it].house) == colorHouse(jackHouse)
                }
                .random()
            val nextCard = currentState.grid[nextPosition]

            Log.d("jackSwap", "from: $currentPos card: $currentId, to: $nextPosition, nextCard: ${nextCard.name}")

            nextCard.name to figureSwap(currentId, nextCard.name, currentState)
        }.second
    }

    fun replaceCard(state: GameUIState,cardID: String): GameUIState{
        val card = findCard(cardID,state) ?: return state
        if (!card.isRevealed) return state
        val newUncover = state.uncoverDeck
        val nextCard = state.uncoverDeck.first()
//        Log.d("replaceJack","${state.uncoverDeck.first()}")

        return state.copy(
            grid = state.grid.map {
                when(it.name){
                    card.name -> nextCard.copy()
                    else -> it.copy()
                }
            },
            uncoverDeck = newUncover - nextCard,
            revealedCards = state.revealedCards + nextCard.name,
            lastReplacedCard = nextCard.name
        )

    }

    fun actionCounter(state: GameUIState,rows: List<List<CardUIStates>>): GameUIState{
        val p1Actions = calcActions(listOf(rows[2], rows[3]))
        val p2Actions = calcActions(listOf(rows[0], rows[1]))

        //se p1 ha rivelato un 7 passa
        if (state.p1Turn && state.incorrectSevenReveled){
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p1ActionsUsed = 0,
                p1Turn = false,
                incorrectSevenReveled = false
            )
        }

        //se p2 ha rivelato un 7 passa
        if (!state.p1Turn && state.incorrectSevenReveled){
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p2ActionsUsed = 0,
                p1Turn = true,
                incorrectSevenReveled = false
            )
        }

        //fine turno p1
        if (p1Actions - state.p1ActionsUsed <= 0 && state.p1Turn){
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p1ActionsUsed = 0,
                p1Turn = false
            )
        }

        //fine turno p2
        if (p2Actions - state.p2ActionsUsed  <=0){
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p2ActionsUsed = 0,
                p1Turn = true
            )
        }

        //continuo il turno
        return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
            )
    }

    fun findCard(cardID: String,state: GameUIState): CardUIStates?{
        return state.grid.find { it.name == cardID }
    }

    fun calcActions(rows: List<List<CardUIStates>>): Int {
        return 1 + rows.sumOf { row ->
            val revealed = row.filter { it.isRevealed }
            if (revealed.size<2) return@sumOf 0
            row.getPredominantOrder().sumOf { triple -> max(triple.second,triple.third)/2 }
        }

        //        var actions = 1
//        rows.forEach { row ->
//            val revealed = row.filter { it.isRevealed }
//            if (revealed.size < 2) return@forEach  // salta righe con meno di 2 carte rivelate
//
//            val dominant = row.getPredominantOrder()
//            if (dominant.isEmpty()) return@forEach  // salta se non c'è ordine predominante
//
//            dominant.forEach { triple ->
//                Log.d("CALCACTIONS","tripla:$triple")
//                actions += max(triple.second,triple.third)/2
//
//            }
//        }
    }

}

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
    val cards = this
    val revealed = filter { it.isRevealed }
    val houses = mutableSetOf<String>()

    revealed.forEach { card -> houses.add(card.house) }

    val triples: MutableList<Triple<String,Int,Int>> = mutableListOf()

    houses.forEach { house -> triples.add(houseRowOrder(house,cards)) }

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