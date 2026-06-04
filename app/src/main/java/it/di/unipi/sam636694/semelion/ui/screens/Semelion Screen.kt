package it.di.unipi.sam636694.semelion.ui.screens
import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.di.unipi.sam636694.semelion.utilities.CardSize
import it.di.unipi.sam636694.semelion.utilities.GreenAccent
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.utilities.TextSecondary
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.viewModels.gameModels.BaseGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.NearbyGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.SemelionGameViewModel
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import it.di.unipi.sam636694.semelion.utilities.Pergamena

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemelionScreen(
    viewModel: BaseGameViewModel = viewModel(),
    onBack : () -> Unit
){

    val state by viewModel.uiState.collectAsState()
    val dbOperationCompleted by viewModel.isDBOperationComplete.collectAsState()
    val goBack by viewModel.wantsToGoBack.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }
    //configurazioni per i giocatori
    val conf = when(viewModel){
        is NearbyGameViewModel ->{
            val connState by viewModel.connectionState.collectAsState()
            if (connState.isHost) {
                Pair(Triple(state.p2ActionsUsed,state.p2Actions,state.p1Turn),
                    Triple(state.p1ActionsUsed,state.p1Actions,!state.p1Turn))
            }
            else{
                Pair(
                    Triple(state.p1ActionsUsed,state.p1Actions,!state.p1Turn),
                    Triple(state.p2ActionsUsed,state.p2Actions,state.p1Turn)
                )
            }
        }
        else ->{
            Pair(Triple(state.p2ActionsUsed,state.p2Actions,state.p1Turn),
                Triple(state.p1ActionsUsed,state.p1Actions,!state.p1Turn))
        }
    }

    LaunchedEffect(dbOperationCompleted, goBack) {
        if (dbOperationCompleted && showExitDialog){
            viewModel.destroy()
            onBack()
        }
    }

    BackHandler(enabled = !showExitDialog && dbOperationCompleted) {
        showExitDialog = true
    }

    GameScreen(viewModel = viewModel, state = state, conf = conf)

    //Dialog per chiedere all'utente se vuole davvero uscire dalla schermata
    if (showExitDialog){
        BasicAlertDialog(onDismissRequest = {showExitDialog = false}) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column{
                    Text(
                        text = "Vuoi terminare la partita oppure sospenderla e riprenderla più tardi?",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row{
                        //Terminazione partita
                        Button(onClick = {endMatch(viewModel, onBack = onBack)}, enabled = dbOperationCompleted) {
                            Text("Interrompi")
                        }
                        //Sospensione Partita
                        Button(onClick = {interruptMatch(viewModel)}, enabled = viewModel is SemelionGameViewModel && dbOperationCompleted) {
                            Text("Sospendi")
                        }
                    }
                }
            }
        }
    }

    //Dialog di gioco
    Dialogs(state = state, viewModel = viewModel, onBack = onBack)
}

fun endMatch(viewModel: BaseGameViewModel,onBack:() -> Unit){
    when(viewModel){
        is SemelionGameViewModel -> viewModel.matchEnd(GameModes.ScreenSharing, loser = viewModel.userID)
        is NearbyGameViewModel ->{
            Log.d("fines","uid:${viewModel.userID}\nssId:${viewModel.secondPlayerId}")
            viewModel.matchEnd(GameModes.NearBy, loser = viewModel.userID)
            viewModel.destroy()
            onBack()
        }
        else -> {}
    }
}

fun interruptMatch(viewModel: BaseGameViewModel){
    when(viewModel){
        is SemelionGameViewModel -> {
            viewModel.interruptMatch(GameModes.ScreenSharing)
        }
        is NearbyGameViewModel -> viewModel.matchEnd(GameModes.NearBy,viewModel.localId)
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dialogs(state: GameUIState, viewModel: BaseGameViewModel,onBack:()-> Unit) {
    when(state.phase){
        is GamePhase.GameOver -> {

            LaunchedEffect(Unit) {
                when(viewModel){
                    is NearbyGameViewModel -> viewModel.matchEnd(GameModes.NearBy)
                    is SemelionGameViewModel -> viewModel.matchEnd(GameModes.ScreenSharing)
                    else -> {}
                }
            }

            //victory fanfare ff7 a cappela
            Log.d("winner","winner:${state.winner}\nuuid:${viewModel.userID}")

            val gameoverText = resolveGameoverText(state.winner,viewModel)
            viewModel.playEndSound()

            BasicAlertDialog(onDismissRequest = {}) {
                Surface(shape = RoundedCornerShape(16.dp)) {
                    Column{
                        Text(
                            text = gameoverText,
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row{
                            if (viewModel is SemelionGameViewModel){
                                Button(onClick = {viewModel.setup()}) {
                                    Text(text = "Gioca ancora")
                                }
                            }
                            Button(
                                onClick = {

                                    if (viewModel is NearbyGameViewModel) {
                                        viewModel.matchEnd(mode= GameModes.NearBy)
                                        viewModel.disconnect()
                                    }
                                    onBack()
                                }
                            ) {
                                Text(text="chiudi")
                            }
                        }
                    }
                }
            }
        }
        is GamePhase.Disconnected ->{
            BasicAlertDialog(onDismissRequest = {}) {
                Surface(shape = RoundedCornerShape(16.dp)) {
                    Column {
                        Text(
                            text = "La connessione è stata persa, la partità verrà registrata come un pareggio",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (viewModel is NearbyGameViewModel) viewModel.disconnect()
                        onBack()

                        }
                    }
                }
            }
        is GamePhase.JackMadness -> {
            if (state.jackSwaps.size == 1) {
                BasicAlertDialog(onDismissRequest = {viewModel.processIntent(GameIntent.JackMadness(state.jackSwaps))}) {
                    Surface(shape = RoundedCornerShape(16.dp)) {
                        Text(
                            text = "il jack non farà scambi perchè la prima carta del mazzo scoperta era un asso",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            else{
                BasicAlertDialog(onDismissRequest = {viewModel.processIntent(GameIntent.JackMadness(state.jackSwaps))}) {
                    Surface(shape = RoundedCornerShape(16.dp)) {
                        Text(
                            text = "il jack farà questi scambi:${state.jackSwaps}, ovviamente le carte in quelle posizioni sono del suo stesso colore",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
        else -> {

        }
    }
}

fun resolveGameoverText(winner: String?, viewModel: BaseGameViewModel): String {
    val isMultiplayer = viewModel is SemelionGameViewModel
    return when (winner) {
        viewModel.userID -> if (isMultiplayer) "Vince il giocatore Blu" else "Complimenti hai vinto!!"
        viewModel.secondPlayerId -> if (isMultiplayer) "Vince il giocatore Rosso" else "Peccato, andrà meglio la prossima volta..."
        else -> "Wow, è stato uno scontro ad armi pari"
    }
}

@Composable
fun GameScreen(
    viewModel: BaseGameViewModel,
    state: GameUIState,
    conf:Pair<Triple<Int,Int, Boolean>,Triple<Int,Int, Boolean>>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pergamena)  // <-- qui
    ) {
        val configuration = LocalConfiguration.current

        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Landscape(
                    viewModel = viewModel,
                    state = state,
                    cardSize = CardSize.LARGE,
                    conf = conf
                )
            }

            Configuration.ORIENTATION_PORTRAIT -> {
                Portrait(conf = conf, viewModel = viewModel, state = state)
            }

            else -> {
            }
        }
    }
}

@Composable
fun Landscape(
    conf:Pair<Triple<Int,Int, Boolean>,Triple<Int,Int, Boolean>>,
    cardSize: CardSize = CardSize.LARGE,
    viewModel: BaseGameViewModel,
    state: GameUIState
){
    val colorsConf = produceConfigs(state,viewModel)
    val p1Color = colorsConf.first().third.first
    val p2Color = colorsConf.last().third.first
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        //avversario
        OpponentHeader(
            actionsUsed = conf.first.first,
            actionsTotal = conf.first.second,
            isWaiting = conf.first.third,
            playerColor = p1Color,
            playerName = viewModel.opponentName,
            avatar = viewModel.secondPlayerAvatar ?: R.drawable.avatar_12
        )

        FinalGrid(state = state, model = viewModel, cardSize = cardSize)

        //giocatore
        OpponentHeader(
            actionsUsed = conf.second.first,
            actionsTotal = conf.second.second,
            isWaiting = conf.second.third,
            playerColor = p2Color,
            playerName = viewModel.playerName,
            avatar = viewModel.firstPlayerAvatar ?: R.drawable.avatar_1
        )
    }
}

@Composable
fun Portrait(
    conf:Pair<Triple<Int,Int, Boolean>,Triple<Int,Int, Boolean>>,
    cardSize: CardSize = CardSize.SMALL,
    viewModel: BaseGameViewModel,
    state: GameUIState
) {
    val colorsConf = produceConfigs(state,viewModel)
    val p1Color = colorsConf.first().third.first
    val p2Color = colorsConf.last().third.first
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.size(64.dp))

        Log.d("uuid","uuid:${viewModel.userID}\nopponentId:${viewModel.secondPlayerId}")
        //avversario
        OpponentHeader(
            actionsUsed = conf.first.first,
            actionsTotal = conf.first.second,
            isWaiting = conf.first.third,
            playerColor= p1Color,
            playerName = viewModel.opponentName,
            avatar = viewModel.secondPlayerAvatar ?: R.drawable.sora_avatar
        )

        Spacer(Modifier.size(32.dp))

        FinalGrid(state = state, model = viewModel, cardSize = cardSize)

        Spacer(Modifier.size(32.dp))

        //giocatore
        OpponentHeader(
            actionsUsed = conf.second.first,
            actionsTotal = conf.second.second,
            isWaiting = conf.second.third,
            playerColor=p2Color,
            playerName = viewModel.playerName,
            avatar = viewModel.firstPlayerAvatar ?: R.drawable.avatar_1
        )
        Spacer(Modifier.size(64.dp))
    }
}

@Composable
fun OpponentHeader(
    actionsUsed: Int,
    actionsTotal: Int,
    isWaiting: Boolean,
    playerColor:Color,
    playerName: String,
    avatar: Int
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (!isWaiting) Color.White else Color(0xFFF5F5F5),
        border = if (!isWaiting) BorderStroke(
            width=2.dp,
            color=playerColor
        ) else null,
        modifier = Modifier.wrapContentWidth(),
        shadowElevation = if (!isWaiting) 4.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9E9E9E)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(avatar),
                    contentDescription = "Playter Avatar"
                )

            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.wrapContentWidth()) {
                if (playerName.length < 20)
                    Text(
                        playerName,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                else
                    Text(
                        playerName.chunked(10)[0],
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
            }

            Spacer(Modifier.width(30.dp))

            Column(horizontalAlignment = Alignment.End) {
                // Pallini azioni
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(actionsTotal) { i ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < actionsTotal - actionsUsed) GreenAccent else Color(
                                        0xFFCCCCCC
                                    )
                                )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (isWaiting) {
                    Text("WAITING FOR YOU", fontSize = 10.sp, color = TextSecondary)
                } else {
                    Text(
                        "PLAYING",
                        fontSize = 10.sp,
                        color = GreenAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}