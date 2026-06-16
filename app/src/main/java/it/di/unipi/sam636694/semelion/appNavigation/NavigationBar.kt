package it.di.unipi.sam636694.semelion.appNavigation

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import it.di.unipi.sam636694.semelion.viewModels.utilityModels.LogViewModel
import it.di.unipi.sam636694.semelion.ui.screens.SemelionRules
import it.di.unipi.sam636694.semelion.ui.screens.SemelionScreen
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.viewModels.gameModels.BaseGameViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import it.di.unipi.sam636694.semelion.ui.theme.Arancione
import it.di.unipi.sam636694.semelion.ui.theme.BgGreen
import it.di.unipi.sam636694.semelion.ui.theme.CardBg
import it.di.unipi.sam636694.semelion.ui.theme.DrawColor
import it.di.unipi.sam636694.semelion.ui.theme.GreenPrimary
import it.di.unipi.sam636694.semelion.ui.theme.OcraMorbida
import it.di.unipi.sam636694.semelion.ui.theme.Purple40
import it.di.unipi.sam636694.semelion.ui.theme.Purple80
import it.di.unipi.sam636694.semelion.ui.theme.TextDark


enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val screen: @Composable (PaddingValues, SemelionDB, BaseGameViewModel, LogViewModel,()->Unit ) -> Unit
) {
    GAME("Game", Icons.Default.Home, { _,_, viewModel,_,onBack -> SemelionScreen(viewModel = viewModel, onBack = onBack) }),
    RULES("Rules", Icons.Outlined.Settings, { _, _,_,_,_-> SemelionRules() }),
    ACTIONS("Actions", Icons.Default.AccountBox, { _, _,_,logViewModel,_ -> LogScreen(viewModel =logViewModel)}),
}



@Composable
fun NavigationUIApp(
    snackBarHostState: SnackbarHostState,
    db: SemelionDB,
    viewModel: BaseGameViewModel,
    logViewModel: LogViewModel,
    onNavigateBack: () -> Unit
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.GAME) }

    // 1. Recuperiamo la configurazione attuale per sapere se siamo in LANDSCAPE
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 2. Scegliamo il tipo di layout in base all'orientamento dello schermo
    val suiteLayoutType = if (isLandscape) {
        NavigationSuiteType.NavigationRail // Barra Verticale a SINISTRA
        // Nota: Se preferisci i tre bottoni classici puoi usare anche NavigationSuiteType.NavigationDrawer
    } else {
        NavigationSuiteType.NavigationBar  // Barra Orizzontale in BASSO
    }

    //intercetto il back per tornare alla schermata di gioco e gestirlo effettivamente da lì
    BackHandler(enabled = currentDestination != AppDestinations.GAME) {
        currentDestination = AppDestinations.GAME
    }
    NavigationSuiteScaffold(
        layoutType = suiteLayoutType,
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackBarHostState,
                    modifier = Modifier.fillMaxSize(),
                    snackbar = { snackbarData ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center  // centra il contenuto
                        ) {
                            Snackbar(snackbarData = snackbarData)
                        }
                    }
                ) },
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            currentDestination.screen(innerPadding,db, viewModel, logViewModel,onNavigateBack)
        }
    }
}

//Tipi di evento
enum class EventType { RIVELAZIONE, RIMPIAZZO,COPERTURA, SCAMBIO, SCAMBIO_CIRCOLARE,REGINA,RE }

data class LogEvent(
    val type: EventType,
    val title: String,
    val body:String,
)

@Composable
fun LogScreen(modifier: Modifier = Modifier, viewModel: LogViewModel){
    val state by viewModel.uiState.collectAsState()
    Log.d("tate","${state.actions}")
    LazyColumn(modifier = modifier.background(BgGreen).fillMaxSize().padding(15.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.actions.reversed()){
            LogEventRow(viewModel.translateAction(it))
        }
    }
}

//Riga evento
@Composable
private fun LogEventRow(event: LogEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                //titolo
                Text(
                    text = event.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = eventTitleColor(event.type)
                )
                //azione
                Text(
                    text = event.body,
                    fontSize = 15.sp,
                    color = TextDark,
                    lineHeight = 22.sp
                )
            }
        }

    }
}

fun eventTitleColor(type: EventType):Color{
    return when(type){
        EventType.RIVELAZIONE-> GreenPrimary
        EventType.RIMPIAZZO -> DrawColor
        EventType.COPERTURA -> Color.Black
        EventType.SCAMBIO -> Arancione
        EventType.SCAMBIO_CIRCOLARE -> Purple40
        EventType.RE -> OcraMorbida
        EventType.REGINA -> Purple80
    }
}