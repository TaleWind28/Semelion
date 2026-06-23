package it.di.unipi.sam636694.semelion.appNavigation

import it.di.unipi.sam636694.semelion.ui.screens.SemelionConnectionsScreen
import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.ui.screens.MatchStatScreen
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import kotlin.collections.listOf
import kotlin.collections.mapOf
import it.di.unipi.sam636694.semelion.viewModels.gameModels.SemelionGameViewModel
import it.di.unipi.sam636694.semelion.ui.screens.ProfilePage
import it.di.unipi.sam636694.semelion.ui.screens.SemelionHome
import it.di.unipi.sam636694.semelion.ui.screens.SemelionRules
import it.di.unipi.sam636694.semelion.ui.screens.WelcomeBottomSheet
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import it.di.unipi.sam636694.semelion.viewModels.gameModels.NearbyGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.utilityModels.LogViewModel
import it.di.unipi.sam636694.semelion.viewModels.utilityModels.MatchViewModel
import it.di.unipi.sam636694.semelion.viewModels.utilityModels.UserProfileViewModel
import kotlinx.coroutines.launch
import kotlin.String

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemelionNavigation(snackBarHostState: SnackbarHostState, db: SemelionDB, player: AudioPlayer, userID:String,firstLaunch:()->Boolean,updateFirstLaunch:()->Unit){
    //backstack per la navigazione
    val backStack = rememberNavBackStack(Route.Home)
    val scope = rememberCoroutineScope()

    val appContext = LocalContext.current.applicationContext as Application
    //creo lvm per poterlo usare in tutte le partite
    val lvm: LogViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                LogViewModel(appContext)
            }
        }
    )
    // aggiunta per correzione bug dopo consegna: -> mvm che consente di visualizzare le stat dei giocartori a fine partita
    val mvm: MatchViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                MatchViewModel(appContext)
            }
        }
    )


    //creo un NavDisplay per consentire la navigazione
    //Mix del tutorial di Philippe Lackner su youtube e android Developer anche se prevalentemente segue il tutorial
    //https://www.youtube.com/watch?v=jhrfx8Uk_y0&t=294s
    NavDisplay(
        modifier = Modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        entryProvider = { key ->
            //in base alla Route sceglo la schermata da mostrare
            when(key){
                is Route.Home -> NavEntry(key){
                        val destinations =  mapOf(
                            "Quick Play" to {backStack.add(Route.ScreenSharingGame);Unit},
                            "Connections" to {backStack.add(Route.SemelionConnections);Unit},
                            "Rules" to {backStack.add(Route.RulesPage);Unit},
                            "Profile" to {backStack.add(Route.ProfilePage);Unit},
                            "Home" to {backStack.add(Route.Home);Unit}
                        )
                        SemelionHome(
                            destinations = destinations,
                        )
                    //BottomSheet di benvenuto per dare una prima direzione ai nuovi utenti, la variabile associata è gestita dalle preferenze
                        var showWelcomeSheet by remember{ mutableStateOf(firstLaunch()) }

                        if (showWelcomeSheet) {
                            WelcomeBottomSheet(
                                onDismiss ={showWelcomeSheet=false;updateFirstLaunch()},
                                onGoToRules = destinations["Rules"] ?: {},
                                onQuickPlay = destinations["Quick Play"] ?: {}
                            )
                        }
                    }

                is Route.RulesPage -> NavEntry(key){
                    SemelionRules()
                }

                is Route.ProfilePage -> NavEntry(key){
                    //creo il viewModel per mostrare i dati del giocatore
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
                            }
                        )
                    }

                }

                is Route.ScreenSharingGame -> NavEntry(key){
                    //creo il viewModel per giocare in ScreenSharing
                    val viewModel: SemelionGameViewModel = viewModel(
                        factory = SemelionGameViewModel.factory(
                            matchesDao= db.matchesDao(),
                            participationsDao = db.participationsDao(),
                            matchStatisticsDao = db.matchStatisticsDao(),
                            playerStatisticsDao = db.playerStatisticsDao(),
                            userDao = db.userDao(),
                            player= player,
                            userID=userID,
                            secondPlayerId = "screenSharing",
                            application = appContext
                        )
                    )

                    val uiState by viewModel.uiState.collectAsState()

                    if (uiState.phase is GamePhase.Loading) {
                        //Mostro una partita sospesa
                        Text(text = "Loading..")
                        if (viewModel.suspendedFound){
                            BasicAlertDialog(onDismissRequest = {}) {
                                Column{
                                    Text(text = "È stata trovata una partita sospesa, vui riprenderla")
                                    Row{
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
                        //vado in navigationUIApp per visualizzare la schermata di gioco con anche lo schermo delle regole e dei log
                        NavigationUIApp(
                            snackBarHostState = snackBarHostState,
                            db = db,
                            viewModel=viewModel,
                            logViewModel = lvm,
                            onNavigateBack = {
                                viewModel.playEndSound()
                                mvm.retrieveEndGameStatsInfo(viewModel)
                                compactNavigation(
                                    backStack,
                                    Route.MatchStatScreen(Route.ScreenSharingGame)
                                )
                            }
                        )
                    }

                }

                is Route.SemelionConnections -> NavEntry(key) {

                    //creo il viewmodel per gestire la connessione e la partita
                    val nvm: NearbyGameViewModel = viewModel(
                        factory = NearbyGameViewModel.factory(
                            matchesDao = db.matchesDao(),
                            participationsDao = db.participationsDao(),
                            matchStatisticsDao = db.matchStatisticsDao(),
                            playerStatisticsDao = db.playerStatisticsDao(),
                            userDao = db.userDao(),
                            player = player,
                            localId = userID,
                            nickname = "Semelion User:$userID",
                            application = LocalContext.current.applicationContext as Application,
                        )
                    )
                    //schermo per connettersi
                    SemelionConnectionsScreen(
                        userId = userID,
                        db=db,
                        nvm = nvm,
                        onGameReady = {
                            //navigo verso la schermata di gioco
                            backStack.add(Route.SemelionNearbyGame(nvm))
                        }
                    )

                }

                is Route.SemelionNearbyGame -> NavEntry(key) {
                    //partita di Semelion in NearbyConnections
                    NavigationUIApp(
                        snackBarHostState = snackBarHostState,
                        db = db,
                        viewModel = key.viewModel,
                        logViewModel = lvm,
                        onNavigateBack = {
                            key.viewModel.player.stop()
                            key.viewModel.playEndSound()
                            mvm.retrieveEndGameStatsInfo(key.viewModel)
                            compactNavigation(
                                backStack,
                                Route.MatchStatScreen(Route.SemelionConnections)
                            )
                        }
                    )
                }

                is Route.MatchStatScreen -> NavEntry(key) {
                    //bug nella consegna, lo schermo non sopravviveva alla rotazione dello schermoù
                    // -> Ho risolto aggiungendo un viewModel dedicato e togliendo di conseguenza il viewmodel nella route
                    // Rendendo così Serializable la rotta, inoltre ho spostato la creazione dei "display" per le stat dei giocatori nel vm stesso
                    val matchStatState = mvm.uiState.collectAsState().value
                    if (matchStatState.p1Stats == null || matchStatState.p2Stats == null) return@NavEntry
                    //schermo di fine partita con le statistiche dei giocatori
                    val winner = if (matchStatState.p1Wins) matchStatState.p1Stats else matchStatState.p2Stats
                    val loser = if (!matchStatState.p1Wins) matchStatState.p1Stats else matchStatState.p2Stats
                    MatchStatScreen(
                        winnerStats = winner,
                        loserStats = loser,
                        onHome = {
                            while (backStack.lastOrNull() != Route.Home) {
                                backStack.removeLastOrNull()
                            }
                        },
                        onNewGame = {
                            compactNavigation(backStack,key.backRoute)
                        }
                    )
                }

                else ->error("Unknown NavKey:$key")
            }



        }
    )
}


fun compactNavigation(backStack: NavBackStack<NavKey>,route: Route){
    while (backStack.lastOrNull() != Route.Home){
        backStack.removeLastOrNull()
    }
    backStack.add(route)
}

