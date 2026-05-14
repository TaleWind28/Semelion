package it.di.unipi.sam636694.semelion
import android.content.res.Configuration
import android.util.Log
import android.widget.Space
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.gameModels.BaseGameViewModel
import it.di.unipi.sam636694.semelion.gameModels.NearbyGameViewModel
import it.di.unipi.sam636694.semelion.gameModels.SemelionGameViewModel
import it.di.unipi.sam636694.semelion.ui.states.FinalGrid
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import it.di.unipi.sam636694.semelion.utilities.LogScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemelionScreen(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    viewModel: BaseGameViewModel = viewModel(),
    //logModel: LogViewModel,
    player: AudioPlayer,
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
                        text = "Vuoi terminare la partita verrà terminata oppure sospenderla e riprenderla più tardi?",
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
        is SemelionGameViewModel -> viewModel.matchEnd(GameModes.ScreenSharing)
        is NearbyGameViewModel ->{
            viewModel.matchEnd(GameModes.NearBy)
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
    //var showLogScreen by remember { mutableStateOf(false) }
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            Landscape(
                modifier = Modifier.wrapContentWidth(),
                state = state,
                viewModel = viewModel
            )
        }
        else -> {
                Portrait(
                    state = state,
                    viewModel = viewModel
                )
        }
    }
}

@Composable
fun MultiPlayer(modifier: Modifier = Modifier,state: GameUIState,viewModel: NearbyGameViewModel) {
    Log.d("finder","jack:${state.grid.indexOfFirst { it.value == 8 }}")
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            Landscape(state = state, viewModel = viewModel, modifier=Modifier)
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Portrait(state = state,viewModel = viewModel)
            }

        }
    }
}

@Composable
fun Portrait(state: GameUIState, viewModel: BaseGameViewModel){
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ){
        Spacer(Modifier.size(10.dp))

        OpponentHeader(
            actionsUsed=state.p2ActionsUsed,
            actionsTotal =state.p2Actions,
            isWaiting = state.p1Turn,
            playerName = viewModel.secondPlayerId
        )

        FinalGrid(state = state, model = viewModel)

        OpponentHeader(
            actionsUsed = state.p1ActionsUsed,
            actionsTotal = state.p1Actions,
            isWaiting = !state.p1Turn,
            playerName = viewModel.userID
        )

        Spacer(Modifier.size(10.dp))
    }
}
@Composable
fun Landscape(state: GameUIState, viewModel: BaseGameViewModel,modifier: Modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OpponentHeader(actionsUsed=state.p2ActionsUsed, actionsTotal =state.p2Actions, isWaiting = state.p1Turn,viewModel.secondPlayerId )
            FinalGrid(state = state, model = viewModel,cardSize= CardSize.LARGE)
            OpponentHeader(actionsUsed = state.p1ActionsUsed, actionsTotal = state.p1Actions, isWaiting = !state.p1Turn,viewModel.userID)
        }
}

@Composable
fun OpponentHeader(
    actionsUsed: Int,
    actionsTotal: Int,
    isWaiting: Boolean,
    playerName: String
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
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
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
fun ActionsPanel(state: GameUIState) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Azioni
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF5F5F5),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        "ACTIONS",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("REMAINING", fontSize = 9.sp, color = TextSecondary)
                        val remaining = state.p1Actions - state.p1ActionsUsed
                        repeat(state.p1Actions) { i ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i < remaining) GreenAccent else Color(
                                            0xFFCCCCCC
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // REVEAL
                    Button(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier
                            //.weight(1f)
                            .height(56.dp),
                        enabled = state.p1Turn && state.p1ActionsUsed < state.p1Actions
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "REVEAL",
                                fontSize = 10.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // SWAP
                    Button(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier
                            //.weight(1f)
                            .height(56.dp),
                        enabled = state.p1Turn && state.p1ActionsUsed < state.p1Actions
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // icona swap
                            Text("⇄", fontSize = 18.sp, color = Color.Black)
                            Text(
                                "SWAP",
                                fontSize = 10.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    //UncoverDeck(state = state)
                }
            }
        }

    }
}

@Composable
fun PlayerFooter(isYourTurn: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isYourTurn) Color.White else Color(0xFFF5F5F5),
        border = if (isYourTurn) BorderStroke(
            2.dp,
            GreenAccent
        ) else null,
        modifier = Modifier,
        shadowElevation = if (isYourTurn) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF607D8B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.wrapContentWidth()) {
                Text(
                    "YOU",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(50.dp))
            if (isYourTurn) {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                    ) {
                        Text(
                            "YOUR TURN",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    //ActionsPanel(state = state)
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

//@Preview(
//    showBackground = true,
//    showSystemUi = false,
//    device = Devices.TABLET
//    )
//@Composable
//fun PreviewScreen(){
//    val context = LocalContext.current
//    val db = SemelionDB.getDatabase(context)
//    val viewModel = SemelionGameViewModel(
//        db.matchesDao(),
//        db.participationsDao(),
//        db.matchStatisticsDao(),
//        db.playerStatisticsDao(),
//        db.userDao(),
//        AudioPlayer(context),
//        userID = "Sora",
//        secondPlayerId = "TaleWind"
//        )
//    SemelionScreen(
//        padding = PaddingValues(4.dp),
//        viewModel = viewModel,
//        player = AudioPlayer(context),
//    ) { }
//}

@Composable
fun SemelionTopBar() {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Semelion",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TURN",
                    fontSize = 10.sp,
                    color = GreenAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "YOU (Player 1)",
                    fontSize = 12.sp,
                    color = GreenAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
        }
    }
}
