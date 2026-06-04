package it.di.unipi.sam636694.semelion.utilities

import android.util.Log
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent

data class ActionData(val type: String, val relevantCards: List<CardInfo>, val outcome: List<CardInfo>)

enum class RowOrder { CRESCENT, DECRESCENT, BOTH }

fun String.toActionData(): ActionData {
    val mainRegex = Regex("""^([^:]+):\{(.*)\} -> \{(.*)\}\s*$""")
    val match = mainRegex.find(this) ?: throw IllegalArgumentException("Formato non valido: $this")

    val type = match.groupValues[1]
    val relevantCardsRaw = match.groupValues[2]
    val outcomeRaw = match.groupValues[3]

    return ActionData(
        type = type,
        relevantCards = parseCards(relevantCardsRaw),
        outcome = parseCards(outcomeRaw)
    )
}

fun parseCards(raw: String): List<CardInfo> {
    if (raw.isBlank()) return emptyList()
    val tripleRegex = Regex("""\(([^,]+),\s*(\d+),\s*(true|false)\)""")
    return tripleRegex.findAll(raw).map {
        CardInfo(
            name = it.groupValues[1].trim(),
            value = it.groupValues[2].toInt(),
            flag = it.groupValues[3].toBoolean()
        )
    }.toList()
}

fun GameIntent.serialize(): String = when (this) {
    is GameIntent.CardClicked -> "cardclicked:$cardId"
    is GameIntent.SwapCards -> "swapcards:$id1:$id2"
    is GameIntent.QueenDirectionChosen -> "queendirection:$colIndex:${direction.name}"
    is GameIntent.KingDirectionChosen -> "kingdirection:$rowIndex:${direction.name}"
    is GameIntent.JackMadness -> "jackmadness:$jackSwaps"
    is GameIntent.Errore -> "errore:$id"
    else -> "Undefined Behavior"
}

fun String.toGameIntent(): GameIntent {
    val parts = this.split(":")
    return when (parts[0]) {
        "cardclicked" -> GameIntent.CardClicked(parts[1])
        "swapcards" -> GameIntent.SwapCards(parts[1], parts[2])
        "queendirection" -> GameIntent.QueenDirectionChosen(parts[1].toInt(),Direction.valueOf(parts[2]))
        "kingdirection" -> GameIntent.KingDirectionChosen( rowIndex = parts[1].toInt(), direction = Direction.valueOf(parts[2]))
        "jackmadness" -> {
            parts.drop(0)
            Log.d("madness", parts[1])
            val swaps = parts[1].removeSurrounding("[", "]").split(",").map { it.trim().toInt() }
            Log.d("madness","$swaps")
            GameIntent.JackMadness(swaps)
        }
        "errore" -> GameIntent.Errore(parts[1])
        else -> throw IllegalArgumentException("Intent sconosciuto: $this")
    }
}


// CardUIStates -> String
// formato: "name|value|house|isRevealed"
fun CardUIStates.serialize(): String = "$name|$value|$house|$isRevealed"

// String -> CardUIStates
fun deserializeCard(raw: String): CardUIStates {
    val parts = raw.split("|")
    return CardUIStates(
        name = parts[0],
        value = parts[1].toInt(),
        house = parts[2],
        isRevealed = parts[3].toBoolean()
    )
}

// List<CardUIStates> -> String
fun List<CardUIStates>.serializeList(): String =
    joinToString(";") { it.serialize() }

// String -> List<CardUIStates>
fun deserializeCardList(raw: String): List<CardUIStates> =
    raw.split(";").map { deserializeCard(it) }

enum class Direction{
    LEFT,
    RIGHT,
    UP,
    DOWN
}

fun Direction.toFunction(rowIndex:Int): (Int, Int) -> Int = when (this) {
    Direction.LEFT -> { i:Int, inc:Int -> rowIndex*7 + i + inc}
    Direction.RIGHT -> { i:Int, inc:Int -> 7*rowIndex + (6-i) - inc}
    Direction.UP -> { i, inc -> rowIndex + 7 * i + inc }
    Direction.DOWN -> { i, inc -> rowIndex + 7 * (3 - i) - inc }
}

fun actionTemplate(type:String, relevantCards:List<Triple<String,Int, Boolean>>, outcome: List<Triple<String,Int, Boolean>>): String{
    return "$type:{$relevantCards} -> {$outcome} "
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
            order
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
