package it.di.unipi.sam636694.semelion.utilities

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.di.unipi.sam636694.semelion.viewModels.utilityModels.LogViewModel
import it.di.unipi.sam636694.semelion.ui.screens.SemelionRules
import it.di.unipi.sam636694.semelion.ui.screens.SemelionScreen
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.viewModels.gameModels.BaseGameViewModel


enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val screen: @Composable (PaddingValues, SemelionDB, BaseGameViewModel, ()->Unit ) -> Unit
) {
    GAME("Game", Icons.Default.Home, { _,_, viewModel,onBack -> SemelionScreen(viewModel = viewModel, onBack = onBack) }),
    RULES("Rules", Icons.Outlined.Settings, { _, _,_,_-> SemelionRules() }),
    ACTIONS("Actions", Icons.Default.AccountBox, { padding, _,_,_ -> LogScreen(padding = padding)}),
}

@Composable
fun LogScreen(modifier: Modifier = Modifier, viewModel: LogViewModel = viewModel(),padding: PaddingValues){
    val state by viewModel.uiState.collectAsState()
    Log.d("padding","$padding")
    LazyColumn(modifier = modifier.background(Color.LightGray).fillMaxSize().padding(15.dp)) {
        items(state.actions.reversed()){
            Text(text=viewModel.translateAction(it),color=Color.Black)
        }
    }
}

@Composable
fun NavigationUIApp(snackBarHostState: SnackbarHostState, db: SemelionDB, viewModel: BaseGameViewModel, onNavigateBack: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.GAME) }
    //intercetto il back per tornare alla schermata di gioco e gestirlo effettivamente da lì
    BackHandler(enabled = currentDestination != AppDestinations.GAME) {
        currentDestination = AppDestinations.GAME
    }

    NavigationSuiteScaffold(
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
            currentDestination.screen(innerPadding,db, viewModel, onNavigateBack)
        }
    }
}