package it.di.unipi.sam636694.semelion.utilities.connectionUtils

import android.app.Application
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.Strategy
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.ui.states.ConnectionUiState
import it.di.unipi.sam636694.semelion.ui.states.DiscoveredEndpoint
import it.di.unipi.sam636694.semelion.utilities.avatarMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SemelionNearbyManager(application: Application): SemelionConnectionManager{
    var heartbeatJob: Job? = null
    var lastHeartbeat = System.currentTimeMillis()

    // Salvo uno scope dove far eseguire timeout ed heartbeat job, il supervisor evita che crashi l'intero scope in caso di fallimento di una coroutine
    private val managerJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + managerJob)

    val pingInterval = 1000L
    val pingTimeout = 8000L

    var connectionTimeoutJob: Job? = null

    val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(application)

    private val _connectionState = MutableStateFlow(ConnectionUiState())
    val connectionState: StateFlow<ConnectionUiState> = _connectionState.asStateFlow()

    fun startHosting(serviceId: String,encodedInfo:String,connectionCallback: ConnectionLifecycleCallback) {
        _connectionState.update {
            it.copy(isSearching = true, isHost = true, status = "In attesa di connessioni...")
        }
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient.startAdvertising(encodedInfo, serviceId, connectionCallback, options)
    }

    fun startDiscovery(serviceId: String) {
        _connectionState.update {
            it.copy(isSearching = true, isHost = false, status = "Cerco un host...")
        }

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

    fun connectToEndpoint(endpointId:String,encodedInfo: String,connectionCallback:ConnectionLifecycleCallback){
        _connectionState.update { it.copy(status = "Connessione a $endpointId...") }
        connectionsClient.requestConnection(encodedInfo, endpointId, connectionCallback)
        connectionsClient.stopDiscovery()
        connectionTimeoutJob = scope.launch {
            delay(timeMillis= 10*1000) //10 secondi
            endpointId.let {
                connectionsClient.disconnectFromEndpoint(it)
            }
            Log.d("Pippo","qui")
            _connectionState.update { it.copy(status = "Connessione non riuscita", discoveredEndpoints =emptyList()) }
        }
    }

    fun disconnect(){
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionTimeoutJob?.cancel()
        stopHeartbeat()
        _connectionState.update {
            ConnectionUiState() // reset completo
        }
    }

    //interrompi tentativo di connessione
    fun cancelSearch(){
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionTimeoutJob?.cancel()
        _connectionState.update {
            it.copy(isSearching = false, status = "Ricerca annullata")  // non resettare tutto
        }
    }

    fun sendMessage(messageType:String,message:String,endpoint: String?){
        val formattedMessage = "$messageType:$message"
        val payload = Payload.fromBytes(formattedMessage.toByteArray(Charsets.UTF_8))
        if (endpoint == null) return
        this.connectionsClient.sendPayload(endpoint,payload)
        //if (formattedMessage == "ping:" || formattedMessage == "pong:" ) return
        Log.d("Payload","Message: $formattedMessage sent")
    }

    //PING UTILITY
    fun startHeartbeat(endpoint: String?,noPing: () -> Unit) {
        if (endpoint == null) return
        lastHeartbeat = System.currentTimeMillis()
        heartbeatJob = scope.launch {
            while (true) {
                delay(pingInterval)
                sendMessage("ping", "",endpoint)

                // Controlla se l'ultimo heartbeat ricevuto è troppo vecchio
                if (System.currentTimeMillis() - lastHeartbeat > pingTimeout) {
                    //invoco la lambda che mi viene passata dal chiamante
                    noPing()
                    break
                }
            }
        }
    }

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun onConnectionResult(endpointId: String,localId:String,result: ConnectionResolution,noPing:() -> Unit) {
        when{
            result.status.isSuccess -> {
                connectionTimeoutJob?.cancel()
                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()

                _connectionState.update {
                    it.copy(
                        connectedEndpointId = endpointId,
                        isSearching = false,
                        status = "Connesso!"
                    )
                }

                sendMessage("endpoint", localId,endpointId)
                //startHeartbeat con lambda
                startHeartbeat(endpointId){
                    noPing()
                }

            }
            else -> {
                _connectionState.update {
                    it.copy(isSearching = false, status = "Connessione fallita")
                }
            }
        }
    }

    fun markDisconnected(){
        _connectionState.update {
            it.copy(connectedEndpointId = null, status = "Disconnesso")
        }
    }
    fun markReceived(){
        _connectionState.update {
            it.copy(received = true, gameStarted = true)
        }
    }
    fun markSent(){
        _connectionState.update { it.copy(gameStarted = true, sent = true) }
    }

    fun markConnectionPending(info: ConnectionInfo){
        _connectionState.update { it.copy(status = "Connessione in arrivo da ${info.endpointName}...") }
    }

    fun defaultPayloadReception(message: String){
        _connectionState.update { it.copy(status = "Ricevuto $message") }
    }

    fun pinPongLogic(messageType: String,endpoint: String?){
        if (endpoint == null) return
        when(messageType) {
            "ping:" -> {
                sendMessage("pong", "", endpoint)
            }

            "pong:" -> {
                lastHeartbeat = System.currentTimeMillis()
            }
        }
    }

    fun acceptConnection(endpointId:String,payloadCallback: PayloadCallback){
        connectionsClient.acceptConnection(endpointId, payloadCallback)    }

}