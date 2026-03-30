package it.di.unipi.sam636694.semelion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.ui.theme.CardUIStates
import it.di.unipi.sam636694.semelion.ui.theme.GameUIState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Locale.getDefault
import kotlin.collections.chunked
import kotlin.math.max

class SemelionGameViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()
    // ✨ 1: Define the queue that holds String IDs
    private val validationQueue = Channel<String>(Channel.BUFFERED)

    init {
        val decks = createCards()
        _uiState.value = GameUIState(
            grid = decks.first,
            uncoverDeck = decks.second
        )

        viewModelScope.launch {
            for (cardId in validationQueue){
                delay(300)
                validateState(cardId)
            }
        }
    }

    fun createCards(): Pair<List<CardUIStates>,List<CardUIStates>> {
        var allCards = mutableListOf<CardUIStates>()
        val uncoverDeck = mutableListOf<CardUIStates>()

        for (i in 1..4) {
            val currentHouse = mapHouse(i)
            for (j in 1..7) {
                allCards.add(
                    CardUIStates(
                        name = "$j$currentHouse",
                        value = j,
                        house = currentHouse,
                        isRevealed = false
                    )
                )
            }
        }

        allCards = allCards.shuffled() as MutableList<CardUIStates>

        //rimuovo 10 carte che non siano figure
//        for (card in allCards){
//            if (uncoverDeck.size == 10){
//                break
//            }
//            if (card.value < 8){
//                uncoverDeck.add(card)
//            }
//        }

        var house = "red"
        //aggiungo jolly
        allCards.add(
            CardUIStates(
                name = "joker_$house",
                value = 0,
                house = house,
                isRevealed = false
            )
        )

        house = "black"

        allCards.add(
            CardUIStates(
                name = "joker_$house",
                value = 0,
                house = house,
                isRevealed = false
            )
        )
        val gridDeck = allCards - uncoverDeck.toSet()

        return Pair(gridDeck.shuffled(),uncoverDeck.shuffled())

    }

    fun revealOnGrid(revealedCards: List<String>, state: GameUIState): List<CardUIStates> {
        return state.grid.map { card ->
            card.copy(
                isRevealed = card.name in revealedCards,
            )
        }
    }

    fun coverCard(cardId: String, state: GameUIState): GameUIState{
        val selectedCard = findCard(cardId) ?: return state

        if (selectedCard.value != 7) return state

        val position = state.grid.indexOfFirst { card -> card.name == selectedCard.name }

        if (position % 7 == 0 || position % 7 == 6) return state

        val revealedCards = state.revealedCards - cardId
        return state.copy(
            grid = revealOnGrid(revealedCards, state),
            revealedCards = revealedCards
        )
    }

    fun swapCards(id1: String, id2: String){
        val card1 = findCard(id1) ?: return
        val card2 = findCard(id2) ?: return
        val (p1Actions, p2Actions) = increaseUsedActions(_uiState.value)
        _uiState.update { state ->
            state.copy(
                grid =  state.grid.map{ card ->
                    when(card.name){
                        id1 -> card2.copy()
                        id2 -> card1.copy()
                        else -> card.copy()
                    }
                },
                p1ActionsUsed = p1Actions,
                p2ActionsUsed = p2Actions
            )
        }

        validationQueue.trySend(card1.name)
        validationQueue.trySend(card2.name)
    }

    fun increaseUsedActions(state: GameUIState): Pair<Int, Int>{
        return if (state.p1Turn){
            Pair(state.p1ActionsUsed + 1, state.p2ActionsUsed)
        } else {
            Pair(state.p1ActionsUsed, state.p2ActionsUsed + 1)
        }
    }

    fun cardClicked(cardId: String){
         _uiState.update { state ->
            val revealedCards = state.revealedCards + cardId
            val (p1Actions, p2Actions) = increaseUsedActions(state)
            state.copy(
                grid = revealOnGrid(revealedCards, state),
                revealedCards = revealedCards,
                p1ActionsUsed = p1Actions,
                p2ActionsUsed = p2Actions
            )
        }
        validationQueue.trySend(cardId)
    }

    fun validateState(cardId: String){
        //Log.d("Figure",cardId)
        //figureRevealed(cardId)
        _uiState.update { currentState ->
            val rows = currentState.grid.chunked(7)
            var modifiedState = currentState

            rows.forEach { row ->
                row.forEach { card ->
                    modifiedState = coverCard(card.name, modifiedState)
                }
            }

            //controlla se la carta rivelata è una figura
            figureRevealed(cardId)

            //cercare jolly sulla griglia
            //modifiedState = jollyApplier(modifiedState,"joker_black")

            //cercare jolly sulla griglia
            modifiedState = jollyApplier(modifiedState,"joker_red")

            Log.d("validate","${modifiedState.grid.filter { it.name == "joker_black" }}, \n${modifiedState.grid.filter { it.name == "joker_red" }}")

            //aggiorna azioni
            modifiedState = actionCounter(modifiedState,rows)



            return@update modifiedState
        }

    }


    fun jollyApplier(state: GameUIState, cardId: String): GameUIState{
        //cerco il jolly
        val jolly = findCard(cardId)?.copy() ?: return state

        //posizione in griglia
        val pos = state.grid.indexOfFirst { it == jolly }
        //il diviso funge da modulo perchè sto usando gli interi
        val row = pos/7
        val orders = state.grid.chunked(7)[row].getPredominantOrder()

        //senza oridnamento predominante il jolly non assume valore
        if (orders.isEmpty() ) return state

        Log.d("applier","orders: $orders")
        //se ho due ordinamenti con stesso colore non ha senso dare valore al jolly
//        if (colorHouse(orders.first().first) == colorHouse(orders.last().first) && orders.size==2){
//            return state.copy(
//                grid = state.grid.map{ card ->
//                    when(card.name){
//                        cardId -> CardUIStates(
//                            name = jolly.name,
//                            value = 0,
//                            house = colorHouse(jolly.house).lowercase(),
//                            isRevealed = jolly.isRevealed
//                        )
//                        else -> card.copy()
//                    }
//                }
//            )
//        }

        //trovo il colore della riga
        var dominantHouse = colorHouse(orders.first().first)
        Log.d("applier",dominantHouse)
        //per adesso lo ignoro -> tripla d'esempio (F,2,0) (C,0,2) (D,0,2)
        // jolly nero domina f
        // jolly rosso ignoro
        //if (orders.size == 3) return state
        var i = 0
        //valutare significato
        orders.forEach {
            val house = colorHouse(it.first)
            listOf("RED","BLACK").forEach{ color ->
                Log.d("applier","forEach colour, $house:$color")
                if (cardId.contains(color.lowercase(getDefault())) && house == color) {
                    dominantHouse = house
                    i += 1
                    return@forEach
                }
            }
        }
        Log.d("applier","$i")
        //ho due case del colore del jolly -> posso ignorarlo
        //non ho una casa dominante del colore del jolly
        if (i != 1) {
            Log.d("applier","doppia uscita, i:$i")
            return state.copy(
                grid = state.grid.map{ card ->
                    when(card.name){
                        cardId -> CardUIStates(
                            name = jolly.name,
                            value = 0,
                            house = colorHouse(jolly.house).lowercase(),
                            isRevealed = jolly.isRevealed
                        )
                        else -> card.copy()
                    }
                }
            )
        }

        Log.d("applier",dominantHouse)
        //viene fatto solo per orders.size = 2 ed è corretto
        val jollyOrder = orders.filter { colorHouse(it.first) == dominantHouse}
        Log.d("applier","jollyorder:${jollyOrder.first().first}")
//        //orders ha size 2
//        if (orders.size == 2){
//            val h1 = colorHouse(orders.first().first)
//            val h2 = colorHouse(orders.last().first)
//
//            //scelgo il colore in base a quello del jolly
//            if (cardId.contains("red") && h1 == "red"){
//                dominantHouse = h1
//            }
//
//            else if( cardId.contains("red") && h2 == "red"){
//                dominantHouse = h2
//            }
//
//            if (cardId.contains("black") && h1 == "black"){
//                dominantHouse = h1
//            }
//            else if( cardId.contains("red") && h2 == "black"){
//                dominantHouse = h2
//            }
//
//        }

//        //controllo che il jolly sia dello stesso colore della riga
//        if (dominantHouse == "red" && !cardId.contains("red"))return state
//        if (dominantHouse == "black" && !cardId.contains("black")) return state

        var value = 0

        //assume il valore in base all'ordinamento predominante
        when (max(jollyOrder.first().second,jollyOrder.first().third)){
            orders.first().second -> value = pos - 7*row +1
            orders.first().third -> value = 7*(row+1) - pos
        }

        return state.copy(
            grid = state.grid.map{ card ->
                when(card.name){
                    cardId -> CardUIStates(
                        name = jolly.name,
                        value = value,
                        house = jollyOrder.first().first,
                        isRevealed = jolly.isRevealed
                    )
                    else -> card.copy()
                }
            }
        )
    }

    fun figureRevealed(cardId: String){
        when{
            cardId.contains("8") -> //add to hand
                Log.d("pippo","coca")
            cardId.contains("9") -> //swipe column
                Log.d("pippo","coca")
            cardId.contains("10") -> //swipe row
                Log.d("pippo","coca")
        }
    }

    fun actionCounter(state: GameUIState,rows: List<List<CardUIStates>>): GameUIState{
        val p1Actions = calcActions(listOf(rows[2], rows[3]))
        val p2Actions = calcActions(listOf(rows[0], rows[1]))

        if (p1Actions - state.p1ActionsUsed <= 0 && state.p1Turn){
            return state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p1ActionsUsed = 0,
                p1Turn = false
            )
        }

        return if (p2Actions - _uiState.value.p2ActionsUsed  <=0){
            state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
                p2ActionsUsed = 0,
                p1Turn = true
            )
        }else{
            state.copy(
                p1Actions = p1Actions,
                p2Actions = p2Actions,
            )
        }
    }
    fun findCard(cardID: String): CardUIStates?{
        return _uiState.value.grid.find { it.name == cardID }
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

    val crescentOrder = houseCards.foldRight(0){ houseCard,acc ->
        val index = cards.indexOfFirst {  card ->
            // Log.d("CRESCENTORDER","card:${card.isRevealed},houseCard:${houseCard.isRevealed}, ${houseCard.isRevealed && card.isRevealed && (houseCard.name == card.name)}")
            card.name == houseCard.name
        }
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
        else -> {
            val order = mutableListOf(maxCrescent)
            if (maxCrescent.first != maxDecrescent.first) order.add(maxDecrescent)
            return order
        }
    }
}