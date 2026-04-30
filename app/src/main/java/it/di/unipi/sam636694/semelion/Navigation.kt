package it.di.unipi.sam636694.semelion

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.utilities.NavigationUIApp
import kotlin.collections.listOf
import kotlin.collections.mapOf
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import it.di.unipi.sam636694.semelion.nearbyConnections.NearbyScreen
import it.di.unipi.sam636694.semelion.nearbyConnections.SemelionConnectionsScreen

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SemelionNavigation(snackBarHostState: SnackbarHostState, db: SemelionDB){

    val backStack = rememberNavBackStack(Route.Home)


    NavDisplay(
        modifier = Modifier,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        backStack = backStack,
        entryProvider = { key ->
            when(key){
                is Route.Home ->
                    NavEntry(key){
                        SemelionHomeScreen(
                            destinations =  mapOf(
                                "Quick Play" to {backStack.add(Route.ScreenSharingGame(123L))},
                                "Connections" to {backStack.add(Route.SemelionConnections)}
                                )
                        )
                    }

                is Route.ScreenSharingGame -> NavEntry(key){
                    val viewModel: SemelionGameViewModel = viewModel(
                        factory = SemelionGameViewModel.factory(
                            matchesDao= db.matchesDao(),
                            participationsDao = db.participationsDao(),
                            matchStatisticsDao = db.matchStatisticsDao(),
                            playerStatisticsDao = db.playerStatisticsDao(),
                            userDao = db.userDao()
                        )
                    )

                    val uiState by viewModel.uiState.collectAsState()

                    if (uiState.phase is GamePhase.Loading) {
                        Text(text = "Loading..")
                    } else {
                        NavigationUIApp(snackBarHostState = snackBarHostState, db = db, viewModel)
                    }

                }

                is Route.SemelionConnections -> NavEntry(key) {
                    SemelionConnectionsScreen(backStack = backStack)
                }

                is Route.NearbyGame -> NavEntry(key){
                    val nearbyViewModel : SemelionGameViewModel = viewModel(
                        factory = SemelionGameViewModel.factory(
                            matchesDao= db.matchesDao(),
                            participationsDao = db.participationsDao(),
                            matchStatisticsDao = db.matchStatisticsDao(),
                            playerStatisticsDao = db.playerStatisticsDao(),
                            userDao = db.userDao()
                        )
                    )
                    NearbyScreen(hostId = key.hostId, guestId =key.guestId, connectionsClient = key.connectionsClient)
                }
                else ->error("Unknown NavKey:$key")
            }

        }

    )
}
@Composable
fun SemelionHomeScreen(
    modifier: Modifier = Modifier,
    destinations: Map<String, ()-> Unit>,
){

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        items(destinations.toList()){ (key,value) ->
            Button(
                onClick = value
            ) {
                Text(
                    text = key,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }

}