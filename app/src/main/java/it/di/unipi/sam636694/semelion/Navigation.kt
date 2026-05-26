package it.di.unipi.sam636694.semelion

import SemelionConnectionsScreen
import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import it.di.unipi.sam636694.semelion.database.Matches
import it.di.unipi.sam636694.semelion.database.PlayerStatistics
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.database.User
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.utilities.NavigationUIApp
import kotlin.collections.listOf
import kotlin.collections.mapOf
import it.di.unipi.sam636694.semelion.viewModels.gameModels.SemelionGameViewModel
import it.di.unipi.sam636694.semelion.ui.screens.ProfilePage
import it.di.unipi.sam636694.semelion.ui.screens.RecentMatch
import it.di.unipi.sam636694.semelion.ui.screens.SemelionHome
import it.di.unipi.sam636694.semelion.ui.screens.SemelionRules
import it.di.unipi.sam636694.semelion.ui.screens.UserData
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import it.di.unipi.sam636694.semelion.viewModels.UserProfileViewModel
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import kotlin.String

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemelionNavigation(snackBarHostState: SnackbarHostState, db: SemelionDB, player: AudioPlayer, userID:String){

    val backStack = rememberNavBackStack(Route.Home)
    val scope = rememberCoroutineScope()

    NavDisplay(
        modifier = Modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        entryProvider = { key ->
            when(key){
                is Route.Home ->
                    NavEntry(key){
                        SemelionHome(
                            destinations =  mapOf(
                                "Quick Play" to {backStack.add(Route.ScreenSharingGame)},
                                "Connections" to {backStack.add(Route.SemelionConnections)},
                            ),
                                navigationFun= {route -> backStack.add(route)}
                        )
                    }

                is Route.RulesPage -> NavEntry(key){
                    SemelionRules()
                }

                is Route.ProfilePage -> NavEntry(key){

                    val profileVm: UserProfileViewModel = viewModel {
                        UserProfileViewModel(db = db, userId = userID)
                    }

                    val uiState by profileVm.uiState.collectAsState()

                    if (uiState.isDataLoading){
                        Text(text = "Loading...")
                    }else{
                        ProfilePage(
                            profile = uiState.data,
                            matches = uiState.matches,
                            onEditProfile = { nickname:String ->
                                scope.launch {
                                    profileVm.onEditNickname(nickname)
                                }
                            },
                            onAvatarChosen = { avatar:Int ->
                                scope.launch {
                                    profileVm.onEditAvatar(avatar)
                                }
                            },
                            onViewAllMatches = {}
                        )
                    }

                }

                is Route.ScreenSharingGame -> NavEntry(key){
                    val viewModel: SemelionGameViewModel = viewModel(
                        factory = SemelionGameViewModel.factory(
                            matchesDao= db.matchesDao(),
                            participationsDao = db.participationsDao(),
                            matchStatisticsDao = db.matchStatisticsDao(),
                            playerStatisticsDao = db.playerStatisticsDao(),
                            userDao = db.userDao(),
                            player= player,
                            userID=userID,
                            secondPlayerId = "screenSharing"
                        )
                    )

                    val uiState by viewModel.uiState.collectAsState()

                    if (uiState.phase is GamePhase.Loading) {
                        Text(text = "Loading..")
                        if (viewModel.suspendedFound){
                            BasicAlertDialog(onDismissRequest = {}) {
                                Column() {
                                    Text(text = "È stata trovata una partita sospesa, vui riprenderla")
                                    Row() {
                                        Button(onClick = {viewModel.resumeMatch()}) {
                                            Text(text = "Riprendi partita")
                                        }
                                        Button(onClick = {viewModel.newMatch()}) {
                                            Text(text = "No, iniziane una nuova")
                                        }
                                    }

                                }

                            }
                        }
                    } else {
                        NavigationUIApp(
                            snackBarHostState = snackBarHostState,
                            db = db,
                            viewModel,
                            onNavigateBack = { backStack.removeLastOrNull()})
                    }

                }

                is Route.SemelionConnections -> NavEntry(key) {
                    SemelionConnectionsScreen(
                        db=db,
                        snackBarHostState,
                        player=player,
                        userId = userID,
                        onBack = { backStack.removeLastOrNull()},
                    )
                }

                else ->error("Unknown NavKey:$key")
            }

        }

    )
}