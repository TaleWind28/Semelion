package it.di.unipi.sam636694.semelion.gameModels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.nearby.connection.ConnectionsClient
import it.di.unipi.sam636694.semelion.ActionType
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.gameActionTemplate
import it.di.unipi.sam636694.semelion.parseGameAction
import it.di.unipi.sam636694.semelion.parseIntent
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import kotlinx.coroutines.flow.update
import sendMessage

class NearbyGameViewModel(
    matchesDao: MatchesDao,
    participationsDao: ParticipationsDao,
    matchStatisticsDao: MatchStatisticsDao,
    playersStatisticsDao: PlayerStatisticsDao,
    userDao: UserDao,
    var endpoint:String?,
    var remoteId: String?,
    var connectionsClient: ConnectionsClient?,
    localId:String
) : BaseGameViewModel(matchesDao, participationsDao, matchStatisticsDao, playersStatisticsDao, userDao) {


    override fun setup() {
        val decks = createDecks()
        _uiState.update { it.copy(grid = decks.first, uncoverDeck = decks.second, phase = GamePhase.PlayerTurn) }
        validation()
    }

    init {
        setup()
    }

    fun updateRemote(remoteId:String){
        this.remoteId = remoteId
    }

    fun updateConnectionsInfo(connectionsClient: ConnectionsClient,endpoint: String?){
        this.connectionsClient = connectionsClient
        this.endpoint = endpoint
    }

    fun produceAction(command:String){
        Log.d("PayloadReceived","actionCommand:$command")
        val action = parseGameAction(command)
        Log.d("PayloadReceived","action:$action")
        when(action?.type){
            ActionType.Swap -> super.processIntent(GameIntent.SwapCards(action.cards.first(),action.cards.last()))
            ActionType.Reveal -> super.processIntent(GameIntent.CardClicked(action.cards.first()))
            else -> Log.d("produce","Azione non riconosciuta")
        }
    }

    //overloading
     override fun processIntent(intent: GameIntent) {
        sendAction(intent)
        super.processIntent(intent)
    }

    //mando messaggio al bro per replicare azione
    fun sendAction(intent: GameIntent){
        val rawAction = parseIntent(intent) ?: return
        val action = gameActionTemplate(rawAction)
        Log.d("nvm","$endpoint:$connectionsClient")
        if (endpoint == null || connectionsClient == null) return
        Log.d("nvm","$endpoint:$connectionsClient")
        sendMessage("gameaction",action, clientConnectionsClient = connectionsClient!!, endpoint = endpoint!!)
    }




    override fun matchEnd() {
        TODO("Not yet implemented")
    }

    companion object {
        fun factory(matchesDao: MatchesDao, participationsDao: ParticipationsDao, matchStatisticsDao: MatchStatisticsDao, playerStatisticsDao: PlayerStatisticsDao,userDao: UserDao, localId:String): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    NearbyGameViewModel(
                        matchesDao,
                        participationsDao,
                        matchStatisticsDao,
                        playersStatisticsDao = playerStatisticsDao,
                        userDao = userDao,
                        remoteId = null,
                        localId = localId,
                        endpoint = null,
                        connectionsClient = null
                        )
                }
            }
        }
    }


}