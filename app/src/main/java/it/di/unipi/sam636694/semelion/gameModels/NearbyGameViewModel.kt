package it.di.unipi.sam636694.semelion.gameModels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.deserializeCardList
import it.di.unipi.sam636694.semelion.serialize
import it.di.unipi.sam636694.semelion.toGameIntent
import it.di.unipi.sam636694.semelion.ui.states.ConnectionUiState
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import sendMessage
import com.google.android.gms.nearby.connection.*


class NearbyGameViewModel(
    matchesDao: MatchesDao,
    participationsDao: ParticipationsDao,
    matchStatisticsDao: MatchStatisticsDao,
    playersStatisticsDao: PlayerStatisticsDao,
    userDao: UserDao,
    var endpoint:String?,
    var remoteId: String?,
    var connectionsClient: ConnectionsClient?,
    val localId:String,
) : BaseGameViewModel(matchesDao, participationsDao, matchStatisticsDao, playersStatisticsDao, userDao) {


    private val _connectionState = MutableStateFlow(ConnectionUiState())
    val connectionState: StateFlow<ConnectionUiState> = _connectionState.asStateFlow()

    fun onConnectionResult(endpointId: String, success: Boolean) {
        if (success) {
            _connectionState.update {
                it.copy(
                    connectedEndpointId = endpointId,
                    isSearching = false,
                    status = "Connesso!"
                )
            }
        } else {
            _connectionState.update {
                it.copy(isSearching = false, status = "Connessione fallita")
            }
        }
    }

    fun onDisconnected() {
        _connectionState.update {
            it.copy(connectedEndpointId = null, status = "Disconnesso")
        }
    }

    fun startHosting(serviceId: String) {
        _connectionState.update {
            it.copy(isSearching = true, isHost = true, status = "In attesa di connessioni...")
        }
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient?.startAdvertising(localId, serviceId, connectionCallback, options)
    }

    fun startDiscovery(serviceId: String) {
        _connectionState.update {
            it.copy(isSearching = true, isHost = false, status = "Cerco un host...")
        }
        this.blankGrid()
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient?.startDiscovery(
            serviceId,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    _connectionState.update { it.copy(status = "Host trovato! Connessione...") }
                    connectionsClient?.requestConnection(localId, endpointId, connectionCallback)
                    connectionsClient?.stopDiscovery()
                }
                override fun onEndpointLost(endpointId: String) {
                    _connectionState.update { it.copy(status = "Host perso, riprovo...") }
                }
            },
            options
        )

    }

    fun onSent() {
        _connectionState.update { it.copy(sent = true) }
    }

    fun disconnect() {
        connectionsClient?.stopAllEndpoints()
        _connectionState.update {
            it.copy(connectedEndpointId = null, status = "Disconnesso")
        }
    }

    fun cancelSearch() {
        connectionsClient?.stopAdvertising()
        connectionsClient?.stopDiscovery()
        _connectionState.update {
            it.copy(isSearching = false, status = "Ricerca annullata")
        }
    }


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


    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val raw = String(payload.asBytes()!!, Charsets.UTF_8)
            val messageType = raw.substringBefore(":") + ":"
            val message = raw.substringAfter(":")

            when (messageType) {
                "endpoint:" -> updateRemote(message)
                "grid:" -> {
                    _uiState.update {
                        it.copy(grid = deserializeCardList(message))
                    }
                }
                "uncover:" -> {
                    _uiState.update {
                        it.copy(
                            uncoverDeck = deserializeCardList(message),
                        )
                    }
                    _connectionState.update { it.copy(received = true) }
                }
                "gameaction:" -> produceAction(message)
                else -> _connectionState.update { it.copy(status = "Ricevuto $message") }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> Log.d("Payload", "inviato")
                PayloadTransferUpdate.Status.FAILURE -> Log.d("Payload", "fallito")
                PayloadTransferUpdate.Status.IN_PROGRESS -> Log.d("Payload", "trasferimento...")
            }
        }
    }

    val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _connectionState.update { it.copy(status = "Connessione in arrivo da ${info.endpointName}...") }
            connectionsClient?.acceptConnection(endpointId, payloadCallback)
            connectionsClient?.let { client ->
                sendMessage("endpoint", localId, client, endpointId)
            }
            updateConnectionsInfo(connectionsClient!!, endpointId)
        }

//        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
//            if (result.status.isSuccess) {
//                _connectionState.update {
//                    it.copy(
//                        connectedEndpointId = endpointId,
//                        isSearching = false,
//                        status = "Connesso!"
//                    )
//                }
//                connectionsClient?.stopAdvertising()
//                connectionsClient?.stopDiscovery()
//            } else {
//                _connectionState.update {
//                    it.copy(isSearching = false, status = "Connessione fallita")
//                }
//            }
//        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            onConnectionResult(endpointId, result.status.isSuccess) // ✅ qui
            if (result.status.isSuccess) {
                connectionsClient?.stopAdvertising()
                connectionsClient?.stopDiscovery()
            }
        }

        override fun onDisconnected(endpointId: String) {
            onDisconnected() // ✅ qui
        }

//        override fun onDisconnected(endpointId: String) {
//            _connectionState.update {
//                it.copy(connectedEndpointId = null, status = "Disconnesso")
//            }
//        }
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