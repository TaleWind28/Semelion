package it.di.unipi.sam636694.semelion.viewModels.gameModels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.Matches
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatistics
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SemelionGameViewModel(
    matchesDao: MatchesDao,
    participationsDao: ParticipationsDao,
    matchStatisticsDao: MatchStatisticsDao,
    playersStatisticsDao: PlayerStatisticsDao,
    userDao: UserDao,
    player: AudioPlayer,
    userID:String,
    secondPlayerId:String,
    application: Application
) : BaseGameViewModel(matchesDao, participationsDao, matchStatisticsDao, playersStatisticsDao, userDao, player, userID, secondPlayerId, app = application){

    init {
        setup()
    }
    //variabili per la riesumazione delle partite
    private var wasResumed = false

    var suspendedFound by mutableStateOf(false)

    private var resumedMatchId:Long = -5

    private var resumedMatchState: GameUIState = GameUIState()

    override fun setup(){
        //creo i mazzi
        val decks = createDecks()
        //aggiorno lo stato di gioco
        _uiState.value = GameUIState(
            grid = decks.first,
            uncoverDeck = decks.second,
            phase = GamePhase.Loading
        )
        //imposto il primo giocatore
        setFirstPlayer()
        viewModelScope.launch {
            //se sono in fase di caricamento oltre a impostare i giocatori controllo se esistono partite sospese
            if (_uiState.value.phase is GamePhase.Loading){
                super.playerName = userDao.getUserById(userID)?.nickName ?: "Player 1"
                firstPlayerAvatar = userDao.getUserById(userID)?.avatar
                secondPlayerAvatar = R.drawable.sora_avatar
                val suspendedMatch = matchesDao.getSuspendedMatch()
                if ( suspendedMatch == null) {
                    //se non esistono inizio una partita normale
                    matchStart(GameModes.ScreenSharing)
                    _uiState.update { it.copy(phase = GamePhase.PlayerTurn) }
                }
                else{
                    //altrimenti lo notifico all'utente e attendo
                    resumedMatchId = suspendedMatch.matchId
                    resumedMatchState = suspendedMatch.gameState
                    suspendedFound = true
                }
            }
        }
    }

    //riprendo un match
    fun resumeMatch(){
        wasResumed = true
        _uiState.update { resumedMatchState }
    }

    //creo una nuova partita
    fun newMatch(){
        viewModelScope.launch {
            //cancello il match rimasto sospeso se era presente
            endResumedMatch()
            matchesDao.update(Matches(matchId = resumedMatchId, GameModes.ScreenSharing,resumedMatchState, isCompleted = true))
            matchStart(GameModes.ScreenSharing)

            _uiState.update { it.copy(phase = GamePhase.PlayerTurn) }
        }
    }

    suspend fun endResumedMatch(){
        //se non trovo il match le statistiche a lui relative anch'esse non verranno trovate
        val match = matchesDao.getMatchById(resumedMatchId) ?: return
        matchesDao.update(match.copy(isCompleted = true))

        val stats = matchStatisticsDao.getStatsByMatch(resumedMatchId)

        stats.forEach {
            val isLocalUser = userID == it.userId
            val userStats = playersStatisticsDao.getStatsByUser(it.userId)
            //aggiorno le statistiche dei giocatori
            if (userStats != null){
                playersStatisticsDao.update(
                    userStats.copy(
                        matchesPlayed = userStats.matchesPlayed + 1,
                        matchesLost = if (isLocalUser)userStats.matchesLost + 1 else 0,
                        matchesWon = if (!isLocalUser)userStats.matchesWon + 1 else 0,
                        currentStreak = if (!isLocalUser)userStats.currentStreak + 1 else 0,
                        bestStreak = if (!isLocalUser) if(userStats.currentStreak +1 > userStats.bestStreak)userStats.currentStreak +1  else userStats.bestStreak else userStats.bestStreak,

                    )
                )
            }else{
                playersStatisticsDao.insert(
                    PlayerStatistics(
                        userId = userID,
                        matchesPlayed = 1,
                        matchesLost = if (isLocalUser)1 else 0,
                        matchesWon = if (!isLocalUser)1 else 0,
                        matchesDrawn = 0,
                        currentStreak = if (!isLocalUser)1 else 0,
                        bestStreak = if (!isLocalUser)1 else 0
                    )
                )
            }
            //aggiorno le statistiche relative al match, queste sono sicuro che esistano perchè:
            // se il match è presente nella tabella allora sono state create le statistiche
            matchStatisticsDao.update(
                it.copy(
                    outcome = "Not Continued",
                    winner = !isLocalUser //l'utente locale viene considerato sconfitto
                )
            )
        }

    }

    //termino il match
    override fun matchEnd(mode: GameModes,loser:String?,resumedMatchId:Long?) {
       super.matchEnd(mode, loser, resumedMatchId = if (wasResumed) this.resumedMatchId else null)
    }

    //non è servito distruggere il vm ma ovviamente la funzione deve essere implementata
    override fun destroy() {
    }

    //METODO FACTORY PER ISTANZIARE IL VIEWMODEL
    companion object {
        fun factory(
            matchesDao: MatchesDao,
            participationsDao: ParticipationsDao,
            matchStatisticsDao: MatchStatisticsDao,
            playerStatisticsDao: PlayerStatisticsDao,
            userDao: UserDao,
            player: AudioPlayer,
            userID:String,
            secondPlayerId:String,
            application: Application
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    SemelionGameViewModel(
                        matchesDao,
                        participationsDao,
                        matchStatisticsDao,
                        playersStatisticsDao = playerStatisticsDao,
                        userDao = userDao,
                        player= player,
                        userID= userID,
                        secondPlayerId= secondPlayerId,
                        application = application
                        )
                }
            }
        }
    }

}

