package it.di.unipi.sam636694.semelion.utilities

import android.util.Log
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent

data class ActionData(val type: String, val relevantCards: List<CardInfo>, val outcome: List<CardInfo>)

enum class RowOrder { CRESCENT, DECRESCENT, BOTH }

fun String.toActionData(): ActionData {
    val mainRegex = Regex("""^(\w+):\{(.*)\} -> \{(.*)\}\s*$""")
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
fun CardUIStates.serialize(): String =
    "$name|$value|$house|$isRevealed"

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
