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
    val incorrectSevenReveled: Boolean = false,
    val p1Turn: Boolean = true,
    val isLoading: Boolean = false,
    val revealedCards: List<String> = emptyList(),
    val phase: GamePhase = GamePhase.PlayerTurn,
    val lastReplacedCard : String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// GamePhase — descrive in quale fase si trova il gioco.
// È un dato puro: nessuna logica, nessun riferimento al ViewModel.
// ─────────────────────────────────────────────────────────────────────────────
sealed class GamePhase {
    /** Il giocatore corrente può rivelare carte e fare swap. */
    object PlayerTurn   : GamePhase()

    /** È stata rivelata una Regina: attesa della scelta di direzione. */
    object QueenPending : GamePhase()

    /** È stato rivelato un Re: attesa della scelta di direzione. */
    object KingPending  : GamePhase()

    /** La partita è terminata. */
    object GameOver     : GamePhase()

    object Validation : GamePhase()
}