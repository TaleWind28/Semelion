package it.di.unipi.sam636694.semelion.ui.theme

data class CardUIStates(
    val name:String,
    val value: Int,
    val house: String,
    val isRevealed: Boolean = false
)

data class GameUIState (
    val grid: List<CardUIStates> = emptyList(),
    val uncoverDeck : List<CardUIStates> = emptyList(),
    val p1Actions: Int = 1,
    val p1ActionsUsed: Int = 0,
    val p2Actions: Int = 1,
    val p2ActionsUsed: Int = 0,
    val p1Turn: Boolean = true,
    val isLoading: Boolean = false,
    val revealedCards: List<String> = emptyList()
)