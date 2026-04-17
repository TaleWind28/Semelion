package it.di.unipi.sam636694.semelion.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import it.di.unipi.sam636694.semelion.LogViewModel
import it.di.unipi.sam636694.semelion.PdfViewerScreen
import it.di.unipi.sam636694.semelion.SemelionScreen


enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val screen: @Composable (Modifier) -> Unit  // ← aggiunto
) {
    HOME("Home", Icons.Default.Home, { modifier -> SemelionScreen(modifier = modifier) }),
    FAVORITES("Rules", Icons.Default.Favorite, { modifier -> PdfViewerScreen(modifier = modifier) }),
    PROFILE("Profile", Icons.Default.AccountBox, { modifier -> LogScreen(modifier = modifier)}),
}

@Composable
fun LogScreen(modifier: Modifier = Modifier, viewModel: LogViewModel = viewModel()){
    Text("LogScreen", color = Color.Black)
    val state by viewModel.uiState.collectAsState()

    LazyColumn(modifier = modifier.background(Color.LightGray)) {
        items(state.actions){
            Text(text=it,color=Color.Black)
        }
    }
}

@Composable
fun NavigationUIApp(snackBarHostState: SnackbarHostState) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
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
            snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->

            currentDestination.screen(Modifier.padding(innerPadding))
        }
    }
}