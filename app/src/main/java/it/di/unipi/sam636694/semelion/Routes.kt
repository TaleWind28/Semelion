package it.di.unipi.sam636694.semelion

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {
    @Serializable
    data class ScreenSharingGame(val userId: Long): Route

    @Serializable
    data object Home: Route

    @Serializable
    data object SemelionConnections : Route

}