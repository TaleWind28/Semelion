package it.di.unipi.sam636694.semelion
// ─────────────────────────────────────────────────────────────────────────────
// GameIntent — ogni azione possibile dell'utente.
//
// La View non chiama metodi sul ViewModel: costruisce un Intent e lo invia
// tramite viewModel.processIntent(...). Questo rende ogni interazione
// esplicita, tracciabile e testabile in isolamento.
// ─────────────────────────────────────────────────────────────────────────────
sealed class GameIntent {

    /** Il giocatore ha toccato una carta coperta per rivelarla. */
    data class CardClicked(val cardId: String) : GameIntent()

    /** Il giocatore ha scelto di scambiare due carte. */
    data class SwapCards(val id1: String, val id2: String) : GameIntent()

    /**
     * Il giocatore ha scelto la direzione per lo spostamento della Regina.
     * Attivo solo quando la fase è QueenPending.
     */
    data class QueenDirectionChosen(val direction: (Int, Int) -> Int) : GameIntent()

    /**
     * Il giocatore ha scelto la direzione per lo spostamento del Re.
     * Attivo solo quando la fase è KingPending.
     */
    data class KingDirectionChosen(val direction: (Int, Int) -> Int) : GameIntent()
}