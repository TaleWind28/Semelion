package it.di.unipi.sam636694.semelion.ui.screens

import android.Manifest
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import it.di.unipi.sam636694.semelion.viewModels.gameModels.NearbyGameViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import it.di.unipi.sam636694.semelion.ui.states.ConnectionUiState
import it.di.unipi.sam636694.semelion.utilities.NavigationUIApp


@Composable
fun SemelionConnectionsScreen(
    db: SemelionDB,
    snackbarHostState: SnackbarHostState,
    player: AudioPlayer,
    userId: String,
    onBack: () -> Unit,
) {
    var nickname = "Semelion User:$userId"
    var avatar: Int

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

    //CAMBIARE IL TIPO//
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            throw Exception("permissions Denied")
        }
    }

    val nvm: NearbyGameViewModel = viewModel(
        factory = NearbyGameViewModel.factory(
            matchesDao = db.matchesDao(),
            participationsDao = db.participationsDao(),
            matchStatisticsDao = db.matchStatisticsDao(),
            playerStatisticsDao = db.playerStatisticsDao(),
            userDao = db.userDao(),
            player = player,
            localId = userId,
            nickname = nickname,
            application = LocalContext.current.applicationContext as Application,
        )
    )

    //richiesta permessi
    LaunchedEffect(nvm) {
        permissionsLauncher.launch(requiredPermissions)
        val user = db.userDao().getUserById(userId)
        if (user!=null){
            nickname = user.nickName
            avatar = user.avatar
            nvm.updateNickname(nickname)
            nvm.updateFirstPlayerAvatar(avatar)
        }

    }

    val connectionState by nvm.connectionState.collectAsState()
    val gameState by nvm.uiState.collectAsState()

    if ((!connectionState.gameStarted) &&
        (
            gameState.grid.isEmpty() ||
            (connectionState.connectedEndpointId == null  && connectionState.isHost) ||
            (!connectionState.received && !connectionState.isHost)
        )
    ) {
        DiscoveryScreen(nvm)
    } else {
        NavigationUIApp(snackBarHostState = snackbarHostState, db = db, viewModel=nvm, onNavigateBack = onBack)
    }
}

fun sendMessage(messageType:String,message:String, clientConnectionsClient: ConnectionsClient,endpoint:String){
    val formattedMessage = "$messageType:$message"
    val payload = Payload.fromBytes(formattedMessage.toByteArray(Charsets.UTF_8))
    clientConnectionsClient.sendPayload(endpoint,payload)
    Log.d("Payload","Message: $formattedMessage sent")
}

@Composable
fun DiscoveryScreen(
    viewModel: NearbyGameViewModel,
) {
    val serviceId = "semelion_nearbyConnections"
    val state by viewModel.connectionState.collectAsState()
    var isHosting by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F7EE))
    ) {
        // Toggle condiviso
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFDDEEDD))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Create Match" to true, "Join Match" to false).forEach { (label, tabIsHost) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isHosting == tabIsHost) Color(0xFF22CC22) else Color.Transparent)
                        .clickable { isHosting = tabIsHost }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label,
                        color = if (isHosting == tabIsHost) Color(0xFF0A3A0A) else Color(0xFF4A7A4A),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LaunchedEffect(isHosting) {
            viewModel.cancelSearch()
            Log.d("disconnect","isHosting:$isHosting")
            if (!isHosting) {
                viewModel.disconnect()
                viewModel.startDiscovery(serviceId)
            }
        }

        if (isHosting) {
            HostScreen(viewModel = viewModel, serviceId = serviceId )
        } else {
            GuestScreen(viewModel = viewModel,state=state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestScreen(state: ConnectionUiState,viewModel: NearbyGameViewModel){
    var joiningDialog by remember { mutableStateOf(false) }
    // Radar
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(160.dp).clip(CircleShape)
                .border(1.5.dp, Color(0xFFB8DCB8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(110.dp).clip(CircleShape)
                    .border(1.5.dp, Color(0xFFC8E8C8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(Color(0xFF22CC22)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0A3A0A), modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    // Titolo
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Searching for Rooms", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A5C1A))
        Spacer(Modifier.height(4.dp))
        Text("STAY WITHIN 10 METERS OF YOUR OPPONENT", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A7A4A), letterSpacing = 1.sp, textAlign = TextAlign.Center)
    }

    // Label sezione
    Text(
        "NEARBY PLAYERS FOUND",
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = Color(0xFF4A7A4A)
    )

    // Lista host
    LazyColumn(modifier = Modifier) {
        items(state.discoveredEndpoints) { endpoint ->
            PlayerCard(
                name = endpoint.endpointName,
                avatar = endpoint.avatarIndex,
                onJoin = {
                    joiningDialog = true
                    viewModel.connectToEndpoint(endpoint.endpointId)
                }
            )
        }

        // Info box
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(0.5.dp, Color(0xFFCCE8CC), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF4A7A4A), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Ensure Bluetooth and Location services are enabled for the most accurate nearby discovery.", fontSize = 12.sp, color = Color(0xFF4A7A4A), lineHeight = 18.sp)
            }
        }
    }

    if (joiningDialog)
        BasicAlertDialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column {
                    Text(
                        text = "Creazione della partita in corso...",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
}


@Composable
fun PlayerCard(name: String,avatar:Int, onJoin: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(0.5.dp, Color(0xFFCCE8CC), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFC8D8F8)),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(avatar), contentDescription = "Avatar")
            //Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2255AA), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Online", fontSize = 12.sp, color = Color(0xFF4A7A4A))
        }
        Button(
            onClick = onJoin,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A5C1A)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text("JOIN", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun HostScreen(
    viewModel: NearbyGameViewModel,
    serviceId: String,
) {
    val state by viewModel.connectionState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F7EE))
    ) {
        // Card principale
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFE8F5E8))
                .padding(32.dp, 32.dp, 32.dp, 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Radar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFFC8DCC8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(86.dp).clip(CircleShape)
                        .background(Color(0xFF22CC22)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null,
                        tint = Color(0xFF0A3A0A), modifier = Modifier.size(30.dp))
                }
            }

            Text("Searching for Rivals", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                if (state.isSearching) "In attesa di giocatori..." else "Premi per iniziare",
                fontSize = 14.sp, color = Color(0xFF5A7A5A)
            )

            Button(
                onClick = { viewModel.startHosting(serviceId, nickname = viewModel.nickname) },
                enabled = !state.isSearching,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A5C1A)),
                contentPadding = PaddingValues(vertical = 18.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("START SCANNING", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp)
            }
        }

        Log.d("conn","stato:$state")

        Spacer(Modifier.height(12.dp))

        // Info box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE8EEF8))
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Info, contentDescription = null,
                tint = Color(0xFF3A5A9A), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Semelion uses nearby connections to find opponents in your immediate vicinity.",
                fontSize = 13.sp, color = Color(0xFF3A5A7A), lineHeight = 20.sp
            )
        }

        Spacer(Modifier.weight(1f))
    }
}