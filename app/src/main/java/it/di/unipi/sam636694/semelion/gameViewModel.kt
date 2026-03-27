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
import kotlin.math.max

class SemelionGameViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()
    // ✨ 1: Define the queue that holds String IDs
    private val validationQueue = Channel<String>(Channel.BUFFERED)

    init {
        _uiState.value = GameUIState(
            grid = createCards().shuffled()
        )

        viewModelScope.launch {
            for (cardId in validationQueue){
                delay(300)
                validateState(cardId)
            }
        }
    }

    fun createCards(revealedCards: List<String> = emptyList()): List<CardUIStates> {
        return buildList {
            for (i in 1..4) {
                val currentHouse = mapHouse(i)
                for (j in 1..7) {
                    add(
                        CardUIStates(
                            name = "$j$currentHouse",
                            value = j,
                            house = currentHouse,
                            isRevealed = "$j$currentHouse" in revealedCards
                        )
                    )
                }
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
        _uiState.update { currentState ->
            val rows = currentState.grid.chunked(7)
            var modifiedState = currentState
            rows.forEach { row ->
                row.forEach { card ->
                    modifiedState = coverCard(card.name, modifiedState)
                }
            }

            val newRows = modifiedState.grid.chunked(7)
            val p1Actions = calcActions(listOf(newRows[2], newRows[3]))
            val p2Actions = calcActions(listOf(newRows[0], newRows[1]))

            if (p1Actions - modifiedState.p1ActionsUsed <= 0 && modifiedState.p1Turn){
                 return@update modifiedState.copy(
                    p1Actions = p1Actions,
                    p2Actions = p2Actions,
                    p1ActionsUsed = 0,
                    p1Turn = false
                )
            }
            if (p2Actions - _uiState.value.p2ActionsUsed  <=0){
                return@update modifiedState.copy(
                    p1Actions = p1Actions,
                    p2Actions = p2Actions,
                    p2ActionsUsed = 0,
                    p1Turn = true
                )
            }else{
                return@update modifiedState.copy(
                    p1Actions = p1Actions,
                    p2Actions = p2Actions,
                )
            }
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
        order.first().second > order.first().third && order.first().second > 1  -> RowOrder.CRESCENT
        order.first().second < order.first().third && order.first().third > 1 -> RowOrder.DECRESCENT
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
        Log.d("CRESCENTORDER","index:$index,houseCard:${houseCard.value} formulaCrescente:${houseCard.value==index+1},formulaDecrescente:${houseCard.value == 7 - index }")
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
fun findMax(triples:List<Triple<String,Int,Int>>,parameter:String):Triple<String,Int,Int>{
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

    val maxCrescent = findMax(triples,"second")

    val maxDecrescent = findMax(triples,"third")

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