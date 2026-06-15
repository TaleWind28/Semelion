package it.di.unipi.sam636694.semelion.viewModels.gameModels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import it.di.unipi.sam636694.semelion.database.MatchStatisticsDao
import it.di.unipi.sam636694.semelion.database.MatchesDao
import it.di.unipi.sam636694.semelion.database.ParticipationsDao
import it.di.unipi.sam636694.semelion.database.PlayerStatisticsDao
import it.di.unipi.sam636694.semelion.database.UserDao
import it.di.unipi.sam636694.semelion.utilities.deserializeCardList
import it.di.unipi.sam636694.semelion.utilities.serialize
import it.di.unipi.sam636694.semelion.utilities.toGameIntent
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import kotlinx.coroutines.flow.update
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import it.di.unipi.sam636694.semelion.utilities.serializeList
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import it.di.unipi.sam636694.semelion.database.GameModes
import kotlinx.coroutines.Job
import java.util.Locale.getDefault
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.utilities.avatarMap
import android.app.Application
import it.di.unipi.sam636694.semelion.utilities.connectionUtils.SemelionNearbyManager
import it.di.unipi.sam636694.semelion.ui.snackbar.SnackBarController
import it.di.unipi.sam636694.semelion.ui.snackbar.SnackBarEvent

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

    val connectionManager = SemelionNearbyManager(application)
    val connectionState = connectionManager.connectionState

    //coda di azioni da replicare sulla griglia
    private val pendingActions = ArrayDeque<String>()
    private var actionFlushJob: Job? = null

    private var gridAckJob: Job? = null

    var isDestroyed = false

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
                connectionManager.sendMessage("gameaction", msg,endpoint)
                pendingActions.removeFirst()
            }
        }
    }

    //disconnessione -> OK
    fun onDisconnected() {
        if (_uiState.value.phase is GamePhase.GameOver) return

// controllare bene ->
//        if (_uiState.value.phase !is GamePhase.Disconnected){
//            _uiState.update { it.copy(phase = GamePhase.Disconnected) }
//            return
//        }

        disconnect()
        //update di _connectionState
        connectionManager.markDisconnected()
    }
    //hosta una partita -> OK
    fun startHosting(serviceId: String, nickname: String) {
        val encodedAvatar = application.resources.getResourceEntryName(firstPlayerAvatar?:R.drawable.avatar_1)
        val encodedInfo = "$nickname|$encodedAvatar"
        connectionManager.startHosting(serviceId,encodedInfo,connectionCallback)
    }

    //cerca un host -> OK
    fun startDiscovery(serviceId: String) {
        this.blankGrid()
        connectionManager.startDiscovery(serviceId)
    }


    //inizia la connessione con un endpoint -> OK
    fun connectToEndpoint(endpointId:String){
        val encodedAvatar = application.resources.getResourceEntryName(firstPlayerAvatar?:R.drawable.avatar_1)
        val encodedInfo = "$nickname|$encodedAvatar"
        connectionManager.connectToEndpoint(endpointId,encodedInfo,connectionCallback)
    }

    //disconnetti l'utente -> OK
    fun disconnect() {
        connectionManager.disconnect()
        endpoint = null
        wantsToGoBack.value = true
    }

    //inizializzazione del viewModel -> chiama solo setup
    init {
        setup()
    }

    fun sendGridWithAck() {
        gridAckJob?.cancel()
        gridAckJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < 10) {
                connectionManager.sendMessage("grid", _uiState.value.grid.serializeList(),endpoint)
                connectionManager.sendMessage("uncover", _uiState.value.uncoverDeck.serializeList(),endpoint)
                delay(1000)
                attempts++
            }
            Log.d("pinoli","disconnetto gridAckJob")
            // Dopo 10 tentativi senza ACK → disconnetto
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
        isDestroyed = false
    }

    override fun setFirstPlayer() {
        if (!connectionManager.connectionState.value.isHost) return
        super.setFirstPlayer()
        if (uiState.value.firstPlayer == "Guest") _uiState.update { it.copy(phase = GamePhase.WaitingForOpponent) }

    }

    override fun processIntent(intent: GameIntent): Boolean {
        if (connectionState.value.connectedEndpointId == null){
            _uiState.update { it.copy(phase = GamePhase.GameOver)}
            return false
        }
        //mappo la direzione scelta dal re
        val mappedIntent = if (intent is GameIntent.KingDirectionChosen && !connectionManager.connectionState.value.isHost) {
            mapKingDirection(intent)
        } else {
            intent
        }
        //eseguo l'intent
        super.processIntent(mappedIntent)
        //mando l'intent
        sendAction(mappedIntent)
        //preferisco mandare l'intent senza controllare il suo esito perchè:
        // se fallisce da chi crea l'intent sicuramente fallirà anche da chi lo riceve e inoltre
        // mantiene lo stato di gioco conforme tra i dispositivi
        return true
    }

    override fun actionCounter(state: GameUIState, rows: List<List<CardUIStates>>): GameUIState{
        val modifiedState = super.actionCounter(state, rows)

        if (modifiedState.phase is GamePhase.GameOver)return modifiedState

        return if (
            modifiedState.p1Turn && !connectionManager.connectionState.value.isHost
            ||
            !modifiedState.p1Turn && connectionManager.connectionState.value.isHost
        ) {
            modifiedState.copy(phase = GamePhase.WaitingForOpponent)
        } else {
            modifiedState
        }

    }

    override fun findWinner(upperHalf:List<List<CardUIStates>>, bottomHalf:List<List<CardUIStates>>,state: GameUIState): GameUIState{

        return if (connectionManager.connectionState.value.isHost){
            super.findWinner(upperHalf=upperHalf,bottomHalf=bottomHalf,state=state)
        }
        else {
            super.findWinner(upperHalf = bottomHalf,bottomHalf=upperHalf,state=state)
        }
    }
    override fun destroy() {
        if (isDestroyed) return
        endpoint?.let {
            connectionManager.sendMessage("destruction", "player gave up",endpoint)
        }
        if (endpoint == null){
            matchEnd(GameModes.NearBy,"Connection Lost")
        }
        isDestroyed = true
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
                (connectionManager.connectionState.value.isHost && _uiState.value.firstPlayer == "Host") || (!connectionManager.connectionState.value.isHost && _uiState.value.firstPlayer == "Guest")
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

            connectionManager.pinPongLogic(messageType = messageType, endpoint = endpoint)

            when (messageType) {
                "endpoint:" -> {
                    updateRemoteId(message)
                    //inizio il match
                    viewModelScope.launch{
                        matchStart(GameModes.NearBy,opponentName)
                    }
                    //se sono host, decido chi è il primo giocatore, aggiorno la variabile relativa e lo comunico al guest
                    if(connectionState.value.isHost){
                        setFirstPlayer()
                        updateFirstPlayer()
                        connectionManager.sendMessage("starting player",_uiState.value.firstPlayer,endpoint)
                    }
                }
                //imposto il primo giocatore giocando sulla variabile p1Turn
                "starting player:" ->{
                    if (message == "Guest") _uiState.update { it.copy(firstPlayer = message, p1Turn = false,p1Actions = it.p1Actions+1, phase = GamePhase.PlayerTurn) }
                    if (message == "Host") _uiState.update { it.copy(firstPlayer = message,p2Actions = it.p2Actions+1, phase = GamePhase.WaitingForOpponent) }
                    updateFirstPlayer()
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
                    connectionManager.sendMessage("ready", "",endpoint) // ← ACK all'host
                    connectionManager.markReceived()
                }
                "ready:" -> {
                    // Guest ha ricevuto tutto, possiamo procedere
                    gridAckJob?.cancel()

                    connectionManager.markSent()
                }

                "gameaction:" -> produceAction(message)

                "destruction:" -> {
                    connectionManager.stopHeartbeat()
                    _uiState.update { it.copy(phase = GamePhase.GameOver, winner = userID) }
                }

                else -> connectionManager.defaultPayloadReception(message)
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
            connectionManager.markConnectionPending(info)
            val parts = info.endpointName.split("|")
            //salvo il nickname dell'oppo e il suo avatar
            opponentName = parts.getOrElse(0) { info.endpointName }
            secondPlayerAvatar = avatarMap[parts.getOrElse(1) { "default" }]
            // accetta la connessione
            connectionManager.acceptConnection(endpointId,payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {

            connectionManager.onConnectionResult(endpointId=endpointId,localId=localId, result = result){
                if (_uiState.value.phase !is GamePhase.GameOver) destroy()
            }

            if (result.status.isSuccess) {
                endpoint = endpointId
                Log.d("Payload","epdId:$endpoint\nepd:$endpointId")
                //comunico la griglia al guest
                if (connectionState.value.isHost) sendGridWithAck()
            }

        }

        override fun onDisconnected(endpointId: String) {
            Log.d("pinoli","disconnetto dalla callback")
            onDisconnected()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("finotto","ammazzo")
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