import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.*
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.gameModels.NearbyGameViewModel
import it.di.unipi.sam636694.semelion.serializeList
import androidx.compose.runtime.collectAsState
import androidx.navigation3.runtime.NavKey
import it.di.unipi.sam636694.semelion.AudioPlayer
import it.di.unipi.sam636694.semelion.Route
import it.di.unipi.sam636694.semelion.ui.states.ConnectionUiState
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import it.di.unipi.sam636694.semelion.utilities.AppDestinations
import it.di.unipi.sam636694.semelion.utilities.NavigationUIApp

@Composable
fun SemelionConnectionsScreen(
    db: SemelionDB,
    snackbarHostState: SnackbarHostState,
    player: AudioPlayer,
    userId: String,
    onBack: () -> Unit,
) {

    Log.d("nav","entro")

    val SERVICE_ID = "com.tuaapp.semelion"

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()
    }

    val nvm: NearbyGameViewModel = viewModel(
        factory = NearbyGameViewModel.factory(
            matchesDao= db.matchesDao(),
            participationsDao = db.participationsDao(),
            matchStatisticsDao = db.matchStatisticsDao(),
            playerStatisticsDao = db.playerStatisticsDao(),
            userDao = db.userDao(),
            player = player,
            localId = userId,
            context = LocalContext.current
        )
    )

    val connectionState by nvm.connectionState.collectAsState()
    val gameState by nvm.uiState.collectAsState()

    //CAMBIARE IL TIPO//
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            throw Exception("permissions Denied(Dioboia)")
        }
    }

    //richiesta permessi
    LaunchedEffect(Unit) {
        permissionsLauncher.launch(requiredPermissions)
    }
    Log.d("conn","${connectionState.gameStarted}")
    if (
        !connectionState.gameStarted &&(
        gameState.grid.isEmpty() ||
        (connectionState.connectedEndpointId == null  && connectionState.isHost) ||
        (!connectionState.received && !connectionState.isHost)
        )){
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
                            color = if (connectionState.connectedEndpointId != null) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (connectionState.connectedEndpointId != null) "Connesso" else "Non connesso")
            }

            if (connectionState.isSearching) {
                CircularProgressIndicator()
            }

            Text(
                text = connectionState.status,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Button(
                    onClick = { nvm.startHosting(SERVICE_ID) },
                    enabled = connectionState.connectedEndpointId == null && !connectionState.isSearching
                ) { Text("Crea una partita") }

                Button(
                    onClick = { nvm.startDiscovery(SERVICE_ID) },
                    enabled = connectionState.connectedEndpointId == null && !connectionState.isSearching
                ) { Text("Unisciti ad una partita") }

                Button(
                    onClick = { nvm.disconnect() },
                    enabled = connectionState.connectedEndpointId != null
                ) { Text("Disconnetti") }

                Button(
                    onClick = { nvm.cancelSearch() },
                    enabled = connectionState.isSearching
                ) { Text("Annulla") }

            }
        }
    }
    else {
        NavigationUIApp(snackBarHostState = snackbarHostState, db = db, nvm,player=player, onNavigateBack = onBack)
    }
}

fun sendMessage(messageType:String,message:String, clientConnectionsClient: ConnectionsClient,endpoint:String){
    val formattedMessage = "$messageType:$message"
    val payload = Payload.fromBytes(formattedMessage.toByteArray(Charsets.UTF_8))
    clientConnectionsClient.sendPayload(endpoint,payload)
    Log.d("Payload","Message: $formattedMessage sent")
}

@Composable
fun OrrebbondoSchermoDiDebug(gameState: GameUIState,connectionState: ConnectionUiState){
    if (!gameState.grid.isEmpty()) {
        //DEBUG
        Text(gameState.grid.serializeList())
        val c1 = gameState.grid.isEmpty()
        val c2 = (connectionState.connectedEndpointId == null)
        val c3 = !connectionState.isHost && connectionState.connectedEndpointId != null && connectionState.received
        val c4 =
            connectionState.isHost && connectionState.connectedEndpointId != null && !connectionState.sent && gameState.grid.isNotEmpty()

        val c5 = gameState.grid.isEmpty()
        val c6 =(connectionState.connectedEndpointId == null && !connectionState.received && !connectionState.isHost)
        val c7 = connectionState.connectedEndpointId == null
        val c8 = !connectionState.received
        Column{
            Text("\ncondizione 1:$c1,\nendpoint Null:$c2,\nreceived:${connectionState.received}, \nisHost:${connectionState.isHost} \nguardia launch:$c3\nguardia sbagliata: $c4")
            Text("isHost:${connectionState.isHost}")
            Text("c1:$c5,c2:$c6,c3:$c7,c4:$c8")
        }

    }
}
