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
import com.google.android.gms.nearby.Nearby
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.ui.states.DiscoveredEndpoint
import kotlinx.coroutines.Job
import java.util.Locale.getDefault

class NearbyGameViewModel(
    private val appContext: Context,
    matchesDao: MatchesDao,
    participationsDao: ParticipationsDao,
    matchStatisticsDao: MatchStatisticsDao,
    playersStatisticsDao: PlayerStatisticsDao,
    userDao: UserDao,
    player: AudioPlayer,
    var nickname: String,
    var endpoint: String?,
    var remoteId: String,
    val localId: String,
) : BaseGameViewModel(matchesDao, participationsDao, matchStatisticsDao, playersStatisticsDao, userDao, player,userID=localId,remoteId) {

    private var heartbeatJob: Job? = null
    private var lastHeartbeat = System.currentTimeMillis()
    private val pingInterval = 1000L
    private val pingTimeout = 8000L
    
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
        disconnect()
        _connectionState.update {
            it.copy(connectedEndpointId = null, status = "Disconnesso", gameStarted = _connectionState.value.gameStarted)
        }
    }
    //hosta una partita
    fun startHosting(serviceId: String) {
        _connectionState.update {
            it.copy(isSearching = true, isHost = true, status = "In attesa di connessioni...")
        }
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient.startAdvertising(localId, serviceId, connectionCallback, options)
    }

    //cerca un host
    fun startDiscovery(serviceId: String) {
        //modifico lo stato di connessione
        _connectionState.update {
            it.copy(isSearching = true, isHost = false, status = "Cerco un host...")
        }
        //svouto la griglia
        this.blankGrid()

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()

        connectionsClient.startDiscovery(
            serviceId,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    _connectionState.update { state ->
                        val updated = state.discoveredEndpoints.toMutableList()
                        if (updated.none{ it.endpointId == endpointId }){
                            updated.add(DiscoveredEndpoint(endpointId, info.endpointName))
                        }
                        state.copy(
                            discoveredEndpoints = updated,
                            status = "${updated.size} host trovati"
                        )
                        //state.copy(status = "Host trovato! Connessione...")
                    }
                }

                override fun onEndpointLost(endpointId: String) {
                    // Rimuove dalla lista se l'host sparisce
                    _connectionState.update { state ->
                        val updated = state.discoveredEndpoints.filter { it.endpointId != endpointId }
                        state.copy(
                            discoveredEndpoints = updated,
                            status = if (updated.isEmpty()) "Nessun host trovato" else "${updated.size} host trovati"
                        )
                    }
                }
            },
            options
        )
    }

    //inizia la connessione con un endpoint
    fun connectToEndpoint(endpointId:String){
        _connectionState.update { it.copy(status = "Connessione a $endpointId...") }
        connectionsClient.requestConnection(localId, endpointId, connectionCallback)
        connectionsClient.stopDiscovery()
    }

    fun onSent() {
        _connectionState.update { it.copy(sent = true) }
    }

    //disconnetti l'utente
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

    //interrompi tentativo di connessione
    fun cancelSearch() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        _connectionState.update {
            ConnectionUiState(status = "Ricerca annullata")
        }
    }

    //PING UTILITY
    fun startHeartbeat(endpointId: String) {
        lastHeartbeat = System.currentTimeMillis()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                delay(pingInterval)
                sendMessage("ping", "", connectionsClient, endpointId)

                // Controlla se l'ultimo heartbeat ricevuto è troppo vecchio
                if (System.currentTimeMillis() - lastHeartbeat > pingTimeout) {
                    destroy()
                    break
                }
            }
        }
    }

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    //inizializzazione del viewModel -> chiama solo setup
    init {
        setup()
    }

    //GESTIONE DELLA PARTITA
    fun sendGrid(endpointId: String) {
        viewModelScope.launch {
            Log.d("send", "provo a mandare su $endpointId")
            delay(300)
            sendMessage("grid", _uiState.value.grid.serializeList(), connectionsClient, endpointId)
            sendMessage("uncover", _uiState.value.uncoverDeck.serializeList(), connectionsClient, endpointId)
            onSent()
        }
    }

    //simula l'azione dell'avversario sulla griglia
    fun produceAction(command: String) {
        if (_uiState.value.phase is GamePhase.GameOver) return
        Log.d("PayloadReceived", "actionCommand:$command")
        val action = command.toGameIntent()
        Log.d("PayloadReceived", "action:$action")
        super.processIntent(action)
    }

    //invia l'azione effettuata all'avversario
    fun sendAction(intent: GameIntent) {
        val action = intent.serialize()
        Log.d("nvm", "$endpoint:$connectionsClient")
        if (endpoint == null) return
        sendMessage("gameaction", action, clientConnectionsClient = connectionsClient, endpoint = endpoint!!)
    }

    //interpreta la direzione del re
    fun mapKingDirection(intent: GameIntent.KingDirectionChosen): GameIntent {
        return when (intent.rowIndex) {
            0 -> GameIntent.KingDirectionChosen(rowIndex = 2, direction = intent.direction)
            1 -> GameIntent.KingDirectionChosen(rowIndex = 3, direction = intent.direction)
            2 -> GameIntent.KingDirectionChosen(rowIndex = 0, direction = intent.direction)
            3 -> GameIntent.KingDirectionChosen(rowIndex = 1, direction = intent.direction)
            else -> GameIntent.Errore("indice non consentito")
        }
    }

    //OVERRIDE
    override fun setup() {
        val decks = createDecks()
        _uiState.update { it.copy(grid = decks.first, uncoverDeck = decks.second, phase = GamePhase.PlayerTurn) }
        validation()
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

    override fun destroy() {
        endpoint?.let {
            sendMessage("destruction", "player gave up", connectionsClient, it)
        }
        if (endpoint == null){
            matchEnd(GameModes.NearBy,"Connection Lost")
        }
        disconnect()
    }

    override fun calculateOutcome(loser:String?,state: GameUIState):Pair<String,Boolean?>{
        val outcome = loser ?: state.winner ?: "interrotta"
        Log.d("outcome","$outcome, ${state.winner}")

        return when {
            loser == userID       -> "vince $secondPlayerId" to false
            loser == secondPlayerId -> "vince $userID" to true
            outcome.lowercase(getDefault()).contains("vince p1") -> outcome to true
            outcome.lowercase(getDefault()).contains("vince p2") -> outcome to false
            outcome == userID       -> outcome to true
            outcome == secondPlayerId -> outcome to false
            else -> outcome to null
        }
    }

    //UPDATER
    fun updateRemoteId(remoteId: String){
        this.remoteId = remoteId
        super.updateSecondPlayer(remoteId)
    }

    fun updateNickname(nickname: String?){
        this.nickname = nickname?:userID
    }

    fun updateFirstPlayer(){
        _matchSummary.update {
            it.map { stat ->
                if (stat.userId == userID){
                    stat.copy(wasFirstPLayer = connectionState.value.isHost)
                }
                else stat
            }
        }
    }

    //CALLBACKS
    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val raw = String(payload.asBytes()!!, Charsets.UTF_8)
            val messageType = raw.substringBefore(":") + ":"
            val message = raw.substringAfter(":")

            Log.d("message","$messageType$message,${messageType ==  "endpoint:"} ")
            when (messageType) {
                "endpoint:" -> {
                    updateRemoteId(message)
                    updateFirstPlayer()
                    sendMessage("nickname",nickname,connectionsClient, endpointId)
                    Log.d("endpoint","endpoint Ottenuto")
                }
                "nickname:" ->{
                    viewModelScope.launch{
                        matchStart(GameModes.NearBy,message)
                    }

                }
                "grid:" -> {
                    _uiState.update {
                        it.copy(grid = deserializeCardList(message))
                    }
                }
                "uncover:" -> {
                    _uiState.update {
                        it.copy(uncoverDeck = deserializeCardList(message), phase = GamePhase.WaitingForOpponent)
                    }
                    _connectionState.update { it.copy(received = true, gameStarted = true) }
                }
                "gameaction:" -> produceAction(message)
                "destruction:" -> {
                    stopHeartbeat()
                    _uiState.update { it.copy(phase = GamePhase.GameOver, winner = userID) }
                    Log.d("message","fase post destruction:${_uiState.value.phase}")
                }
                "ping:" -> {
                    sendMessage("pong", "", connectionsClient, endpointId)
                }
                "pong:" -> {
                    lastHeartbeat = System.currentTimeMillis()
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
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            onConnectionResult(endpointId, result.status.isSuccess)
            if (result.status.isSuccess) {
                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()
                endpoint = endpointId
                sendMessage("endpoint", localId, connectionsClient, endpointId)
                startHeartbeat(endpointId)
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

    //METODO FACTORY PER ISTANZIARE IL VIEWMODEL
    companion object {
        fun factory(
            matchesDao: MatchesDao,
            participationsDao: ParticipationsDao,
            matchStatisticsDao: MatchStatisticsDao,
            playerStatisticsDao: PlayerStatisticsDao,
            player: AudioPlayer,
            nickname:String,
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
                        nickname = nickname,
                        remoteId = "",
                        localId = localId,
                        endpoint = null,
                    )
                }
            }
        }
    }
}