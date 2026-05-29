package it.di.unipi.sam636694.semelion

import androidx.navigation3.runtime.NavKey
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

}