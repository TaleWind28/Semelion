package it.di.unipi.sam636694.semelion.viewModels.gameModels

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
import it.di.unipi.sam636694.semelion.utilities.deserializeCardList
import it.di.unipi.sam636694.semelion.utilities.serialize
import it.di.unipi.sam636694.semelion.utilities.toGameIntent
import it.di.unipi.sam636694.semelion.ui.states.ConnectionUiState
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.google.android.gms.nearby.connection.*
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import it.di.unipi.sam636694.semelion.utilities.serializeList
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.gms.nearby.Nearby
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.ui.states.DiscoveredEndpoint
import kotlinx.coroutines.Job
import java.util.Locale.getDefault
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.utilities.avatarMap
import android.app.Application
import it.di.unipi.sam636694.semelion.utilities.SnackBarController
import it.di.unipi.sam636694.semelion.utilities.SnackBarEvent
import kotlin.concurrent.Volatile


class NearbyGameViewModel(
    private val application: Application,
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
) :
    BaseGameViewModel(
        matchesDao,
        participationsDao,
        matchStatisticsDao,
        playersStatisticsDao,
        userDao,
        player,
        userID=localId,
        remoteId,
        app = application
    ) {

    private var heartbeatJob: Job? = null
    private var lastHeartbeat = System.currentTimeMillis()
    // Salvi il riferimento alla coroutine

    private val pingInterval = 1000L
    private val pingTimeout = 8000L

    private var connectionTimeoutJob: Job? = null

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(application)

    private val _connectionState = MutableStateFlow(ConnectionUiState())
    val connectionState: StateFlow<ConnectionUiState> = _connectionState.asStateFlow()

    //coda di azioni da replicare sulla griglia
    private val pendingActions = ArrayDeque<String>()
    private var actionFlushJob: Job? = null

    @Volatile
    private var isSimulating:Boolean = false

    // Lato host: aspetta l'ACK prima di procedere
    private var gridAckJob: Job? = null

    fun enqueueAction(message: String) {
        pendingActions.addLast(message)
        flushActions()
    }

    private fun flushActions() {
        if (actionFlushJob?.isActive == true) return
        actionFlushJob = viewModelScope.launch {
            while (pendingActions.isNotEmpty()) {
                val ep = endpoint
                if (ep == null) {
                    delay(200) // aspetta che endpoint sia disponibile
                    continue
                }
                val msg = pendingActions.first()
                sendMessage("gameaction", msg)
                pendingActions.removeFirst()
            }
        }
    }


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
        if (_uiState.value.phase is GamePhase.GameOver) return
        if (_uiState.value.phase !is GamePhase.Disconnected){
            _uiState.update { it.copy(phase = GamePhase.Disconnected) }
            return
        }
        disconnect()
        _connectionState.update {
            it.copy(connectedEndpointId = null, status = "Disconnesso")
        }
    }
    //hosta una partita
    fun startHosting(serviceId: String, nickname: String) {
        _connectionState.update {
            it.copy(isSearching = true, isHost = true, status = "In attesa di connessioni...")
        }
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        val encodedAvatar = application.resources.getResourceEntryName(firstPlayerAvatar?:R.drawable.avatar_1)
        val encodedInfo = "$nickname|$encodedAvatar"
        connectionsClient.startAdvertising(encodedInfo, serviceId, connectionCallback, options)
    }

    //cerca un host
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
                    _connectionState.update { state ->
                        val updated = state.discoveredEndpoints.toMutableList()
                        val parts = info.endpointName.split("|")
                        val name = parts.getOrElse(0) { info.endpointName }
                        val avatar = avatarMap[parts.getOrElse(1) { "default" }]
                        if (updated.none { it.endpointId == endpointId }) {
                            updated.add(DiscoveredEndpoint(endpointId, name,avatar?:R.drawable.avatar_1))
                        }
                        state.copy(
                            discoveredEndpoints = updated,
                            status = "${updated.size} host trovati"
                        )
                    }
                }

                override fun onEndpointLost(endpointId: String) {
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
        val encodedAvatar = application.resources.getResourceEntryName(firstPlayerAvatar?:R.drawable.avatar_1)
        val encodedInfo = "$nickname|$encodedAvatar"
        connectionsClient.requestConnection(encodedInfo, endpointId, connectionCallback)
        connectionsClient.stopDiscovery()

        connectionTimeoutJob = viewModelScope.launch {
            delay(5000)
            endpointId.let {
                connectionsClient.disconnectFromEndpoint(it)
            }
            Log.d("Pippo","qui")
            _connectionState.update { it.copy(status = "Connessione non riuscita", discoveredEndpoints =emptyList()) }
        }
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
        connectionTimeoutJob?.cancel()
        _connectionState.update {
            it.copy(isSearching = false, status = "Ricerca annullata")  // non resettare tutto
        }
    }

    //PING UTILITY
    fun startHeartbeat() {
        lastHeartbeat = System.currentTimeMillis()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                delay(pingInterval)
                sendMessage("ping", "")

                // Controlla se l'ultimo heartbeat ricevuto è troppo vecchio
                if (System.currentTimeMillis() - lastHeartbeat > pingTimeout) {
                    if (_uiState.value.phase !is GamePhase.GameOver) destroy()
                    break
                }
            }
        }
    }

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    //INVIO MESSAGGI PER COMUNICARE TRA PEER
    fun sendMessage(messageType:String,message:String){
        val formattedMessage = "$messageType:$message"
        val payload = Payload.fromBytes(formattedMessage.toByteArray(Charsets.UTF_8))
        if (endpoint == null) return
        this.connectionsClient.sendPayload(endpoint!!,payload)
        if (formattedMessage == "ping:" || formattedMessage == "pong:" ) return
        else Log.d("Payload","Message: $formattedMessage sent")
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
            sendMessage("grid", _uiState.value.grid.serializeList())
            sendMessage("uncover", _uiState.value.uncoverDeck.serializeList())
            onSent()
        }
    }

    fun sendGridWithAck() {
        gridAckJob?.cancel()
        gridAckJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < 5) {
                sendMessage("grid", _uiState.value.grid.serializeList())
                sendMessage("uncover", _uiState.value.uncoverDeck.serializeList())
                delay(1000)
                // Se nel frattempo è arrivato l'ACK, il job viene cancellato
                // (vedi payloadCallback sotto)
                attempts++
            }
            // Dopo 5 tentativi senza ACK → errore di connessione
            onDisconnected()
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
        enqueueAction(action)
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
        Log.d("finder","${decks.first.indexOfFirst { it.value > 7 }}")
        _uiState.update { it.copy(grid = decks.first, uncoverDeck = decks.second, phase = GamePhase.PlayerTurn) }
        validation()
    }

    override fun setFirstPlayer() {
        if (!connectionState.value.isHost) return
        super.setFirstPlayer()
        if (uiState.value.firstPlayer == "Guest") _uiState.update { it.copy(phase = GamePhase.WaitingForOpponent) }

    }

    override fun processIntent(intent: GameIntent): Boolean {
        if (_connectionState.value.connectedEndpointId == null){
            _uiState.update { it.copy(phase = GamePhase.GameOver)}
            return false
        }
        //mappo la direzione scelta dal re
        val mappedIntent = if (intent is GameIntent.KingDirectionChosen && !this.connectionState.value.isHost) {
            mapKingDirection(intent)
        } else {
            intent
        }
        //eseguo l'intent
        val result = super.processIntent(mappedIntent)
        if (!result) return false

        sendAction(mappedIntent)
        return true
    }

    override fun actionCounter(state: GameUIState, rows: List<List<CardUIStates>>): GameUIState{
        val modifiedState = super.actionCounter(state, rows)

        if (modifiedState.phase is GamePhase.GameOver)return modifiedState
        //if (!(modifiedState.phase is GamePhase.Validation || modifiedState.phase is GamePhase.PlayerTurn)) return modifiedState

        return if (
            modifiedState.p1Turn && !connectionState.value.isHost
            ||
            !modifiedState.p1Turn && connectionState.value.isHost
        ) {
            modifiedState.copy(phase = GamePhase.WaitingForOpponent)
        } else {
            modifiedState
        }

    }

    override fun findWinner(upperHalf:List<List<CardUIStates>>, bottomHalf:List<List<CardUIStates>>,state: GameUIState): GameUIState{

        return if (connectionState.value.isHost){
            super.findWinner(upperHalf=upperHalf,bottomHalf=bottomHalf,state=state)
        }
        else {
            super.findWinner(upperHalf = bottomHalf,bottomHalf=upperHalf,state=state)
        }
    }
    override fun destroy() {
        endpoint?.let {
            sendMessage("destruction", "player gave up")
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

    override suspend fun handleFigureRevealed() {
        val phase = _uiState.value.phase
        val nextState = actionCounter(_uiState.value, _uiState.value.grid.chunked(7))

        if (phase != nextState.phase){
            when(phase){
                is GamePhase.JackMadness -> {
                    SnackBarController.sendEvent(
                        event = SnackBarEvent(
                            message = app.getString(R.string.opponentJackRevealed,_uiState.value.jackSwaps)
                        )
                    )

                }
                is GamePhase.QueenPending -> {
                    SnackBarController.sendEvent(
                        event = SnackBarEvent(
                            message = app.getString(R.string.opponentQueenRevealed)
                        )
                    )
                }
                is GamePhase.KingPending -> {
                    SnackBarController.sendEvent(
                        event = SnackBarEvent(
                            message = app.getString(R.string.opponentKingRevealed)
                        )
                    )
                }
                else -> {

                }
            }
            _uiState.update { actionCounter(it, it.grid.chunked(7)) }
        }else{
            super.handleFigureRevealed()
        }


    }

    //UPDATER
    fun updateRemoteId(remoteId: String){
        this.remoteId = remoteId
        super.updateSecondPlayer(remoteId)
    }

    fun updateNickname(nickname: String?){
        this.nickname = nickname?:userID
        super.playerName = this.nickname
    }

    fun updateFirstPlayerAvatar(avatar:Int){
        this.firstPlayerAvatar = avatar
    }

    //aggiorna i matchSummary in base al primo giocatore
    fun updateFirstPlayer(){
        _matchSummary.update {
            val (first, second) = it
            val isFirstPlayer =
                (connectionState.value.isHost && _uiState.value.firstPlayer == "Host") || (!connectionState.value.isHost && _uiState.value.firstPlayer == "Guest")
            Pair(
                if (first.userId == userID) first.copy( wasFirstPLayer = isFirstPlayer) else first,
                if (second.userId == userID) second.copy( wasFirstPLayer = isFirstPlayer) else second
            )
        }
    }

    //CALLBACKS
    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val raw = String(payload.asBytes()!!, Charsets.UTF_8)
            val messageType = raw.substringBefore(":") + ":"
            val message = raw.substringAfter(":")

//            Log.d("message","$messageType$message,${messageType ==  "endpoint:"} ")
//            Log.d("msg","tipo:$messageType")
            when (messageType) {
                "endpoint:" -> {
                    updateRemoteId(message)
                    //inizio il match
                    viewModelScope.launch{
                        matchStart(GameModes.NearBy,opponentName)
                    }
                    //se sono host, decido chi è il primo giocatore, aggiorno la variabile relativa e lo comunico al guest
                    if(_connectionState.value.isHost){
                        setFirstPlayer()
                        updateFirstPlayer()
                        sendMessage("starting player",_uiState.value.firstPlayer)
                    }
                }
                //imposto il primo giocatore giocando sulla variabile p1Turn
                "starting player:" ->{
                    if (message == "Guest") _uiState.update { it.copy(firstPlayer = message, p1Turn = false,p1Actions = it.p1Actions+1, phase = GamePhase.PlayerTurn) }
                    if (message == "Host") _uiState.update { it.copy(firstPlayer = message,p2Actions = it.p2Actions+1, phase = GamePhase.WaitingForOpponent) }
                    updateFirstPlayer()
                    //Log.d("coinFlip","fp:${_uiState.value.firstPlayer}")
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
                    sendMessage("ready", "") // ← ACK all'host
                }
                "ready:" -> {
                    // Guest ha ricevuto tutto, possiamo procedere
                    gridAckJob?.cancel()
                    _connectionState.update { it.copy(gameStarted = true) }
                    onSent()
                }
                "gameaction:" -> produceAction(message)

                "destruction:" -> {
                    stopHeartbeat()
                    //Log.d("disc","hb fermato")
                    _uiState.update { it.copy(phase = GamePhase.GameOver, winner = userID) }
                   //Log.d("message","fase post destruction:${_uiState.value.phase}")
                }
                "ping:" -> {
                    sendMessage("pong", "")
                }
                "pong:" -> {
                    lastHeartbeat = System.currentTimeMillis()
                }
                else -> _connectionState.update { it.copy(status = "Ricevuto $message") }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS ->{}// Log.d("Payload", "inviato")
                PayloadTransferUpdate.Status.FAILURE -> Log.d("Payload", "fallito")
                PayloadTransferUpdate.Status.IN_PROGRESS -> {}//Log.d("Payload", "trasferimento...")
            }
        }
    }

    val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _connectionState.update { it.copy(status = "Connessione in arrivo da ${info.endpointName}...") }
            val parts = info.endpointName.split("|")
            //salvo il nickname dell'oppo e il suo avatar
            opponentName = parts.getOrElse(0) { info.endpointName }
            secondPlayerAvatar = avatarMap[parts.getOrElse(1) { "default" }]
            // accetta la connessione
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            onConnectionResult(endpointId, result.status.isSuccess)

            when{
                result.status.isSuccess -> {
                    connectionTimeoutJob?.cancel()
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                    endpoint = endpointId

                    sendMessage("endpoint", localId)
                    startHeartbeat()
                    if (_connectionState.value.isHost) {
                        //comunico la griglia al guest
                        sendGridWithAck()
                        _connectionState.update { it.copy(gameStarted = true) }
                    }
                }
                else -> {
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
            application: Application
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    NearbyGameViewModel(
                        application = application,
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