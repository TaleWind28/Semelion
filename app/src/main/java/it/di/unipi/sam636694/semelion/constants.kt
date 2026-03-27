package it.di.unipi.sam636694.semelion

import it.di.unipi.sam636694.semelion.R

fun mapHouse(value:Int):String{
    return when (value){
        1 -> "F"
        2 -> "D"
        3 -> "C"
        4 -> "P"
        else -> "joker"
    }
}

//coefficienti di riga per trovare le carte nella stessa colonna velocemente
val rowCoefficients = listOf<Int>(
    0,
    7,
    14,
    21,
)

enum class RowOrder { CRESCENT, DECRESCENT, BOTH }

val cardImageMap = mapOf(
    "1F" to R.drawable.fiori_1,
    "2F" to R.drawable.fiori_2,
    "3F" to R.drawable.fiori_3,
    "4F" to R.drawable.fiori_4,
    "5F" to R.drawable.fiori_5,
    "6F" to R.drawable.fiori_6,
    "7F" to R.drawable.fiori_7,
    "1C" to R.drawable.cuori_1,
    "2C" to R.drawable.cuori_2,
    "3C" to R.drawable.cuori_3,
    "4C" to R.drawable.cuori_4,
    "5C" to R.drawable.cuori_5,
    "6C" to R.drawable.cuori_6,
    "7C" to R.drawable.cuori_7,
    "1D" to R.drawable.denari_1,
    "2D" to R.drawable.denari_2,
    "3D" to R.drawable.denari_3,
    "4D" to R.drawable.denari_4,
    "5D" to R.drawable.denari_5,
    "6D" to R.drawable.denari_6,
    "7D" to R.drawable.denari_7,
    "1P" to R.drawable.picche_1,
    "2P" to R.drawable.picche_2,
    "3P" to R.drawable.picche_3,
    "4P" to R.drawable.picche_4,
    "5P" to R.drawable.picche_5,
    "6P" to R.drawable.picche_6,
    "7P" to R.drawable.picche_7
)
