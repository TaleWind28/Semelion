import android.Manifest
import android.os.Build
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

    var connectedEndpointId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Scegli un ruolo per iniziare") }
    var isSearching by remember { mutableStateOf(false) }
    var isHost by remember { mutableStateOf(false) }
    var griglia by remember { mutableStateOf<List<CardUIStates>>(emptyList()) }
    var sent by remember {mutableStateOf(false) }
    var nearbyGameViewModel by remember { mutableStateOf<NearbyGameViewModel?>(null) }


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

    val payloadCallback = remember {

        object : PayloadCallback() {

            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                val message = String(payload.asBytes()!!)
                Log.d("Payload",message)
                try {
                    griglia = deserializeCardList(message)
                    status = "Ricevuto: $message"
                }
                catch(e: Exception){
                    status = "Ricevuto: $message"
                }

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
            Button(
                onClick = {
                    isSearching = true
                    status = "In attesa di connessioni..."
                    isHost = true
                    val options = AdvertisingOptions.Builder()
                        .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
                    connectionsClient.startAdvertising(
                        "Host", SERVICE_ID, connectionCallback, options
                    )
                },
                enabled = connectedEndpointId == null && !isSearching
            ) { Text("Crea una partita") }

            Button(
                onClick = {
                    isSearching = true
                    status = "Cerco un host..."
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
                                    "Guest", endpointId, connectionCallback
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

        if(isHost && connectedEndpointId != null && !sent){
             val nvm: SemelionGameViewModel = viewModel(
                factory = SemelionGameViewModel.factory(
                    matchesDao= db.matchesDao(),
                    participationsDao = db.participationsDao(),
                    matchStatisticsDao = db.matchStatisticsDao(),
                    playerStatisticsDao = db.playerStatisticsDao(),
                    userDao = db.userDao()
                )
            )
            Log.d("prova send","pippo")
            val state = nvm.uiState.collectAsState()
            griglia = state.value.grid
            sendMessage(griglia.serializeList(),connectionsClient,connectedEndpointId!!)
            //nearbyGameViewModel = nvm
            sent = true
        }

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

        if (!griglia.isEmpty()){
            Text(griglia.serializeList())
        }
    }

}

fun sendMessage(message:String, clientConnectionsClient: ConnectionsClient,endpoint:String){
    val payload = Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
    clientConnectionsClient.sendPayload(endpoint,payload)
    Log.d("Payload","Message: $message sent")
}

@Composable
fun LobbyScreen(modifier: Modifier = Modifier) {

}