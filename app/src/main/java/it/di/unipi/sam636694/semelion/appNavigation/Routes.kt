package it.di.unipi.sam636694.semelion.appNavigation

import androidx.navigation3.runtime.NavKey
import it.di.unipi.sam636694.semelion.ui.screens.DisplayPlayerStats
import it.di.unipi.sam636694.semelion.viewModels.gameModels.BaseGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.NearbyGameViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {
    @Serializable
    data object ScreenSharingGame: Route

    @Serializable
    data object Home: Route

    @Serializable
    data object SemelionConnections : Route

    @Serializable
    data object RulesPage : Route

    @Serializable
    data object ProfilePage : Route

    data class SemelionNearbyGame(val viewModel: NearbyGameViewModel) : Route

    @Serializable
    data class MatchStatScreen(val backRoute: Route): Route
}