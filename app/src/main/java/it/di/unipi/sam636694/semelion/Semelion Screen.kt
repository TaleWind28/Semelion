package it.di.unipi.sam636694.semelion
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.FinalGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemelionScreen(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    viewModel: SemelionGameViewModel = viewModel(),
){
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current

    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            Log.d("kingFinder","${state.grid.indexOfFirst { it.name == "10D" }}")
            Landscape(state = state, viewModel = viewModel)
        }
        else -> {
            Column(
                modifier = modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Portrait(state = state,viewModel = viewModel)
            }

        }
    }

    // Game over dialog
    if (state.phase is GamePhase.GameOver) {
        BasicAlertDialog(onDismissRequest = {viewModel.setup()}) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Text(
                    text = "${state.winner}",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
@Composable
fun Portrait(state: GameUIState, viewModel: SemelionGameViewModel){

    OpponentHeader(actionsUsed=state.p2ActionsUsed, actionsTotal =state.p2Actions, isWaiting = state.p1Turn )

    FinalGrid(state = state, model = viewModel)

    Column{
        ActionsPanel(state = state)

        PlayerFooter(isYourTurn = state.p1Turn)
    }
}

@Composable
fun Landscape(state: GameUIState, viewModel: SemelionGameViewModel) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(
                text = "il giocatore 2 ha: ${state.p2Actions - state.p2ActionsUsed} azioni Rimanenti",
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally).rotate(180f)
            )

            FinalGrid(state = state, model = viewModel)

            Text(
                text = "il giocatore 1 ha: ${state.p1Actions - state.p1ActionsUsed} azioni Rimanenti",
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

}
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

@Composable
fun OpponentHeader(
    actionsUsed: Int,
    actionsTotal: Int,
    isWaiting: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE8F5E9),
        modifier = Modifier.fillMaxWidth()
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "OPPONENT",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

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
        modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.weight(1f).height(56.dp),
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
                        modifier = Modifier.weight(1f).height(56.dp),
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
        border = if (isYourTurn) androidx.compose.foundation.BorderStroke(
            2.dp,
            GreenAccent
        ) else null,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = if (isYourTurn) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "YOU",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

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
