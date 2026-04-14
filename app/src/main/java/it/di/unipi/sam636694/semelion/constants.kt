package it.di.unipi.sam636694.semelion

import androidx.compose.ui.graphics.Color

fun mapHouse(value:Int):String{
    return when (value){
        1 -> "F"
        2 -> "D"
        3 -> "C"
        4 -> "P"
        else -> "joker"
    }
}

fun colorHouse(value:String):String{
    return when(value){
        "C"  -> "red"
        "D"  -> "red"
        "F" -> "black"
        "P" -> "black"
        "red" -> "red"
        "black" -> "black"
        else -> "errorHouse"
    }
}

enum class RowOrder { CRESCENT, DECRESCENT, BOTH }

val JOLLY_COLOR = listOf("black","red")

const val DELAY_TIME: Long = 500
const val UNCOVER_DECK_SIZE = 8

val POSITION_VALUES = Pair(first = {rid:Int,pos:Int -> 7*(rid+1)-pos},second= {rid:Int,pos:Int -> pos+1-(7*rid)})
val SEMELION_FIGURES = listOf(
    Pair(10,"D"),
    Pair(9,"C"),
    Pair(9,"P"),
    Pair(9,"F"),
    Pair(8,"C"),
    Pair(8,"P")

    )
val cardImageMap = mapOf(
    "1F" to R.drawable.fiori_1,
    "2F" to R.drawable.fiori_2,
    "3F" to R.drawable.fiori_3,
    "4F" to R.drawable.fiori_4,
    "5F" to R.drawable.fiori_5,
    "6F" to R.drawable.fiori_6,
    "7F" to R.drawable.fiori_7,
    "8F" to R.drawable.fiori_8,
    "9F" to R.drawable.fiori_9,
    "10F" to R.drawable.fiori_10,
    "1C" to R.drawable.cuori_1,
    "2C" to R.drawable.cuori_2,
    "3C" to R.drawable.cuori_3,
    "4C" to R.drawable.cuori_4,
    "5C" to R.drawable.cuori_5,
    "6C" to R.drawable.cuori_6,
    "7C" to R.drawable.cuori_7,
    "8C" to R.drawable.cuori_8,
    "9C" to R.drawable.cuori_9,
    "10C" to R.drawable.cuori_10,
    "1D" to R.drawable.denari_1,
    "2D" to R.drawable.denari_2,
    "3D" to R.drawable.denari_3,
    "4D" to R.drawable.denari_4,
    "5D" to R.drawable.denari_5,
    "6D" to R.drawable.denari_6,
    "7D" to R.drawable.denari_7,
    "8D" to R.drawable.denari_8,
    "9D" to R.drawable.denari_9,
    "10D" to R.drawable.denari_10,
    "1P" to R.drawable.picche_1,
    "2P" to R.drawable.picche_2,
    "3P" to R.drawable.picche_3,
    "4P" to R.drawable.picche_4,
    "5P" to R.drawable.picche_5,
    "6P" to R.drawable.picche_6,
    "7P" to R.drawable.picche_7,
    "8P" to R.drawable.picche_8,
    "9P" to R.drawable.picche_9,
    "10P" to R.drawable.picche_10,
    "joker_red" to R.drawable.joker_red,
    "joker_black" to R.drawable.joker_black
)


// ─── Colori del tema ──────────────────────────────────────────────────────────
val GreenAccent   = Color(0xFF3BFF7C)
val TextPrimary   = Color(0xFF111111)
val TextSecondary = Color(0xFF888888)