package it.di.unipi.sam636694.semelion.nearbyConnections

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
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
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import it.di.unipi.sam636694.semelion.Route

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SemelionConnectionsScreen(backStack: NavBackStack<NavKey>) {
    val context = LocalContext.current
    val connectionsClient = remember { Nearby.getConnectionsClient(context) }
    val SERVICE_ID = "com.tuaapp.semelion"

    var connectedEndpointId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Scegli un ruolo per iniziare") }
    var isSearching by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            status = "Permessi negati, impossibile procedere"
        }
    }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        )
    }

    val payloadCallback = remember {
        object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                val message = String(payload.asBytes()!!)
                status = "Ricevuto: $message"
            }
            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {

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

        // Spinner mentre cerca
        if (isSearching) {
            CircularProgressIndicator()
        }

        if (status == "Connesso!"){
            backStack.add(
                Route.NearbyGame(
                    hostId=123L,
                    guestId=124L,
                    connectionsClient= connectionsClient,
                )
            )
        }

        // Messaggio di stato
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bottoni Host / Guest
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    isSearching = true
                    status = "In attesa di connessioni..."
                    val options = AdvertisingOptions.Builder()
                        .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
                    connectionsClient.startAdvertising(
                        "Host", SERVICE_ID, connectionCallback, options
                    )
                },
                enabled = connectedEndpointId == null && !isSearching
            ) { Text("Host") }

            Button(
                onClick = {
                    isSearching = true
                    status = "Cerco un host..."
                    val options = DiscoveryOptions.Builder()
                        .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
                    connectionsClient.startDiscovery(
                        SERVICE_ID,
                        object : EndpointDiscoveryCallback() {
                            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
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
            ) { Text("Guest") }
        }

        // Bottone disconnetti
        if (connectedEndpointId != null) {
            Button(
                onClick = {
                    connectionsClient.stopAllEndpoints()
                    connectedEndpointId = null
                    status = "Disconnesso"
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Disconnetti") }
        }

        // Bottone annulla ricerca
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
    }
}

fun sendMessage(connectionsClient: ConnectionsClient, endpointId: String, message: String) {
    val payload = Payload.fromBytes(message.toByteArray())
    connectionsClient.sendPayload(endpointId, payload)
}