package it.di.unipi.sam636694.semelion

sealed class GameIntent {
    data class CardClicked(val cardId: String) : GameIntent()

    data class SwapCards(val id1: String, val id2: String) : GameIntent()

    data class QueenDirectionChosen(val direction: (Int, Int) -> Int) : GameIntent()

    data class KingDirectionChosen(val direction: (Int, Int) -> Int) : GameIntent()
}