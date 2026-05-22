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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import kotlin.String

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemelionNavigation(snackBarHostState: SnackbarHostState, db: SemelionDB, player: AudioPlayer, userID:String){

    val backStack = rememberNavBackStack(Route.Home)
    var user by remember {  mutableStateOf<PlayerStatistics?>(null)  }
    var username by remember { mutableStateOf("none") }
    var matches by remember { mutableStateOf(emptyList<Matches?>()) }
    var recentMatches by remember {  mutableStateOf(emptyList<RecentMatch>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userID, backStack.size) {
        user = db.playerStatisticsDao().getStatsByUser(userID)
        username = db.userDao().getUserById(userID)?.nickName ?: "not in db"
        Log.d("DBMS","username:$username, user: ${db.userDao().getUserById(userID)}")
        if (username == "not in db") db.userDao().insert(User(userId = userID, nickName = "Semelion User: $userID"))
        username = db.userDao().getUserById(userID)?.nickName ?: "not in db"
        Log.d("nick","preComp:$username")
        matches = db.matchesDao().getMatchesByUser(userID)
        recentMatches = matches.mapNotNull { match ->
            val matchStats = db.matchesDao().getMatchStats(match?.matchId ?: return@mapNotNull null)
            val opponentMatch = matchStats.firstOrNull { it.userId != userID } ?: return@mapNotNull null
            val date = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(Date(opponentMatch.date))
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(opponentMatch.date))
            val opponentName = db.userDao().getUserById(opponentMatch.userId)?.nickName

            RecentMatch(
                opponent = opponentName ?: opponentMatch.userId,
                date = date,
                time = time,
                isWin = opponentMatch.winner
            )
        }
    }

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
                    val profileData =
                        if (user == null)
                            UserData(userID,0f,0,0,0, losses = 0, draws = 0, wins = 0)
                        else
                            UserData(
                                username = username,
                                winRate=user!!.matchesWon.toFloat()/user!!.matchesPlayed.toFloat() * 100,
                                gamesPlayed=user!!.matchesPlayed,
                                winStreak=user!!.currentStreak,
                                bestWinStreak=user!!.bestStreak,
                                wins = user!!.matchesWon,
                                losses = user!!.matchesLost,
                                draws = user!!.matchesDrawn
                            )
                    Log.d("DBMS","preProfile: $username\n $profileData")
                    ProfilePage(
                        profile = profileData.copy(username = username),
                        matches = recentMatches,
                        onEditProfile = { nickname:String ->
                            scope.launch {
                                db.userDao().update(User(userId = userID, nickName = nickname))
                                //forza la recomposition
                                username = nickname
                                Log.d("nick","nicck:$nickname")
                            }
                        },
                        onViewAllMatches = {}
                    )
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