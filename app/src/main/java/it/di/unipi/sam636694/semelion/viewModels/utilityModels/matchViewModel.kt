package it.di.unipi.sam636694.semelion.viewModels.utilityModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import it.di.unipi.sam636694.semelion.ui.screens.DisplayPlayerStats
import it.di.unipi.sam636694.semelion.viewModels.gameModels.BaseGameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MatchViewModel(private val app: Application): AndroidViewModel(application = app) {
    private val _uiState = MutableStateFlow(MatchUIState())

    val uiState = _uiState.asStateFlow()

    //creo le informazioni da mostrare nella schermata
    fun retrieveEndGameStatsInfo(viewModel: BaseGameViewModel){
        val p1Stats = viewModel.matchSummary.value.first
        val p2Stats = viewModel.matchSummary.value.second

        val p2Display = DisplayPlayerStats(
            name = viewModel.opponentName,
            timePlayed = (System.currentTimeMillis() - p1Stats.date).toString(),
            figuresRevealed = p2Stats.figureRevealed,
            totalMoves = p2Stats.totalActions,
            isFirstPlayer = p2Stats.wasFirstPLayer,
            avatarRes = viewModel.secondPlayerAvatar,
            isWinner = p2Stats.winner ?: false
        )
        val p1Display = DisplayPlayerStats(
            name = viewModel.playerName,
            timePlayed = (System.currentTimeMillis() - p1Stats.date).toString(),
            figuresRevealed = p1Stats.figureRevealed,
            totalMoves = p1Stats.totalActions,
            isFirstPlayer = p1Stats.wasFirstPLayer,
            avatarRes = viewModel.firstPlayerAvatar,
            isWinner = p1Stats.winner ?: false
        )

        _uiState.update { it.copy(p1Stats = p1Display, p2Stats = p2Display, p1Wins = p1Display.isWinner, draw = !p1Display.isWinner && !p2Display.isWinner) }
    }

}

data class MatchUIState(
    val p1Stats: DisplayPlayerStats? = null,
    val p2Stats: DisplayPlayerStats? = null,
    val p1Wins:Boolean = false,
    val draw: Boolean = false
)

