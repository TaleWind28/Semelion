package it.di.unipi.sam636694.semelion.gameModels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import it.di.unipi.sam636694.semelion.AudioPlayer
import it.di.unipi.sam636694.semelion.serializeList
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.google.android.gms.nearby.Nearby
import it.di.unipi.sam636694.semelion.database.GameModes

class NearbyGameViewModel(
    private val appContext: Context,
    matchesDao: MatchesDao,
    participationsDao: ParticipationsDao,
    matchStatisticsDao: MatchStatisticsDao,
    playersStatisticsDao: PlayerStatisticsDao,
    userDao: UserDao,
    player: AudioPlayer,
    var endpoint: String?,
    var remoteId: String,
    val localId: String,
) : BaseGameViewModel(matchesDao, participationsDao, matchStatisticsDao, playersStatisticsDao, userDao, player,userID=localId,remoteId) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(appContext)

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
        val gameStarted = _connectionState.value.gameStarted
        disconnect()
        _connectionState.update {
            it.copy(connectedEndpointId = null, status = "Disconnesso", gameStarted = gameStarted)
        }
    }

    fun startHosting(serviceId: String) {
        _connectionState.update {
            it.copy(isSearching = true, isHost = true, status = "In attesa di connessioni...")
        }
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient.startAdvertising(localId, serviceId, connectionCallback, options)
    }

    fun startDiscovery(serviceId: String) {
        _connectionState.update {
            it.copy(isSearching = true, isHost = false, status = "Cerco un host...")
        }
        this.blankGrid()
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient.startDiscovery(
            serviceId,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    _connectionState.update { it.copy(status = "Host trovato! Connessione...") }
                    connectionsClient.requestConnection(localId, endpointId, connectionCallback)
                    connectionsClient.stopDiscovery()
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
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        endpoint = null
        wantsToGoBack.value = true
        _connectionState.update {
            ConnectionUiState() // reset completo
        }

    }

    fun cancelSearch() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        _connectionState.update {
            ConnectionUiState(status = "Ricerca annullata")
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


    fun sendGrid(endpointId: String) {
        viewModelScope.launch {
            Log.d("send", "provo a mandare su $endpointId")
            delay(300)
            sendMessage("grid", _uiState.value.grid.serializeList(), connectionsClient, endpointId)
            sendMessage("uncover", _uiState.value.uncoverDeck.serializeList(), connectionsClient, endpointId)
            onSent()
        }
    }

    fun produceAction(command: String) {
        if (_uiState.value.phase is GamePhase.GameOver) return
        Log.d("PayloadReceived", "actionCommand:$command")
        val action = command.toGameIntent()
        Log.d("PayloadReceived", "action:$action")
        super.processIntent(action)
    }

    fun mapKingDirection(intent: GameIntent.KingDirectionChosen): GameIntent {
        return when (intent.rowIndex) {
            0 -> GameIntent.KingDirectionChosen(rowIndex = 2, direction = intent.direction)
            1 -> GameIntent.KingDirectionChosen(rowIndex = 3, direction = intent.direction)
            2 -> GameIntent.KingDirectionChosen(rowIndex = 0, direction = intent.direction)
            3 -> GameIntent.KingDirectionChosen(rowIndex = 1, direction = intent.direction)
            else -> GameIntent.Errore("indice non consentito")
        }
    }

    override fun processIntent(intent: GameIntent): Boolean {
        if (_connectionState.value.connectedEndpointId == null){
            _uiState.update { it.copy(phase = GamePhase.GameOver)}
            return false
        }

        val mappedIntent = if (intent is GameIntent.KingDirectionChosen && !this.connectionState.value.isHost) {
            mapKingDirection(intent)
        } else {
            intent
        }

        val result = super.processIntent(mappedIntent)
        if (!result) return false
        sendAction(mappedIntent)
        return true
    }

    override fun validateState(cardId: String, state: GameUIState): GameUIState {//cannolo
        val validatedState = super.validateState(cardId, state)
        Log.d("validazionesotto", "${validatedState.p1Turn}")
        if (!(validatedState.phase is GamePhase.Validation || validatedState.phase is GamePhase.PlayerTurn)) return validatedState
        return if (
            validatedState.p1Turn && !connectionState.value.isHost
            ||
            !validatedState.p1Turn && connectionState.value.isHost
        ) {
            validatedState.copy(phase = GamePhase.WaitingForOpponent)
        } else {
            validatedState
        }
    }

    fun sendAction(intent: GameIntent) {
        val action = intent.serialize()
        Log.d("nvm", "$endpoint:$connectionsClient")
        if (endpoint == null) return
        sendMessage("gameaction", action, clientConnectionsClient = connectionsClient, endpoint = endpoint!!)
    }

    fun updateRemoteId(remoteId: String){
        this.remoteId = remoteId
        super.updateSecondPlayer(remoteId)
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val raw = String(payload.asBytes()!!, Charsets.UTF_8)
            val messageType = raw.substringBefore(":") + ":"
            val message = raw.substringAfter(":")

            Log.d("message","$messageType$message,${messageType ==  "endpoint:"} ")
            when (messageType) {
                "endpoint:" -> {
                    Log.d("endpoint","endpoint Ottenuto")

                    viewModelScope.launch {
                        updateRemoteId(message)
                        matchStart(GameModes.NearBy)
                        Log.d("endpoint","match iniziato")
                    }
                    Log.d("endpoint","endpoint Ottenuto")
                }
                "grid:" -> {
                    _uiState.update {
                        it.copy(grid = deserializeCardList(message))
                    }
                }
                "uncover:" -> {
                    _uiState.update {
                        it.copy(uncoverDeck = deserializeCardList(message))
                    }
                    _connectionState.update { it.copy(received = true, gameStarted = true) }
                }
                "gameaction:" -> produceAction(message)
                "destruction:" -> {
                    _uiState.update { it.copy(phase = GamePhase.GameOver, winner = userID) }
                    Log.d("message","fase post destruction:${_uiState.value.phase}")
                }
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
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            sendMessage("endpoint", localId, connectionsClient, endpointId)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            onConnectionResult(endpointId, result.status.isSuccess)
            if (result.status.isSuccess) {
                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()
                endpoint = endpointId
                if (_connectionState.value.isHost) {
                    sendGrid(endpointId)
                    _connectionState.update { it.copy(gameStarted = true) }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            onDisconnected()
        }
    }

    override fun destroy() {
        sendMessage("destruction","player gave up",this.connectionsClient,this.endpoint!!)
        disconnect()
    }

    companion object {
        fun factory(
            matchesDao: MatchesDao,
            participationsDao: ParticipationsDao,
            matchStatisticsDao: MatchStatisticsDao,
            playerStatisticsDao: PlayerStatisticsDao,
            player: AudioPlayer,
            userDao: UserDao,
            localId: String,
            context: Context
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    NearbyGameViewModel(
                        appContext = context,
                        matchesDao = matchesDao,
                        participationsDao = participationsDao,
                        matchStatisticsDao = matchStatisticsDao,
                        playersStatisticsDao = playerStatisticsDao,
                        userDao = userDao,
                        player = player,
                        remoteId = "",
                        localId = localId,
                        endpoint = null,
                    )
                }
            }
        }
    }
}