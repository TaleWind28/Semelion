package it.di.unipi.sam636694.semelion

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import it.di.unipi.sam636694.semelion.database.GameModes
import it.di.unipi.sam636694.semelion.database.Matches
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.utilities.NavigationUIApp
import kotlin.collections.listOf
import kotlin.collections.mapOf

@Composable
fun SemelionNavigation(snackBarHostState: SnackbarHostState, db: SemelionDB){

    val backStack = rememberNavBackStack(Route.Home)


    NavDisplay(
        modifier = Modifier,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        backStack = backStack,
        entryProvider = { key ->
            when(key){
                is Route.Home ->
                    NavEntry(key){
                        SemelionHomeScreen(
                            destinations =  mapOf("Quick Play" to {backStack.add(Route.ScreenSharingGame(123L))})
                        )
                    }
                is Route.ScreenSharingGame -> NavEntry(key){
                    val viewModel: SemelionGameViewModel = viewModel(
                        factory = SemelionGameViewModel.factory(db.matchesDao(), db.participationsDao())
                    )
                    viewModel.createMatch(GameModes.ScreenSharing,viewModel.uiState.value,123L,123L)
                    NavigationUIApp(snackBarHostState = snackBarHostState,db = db,viewModel)
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
){

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