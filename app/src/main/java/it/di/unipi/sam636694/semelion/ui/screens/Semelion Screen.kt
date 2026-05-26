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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import it.di.unipi.sam636694.semelion.utilities.cardImageMap
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.viewModels.gameModels.BaseGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.NearbyGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.SemelionGameViewModel
import it.di.unipi.sam636694.semelion.ui.states.FinalGrid
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemelionScreen(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    viewModel: BaseGameViewModel = viewModel(),
    onBack : () -> Unit
){

    val state by viewModel.uiState.collectAsState()
    val dbOperationCompleted by viewModel.isDBOperationComplete.collectAsState()
    val goBack by viewModel.wantsToGoBack.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dbOperationCompleted, goBack) {
        if (dbOperationCompleted && showExitDialog){
            viewModel.destroy()
            onBack()
        }
    }

    Log.d("message","${state.phase}")
    when(viewModel){
        is SemelionGameViewModel -> SinglePlayer(state = state, viewModel= viewModel, modifier = modifier, padding=padding)
        is NearbyGameViewModel -> MultiPlayer(state= state, viewModel= viewModel, modifier = modifier)
    }

    Log.d("finder","$dbOperationCompleted")

    BackHandler(enabled = !showExitDialog && dbOperationCompleted) {
        showExitDialog = true
    }

    if (showExitDialog){
        BasicAlertDialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column{
                    Text(
                        text = "Vuoi terminare la partita oppure sospenderla e riprenderla più tardi?",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row() {
                        //Interruzione partita
                        Button(onClick = {endMatch(viewModel, onBack = onBack)}, enabled = dbOperationCompleted) {
                            Text("Interrompi")
                        }
                        //Richiesta interruzione
                        Button(onClick = {interruptMatch(viewModel)}, enabled = viewModel is SemelionGameViewModel && dbOperationCompleted) {
                            Text("Sospendi")
                        }
                    }
                }
            }
        }
    }

    // Game over dialog
    Dialogs(state = state, viewModel = viewModel, onBack = onBack)
}

fun endMatch(viewModel: BaseGameViewModel,onBack:() -> Unit){
    when(viewModel){
        is SemelionGameViewModel -> viewModel.matchEnd(GameModes.ScreenSharing, loser = viewModel.userID)
        is NearbyGameViewModel ->{
            viewModel.matchEnd(GameModes.NearBy, loser = if (viewModel.connectionState.value.isHost) viewModel.userID else viewModel.secondPlayerId)
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
fun Dialogs(modifier: Modifier = Modifier,state: GameUIState, viewModel: BaseGameViewModel,onBack:()-> Unit) {
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
            if (state.winner == viewModel.userID) viewModel.player.playFile(R.raw.victory_fanfare)
            else viewModel.player.playFile(R.raw.gameover)

            BasicAlertDialog(onDismissRequest = {}) {
                Surface(shape = RoundedCornerShape(16.dp)) {
                    Column{
                        Text(
                            text = "${state.winner}",
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
                                    if (viewModel is NearbyGameViewModel) viewModel.disconnect();
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
        else -> {

        }
    }
}
@Composable
fun SinglePlayer(modifier: Modifier = Modifier,state: GameUIState,viewModel: SemelionGameViewModel,padding: PaddingValues) {
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            GameScreen(modifier=Modifier.wrapContentWidth(),viewModel=viewModel, state = state, cardSize= CardSize.LARGE)
        }
        Configuration.ORIENTATION_PORTRAIT -> {
            GameScreen(modifier=Modifier.wrapContentWidth(),viewModel=viewModel, state=state, cardSize = CardSize.SMALL)
        }
        else -> {
        }
    }
}
@Composable
fun MultiPlayer(modifier: Modifier = Modifier,state: GameUIState,viewModel: NearbyGameViewModel) {
    //Log.d("finder","jack:${state.grid.indexOfFirst { it.value == 8 }}")
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            GameScreen(modifier=Modifier.wrapContentWidth(),viewModel=viewModel, state = state, cardSize= CardSize.LARGE)
        }
        Configuration.ORIENTATION_PORTRAIT -> {
            GameScreen(modifier=Modifier.wrapContentWidth(),viewModel=viewModel, state=state, cardSize = CardSize.SMALL)
        }
        else -> {

        }
    }
}

@Composable
fun GameScreen(modifier: Modifier = Modifier,viewModel: BaseGameViewModel,state: GameUIState, cardSize: CardSize) {
    val conf = when(viewModel){
        is NearbyGameViewModel ->{
            if (viewModel.connectionState.value.isHost) {
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
            playerName = viewModel.opponentName,
            avatar = viewModel.secondPlayerAvatar
        )

        FinalGrid(state = state, model = viewModel, cardSize = cardSize)
        //giocatore
        OpponentHeader(
            actionsUsed = conf.second.first,
            actionsTotal = conf.second.second,
            isWaiting = conf.second.third,
            playerName = viewModel.playerName,
            avatar = viewModel.firstPlayerAvatar
        )
    }
}
@Composable
fun OpponentHeader(
    actionsUsed: Int,
    actionsTotal: Int,
    isWaiting: Boolean,
    playerName: String,
    avatar: Int
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (!isWaiting) Color.White else Color(0xFFF5F5F5),
        border = if (!isWaiting) BorderStroke(
            2.dp,
            GreenAccent
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

@Composable
fun UncoverDeck(state: GameUIState){
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5F5F5),
        modifier = Modifier.width(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "MAZZO SCOPERTA",
                fontSize = 9.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            // Mostra la carta scoperta se disponibile, altrimenti placeholder

            val revealedCard =
                if (state.uncoverDeck.isNotEmpty()) state.uncoverDeck.first() else null
            if (revealedCard != null) {
                val imageResId =
                    if (revealedCard.isRevealed) cardImageMap[revealedCard.name]
                        ?: R.drawable.purple_back else R.drawable.purple_back
                Image(
                    painter = painterResource(imageResId),
                    contentDescription = "Carta scoperta",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.65f)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.65f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("—", color = TextSecondary, fontSize = 20.sp)
                }
            }
        }
    }
}