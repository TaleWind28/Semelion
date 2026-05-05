package it.di.unipi.sam636694.semelion.gameModels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.nearby.connection.ConnectionsClient
import it.di.unipi.sam636694.semelion.Direction
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.serialize
import it.di.unipi.sam636694.semelion.toGameIntent
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
        val action = command.toGameIntent()
        Log.d("PayloadReceived","action:$action")
        super.processIntent(action)
    }

    //overloading
     override fun processIntent(intent: GameIntent): Boolean {
         //eseguo l'azione in locale
        val result  = super.processIntent(intent)
        //se è stata ammessa la forwardo al peer
        if (!result) return false
//        //specchio la direzione della donna
//        if (intent is GameIntent.QueenDirectionChosen){
//            if (intent.direction == Direction.UP) sendAction(GameIntent.QueenDirectionChosen(intent.colIndex, Direction.DOWN))
//            if (intent.direction == Direction.DOWN) sendAction(GameIntent.QueenDirectionChosen(intent.colIndex, Direction.UP))
//        }else{
//            sendAction(intent)
//        }
        sendAction(intent)
        return true
    }

    //mando messaggio al bro per replicare azione
    fun sendAction(intent: GameIntent){
        val action = intent.serialize()
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