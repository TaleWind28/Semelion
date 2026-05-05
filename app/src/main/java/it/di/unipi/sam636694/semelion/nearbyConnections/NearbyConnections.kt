import android.Manifest
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.nearby.connection.*
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.gameModels.NearbyGameViewModel
import it.di.unipi.sam636694.semelion.gameModels.SemelionGameViewModel
import it.di.unipi.sam636694.semelion.serializeList
import androidx.compose.runtime.collectAsState
import it.di.unipi.sam636694.semelion.SemelionScreen
import it.di.unipi.sam636694.semelion.deserializeCardList

import it.di.unipi.sam636694.semelion.ui.states.CardUIStates

@Composable
fun SemelionConnectionsScreen(db: SemelionDB, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val connectionsClient = remember { Nearby.getConnectionsClient(context) }
    val SERVICE_ID = "com.tuaapp.semelion"

    val localEndpointName = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    var connectedEndpointId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Scegli un ruolo per iniziare") }
    var isSearching by remember { mutableStateOf(false) }
    var isHost by remember { mutableStateOf(false) }
    var griglia by remember { mutableStateOf<List<CardUIStates>>(emptyList()) }
    var uncoverDeck by remember { mutableStateOf<List<CardUIStates>>(emptyList()) }
    var sent by remember {mutableStateOf(false) }
    var received by remember {mutableStateOf(false) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()
    }

    //CAMBIARE IL TIPO//
    val nvm: NearbyGameViewModel = viewModel(
        factory = NearbyGameViewModel.factory(
            matchesDao= db.matchesDao(),
            participationsDao = db.participationsDao(),
            matchStatisticsDao = db.matchStatisticsDao(),
            playerStatisticsDao = db.playerStatisticsDao(),
            userDao = db.userDao(),
            localId = localEndpointName
        )
    )
    //CAMBIARE IL TIPO//

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            status = "Permessi negati, impossibile procedere"
        }
    }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(requiredPermissions)
    }

    val gameState by nvm.uiState.collectAsState()

    LaunchedEffect(gameState.grid, connectedEndpointId, received) {
        if (isHost && connectedEndpointId != null && !sent && gameState.grid.isNotEmpty()) {
            Log.d("launch","mando grid e uncover")
            sendMessage("grid",gameState.grid.serializeList(), connectionsClient, connectedEndpointId!!)
            sendMessage("uncover",gameState.uncoverDeck.serializeList(), connectionsClient, connectedEndpointId!!)
            sent = true
        }

        if (!isHost && connectedEndpointId != null && received){
            nvm.updateGrid(griglia, uncoverDeck = uncoverDeck)
            received = false
        }
    }

    val payloadCallback = remember {

        object : PayloadCallback() {

            override fun onPayloadReceived(endpointId: String, payload: Payload) {

                val raw = String(payload.asBytes()!!, Charsets.UTF_8)
                val messageType = raw.substringBefore(":")  + ":"
                val message = raw.substringAfter(":")

                Log.d("PayloadReceived","$messageType:$message")

                when(messageType){
                    "endpoint:" -> nvm.updateRemote(message)
                    "grid:" -> {
                            griglia = deserializeCardList(message)
                        }
                    "uncover:" -> {
                        uncoverDeck = deserializeCardList(message)
                        received = true
                    }
                    "gameaction:" -> {
                        nvm.produceAction(message)
                    }
                    else -> status = "Ricevuto $message"

                }
                Log.d("Payload",message)
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
                when(update.status){
                    PayloadTransferUpdate.Status.SUCCESS -> Log.d("Payload","inviato")
                    PayloadTransferUpdate.Status.FAILURE -> Log.d("Payload", "fallito")
                    PayloadTransferUpdate.Status.IN_PROGRESS -> Log.d("Payload","traserimento...")
                    else -> return
                }

            }
        }
    }

    val connectionCallback = remember {
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                status = "Connessione in arrivo da ${info.endpointName}..."
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                sendMessage("endpoint",localEndpointName,connectionsClient,endpointId)
                nvm.updateConnectionsInfo(connectionsClient,endpointId)
            }
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.isSuccess) {
                    connectedEndpointId = endpointId
                    isSearching = false
                    status = "Connesso!"
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                } else {
                    status = "Connessione fallita"
                    isSearching = false
                }
            }
            override fun onDisconnected(endpointId: String) {
                connectedEndpointId = null
                status = "Disconnesso"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { connectionsClient.stopAllEndpoints() }
    }

    if (gameState.grid.isEmpty() || (connectedEndpointId == null && !sent && isHost) || (connectedEndpointId == null && !received && !isHost)){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stato connessione
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (connectedEndpointId != null) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (connectedEndpointId != null) "Connesso" else "Non connesso")
            }

            if (isSearching) {
                CircularProgressIndicator()
            }

            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                //cerco un guest
                Button(
                    onClick = {
                        isSearching = true
                        status = "In attesa di connessioni..."
                        isHost = true
                        val options = AdvertisingOptions.Builder()
                            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
                        connectionsClient.startAdvertising(
                            localEndpointName, SERVICE_ID, connectionCallback, options
                        )
                    },
                    enabled = connectedEndpointId == null && !isSearching
                ) { Text("Crea una partita") }
                //cerco un'host
                Button(
                    onClick = {
                        isSearching = true
                        status = "Cerco un host..."
                        nvm.blankGrid()
                        val options = DiscoveryOptions.Builder()
                            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
                        connectionsClient.startDiscovery(
                            SERVICE_ID,
                            object : EndpointDiscoveryCallback() {
                                override fun onEndpointFound(
                                    endpointId: String,
                                    info: DiscoveredEndpointInfo
                                ) {
                                    status = "Host trovato! Connessione..."
                                    connectionsClient.requestConnection(
                                        localEndpointName, endpointId, connectionCallback
                                    )
                                    connectionsClient.stopDiscovery()
                                }
                                override fun onEndpointLost(endpointId: String) {
                                    status = "Host perso, riprovo..."
                                }
                            },
                            options
                        )
                    },
                    enabled = connectedEndpointId == null && !isSearching
                ) { Text("unisciti ad una partita esistente") }
            }
            //nessuna connessione
            if (connectedEndpointId != null) {
                Button(
                    onClick = {
                        connectionsClient.stopAllEndpoints()
                        connectedEndpointId = null
                        status = "Disconnesso"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Disconnetti") }
            }
            //cercando connessione
            if (isSearching) {
                TextButton(
                    onClick = {
                        connectionsClient.stopAdvertising()
                        connectionsClient.stopDiscovery()
                        isSearching = false
                        status = "Ricerca annullata"
                    }
                ) { Text("Annulla") }
            }

            //gliglia non ancora aggiornata //DEBUG
            if (!griglia.isEmpty()){
                //DEBUG
                Text(griglia.serializeList())
                val c1 = gameState.grid.isEmpty()
                val c2 = (connectedEndpointId == null)
                val c3 = !isHost && connectedEndpointId != null && received
                val c4 = isHost && connectedEndpointId != null && !sent && gameState.grid.isNotEmpty()
                Text("\ncondizione 1:$c1,\nendpoint Null:$c2,\nreceived:$received, \nisHost:$isHost \nguardia launch:$c3\nguardia sbagliata: $c4")
            }
        }
    }
    else{
        SemelionScreen(padding= PaddingValues(4.dp), viewModel = nvm)
    }



}

fun sendMessage(messageType:String,message:String, clientConnectionsClient: ConnectionsClient,endpoint:String){
    val formattedMessage = "$messageType:$message"
    val payload = Payload.fromBytes(formattedMessage.toByteArray(Charsets.UTF_8))
    clientConnectionsClient.sendPayload(endpoint,payload)
    Log.d("Payload","Message: $message sent")
}
@Composable
fun LobbyScreen(modifier: Modifier = Modifier) {

}