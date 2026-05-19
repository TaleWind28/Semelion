package it.di.unipi.sam636694.semelion

import SemelionConnectionsScreen
import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import it.di.unipi.sam636694.semelion.database.MatchStatistics
import it.di.unipi.sam636694.semelion.database.Matches
import it.di.unipi.sam636694.semelion.database.PlayerStatistics
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.utilities.NavigationUIApp
import kotlin.collections.listOf
import kotlin.collections.mapOf
import it.di.unipi.sam636694.semelion.gameModels.SemelionGameViewModel
import it.di.unipi.sam636694.semelion.ui.screens.ProfilePage
import it.di.unipi.sam636694.semelion.ui.screens.RecentMatch
import it.di.unipi.sam636694.semelion.ui.screens.SemelionHome
import it.di.unipi.sam636694.semelion.ui.screens.UserData
import it.di.unipi.sam636694.semelion.ui.screens.mockMatches
import java.util.Date
import java.util.Locale

@Composable
fun SemelionNavigation(snackBarHostState: SnackbarHostState, db: SemelionDB, player: AudioPlayer,userID:String){

    val backStack = rememberNavBackStack(Route.Home)


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
                    var user by remember {  mutableStateOf<PlayerStatistics?>(null)  }
                    var username by remember { mutableStateOf("none") }
                    var matches by remember { mutableStateOf(emptyList<Matches?>()) }
                    var recentMatches by remember {  mutableStateOf(emptyList<RecentMatch?>()) }
                    LaunchedEffect(userID) {
                        user = db.playerStatisticsDao().getStatsByUser(userID)
                        username = db.userDao().getUserById(userID)?.nickName ?: "none"
                        matches = db.matchesDao().getMatchesByUser(userID)
                        recentMatches = matches.map { match ->
                            val matchStats =  db.matchesDao().getMatchStats(match?.matchId!!)
                            val opponentMatch = matchStats.firstOrNull{ it.userId != userID }
                            val date = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(
                                Date(opponentMatch?.date!!)
                            )
                            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(opponentMatch.date))

                            RecentMatch(
                                opponent = opponentMatch.userId,
                                date = date,
                                time = time,
                                isWin = opponentMatch.winner != opponentMatch.userId,
                                rankChange = 0
                            )
                        }
                        recentMatches.forEach { Log.d("DB","$it") }
                        //Log.d("DB","$recentMatches.")
                    }


                    val profileData = if (user == null) UserData(userID,0f,0,0,0, losses = 0, draws = 0, wins = 0)
                        else
                            UserData(
                                username = username,user!!.matchesWon.toFloat()/user!!.matchesPlayed.toFloat() * 100,
                                gamesPlayed=user!!.matchesPlayed,
                                winStreak=user!!.currentStreak,
                                bestWinStreak=user!!.bestStreak,
                                wins = user!!.matchesWon,
                                losses = user!!.matchesLost,
                                draws = if (user!!.matchesWon != 0 && user!!.matchesLost != 0)
                                    user!!.matchesWon- user!!.matchesLost
                                else 0
                            )
                    ProfilePage(
                        profile = profileData,
                        matches = recentMatches as List<RecentMatch>,
                        onEditProfile = {},
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
@Composable
fun SemelionHomeScreen(
    modifier: Modifier = Modifier,
    destinations: Map<String, ()-> Unit>,
    navigationFun: (route: NavKey) -> Unit
){

    val buttonList = listOf(
        IconButton(
            "Regole",
            "Consulta le regole di Semelion",
            "icona di regole",
            R.drawable.article_24px,
            goTo = {navigationFun(Route.RulesPage)}
        ),
        IconButton(
            "Profile",
            "Profile Page",
            "icona del profilo",
            R.drawable.account_circle_24px,
            goTo = {navigationFun(Route.ProfilePage)}
        )

    )

    Column(){
        GreetingsBox(destinations = destinations)
        buttonList.forEach { button ->
            DestinationButton(button = button)
        }
    }
}

@Composable
fun GreetingsBox(modifier: Modifier = Modifier,destinations: Map<String, ()-> Unit>,) {
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

data class IconButton(
    val title: String,
    val subtitle:String,
    val contentDescription:String,
    val resId: Int,
    val goTo: () -> Unit
)
@Composable
fun DestinationButton(modifier: Modifier = Modifier,button:IconButton){
    Row{
        Button(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            onClick = {button.goTo()}
        ){
            Icon(painter = painterResource(button.resId), contentDescription = button.contentDescription)
            Column{
                Text(text = button.title)
                Text(text = button.subtitle)
            }
        }
    }
}

fun getMatchesData(matches:List<Matches?>): List<RecentMatch>{
    if (matches.isEmpty()) return listOf(RecentMatch(opponent = "null","null","null",false,0))
    return listOf(RecentMatch(opponent = "null","null","null",false,0))
}