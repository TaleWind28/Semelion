package it.di.unipi.sam636694.semelion.ui.states

import it.di.unipi.sam636694.semelion.Direction

sealed class GameIntent {
    data class CardClicked(val cardId: String) : GameIntent()

    data class SwapCards(val id1: String, val id2: String) : GameIntent()

    data class QueenDirectionChosen(val colIndex:Int,val direction: Direction) : GameIntent()
    data class KingDirectionChosen(val rowIndex: Int, val direction: Direction) : GameIntent()

    data object JackMadness : GameIntent()

    data class Errore(val id: String) : GameIntent()
}