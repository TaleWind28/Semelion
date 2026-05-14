package it.di.unipi.sam636694.semelion

import androidx.navigation3.runtime.NavKey
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.PayloadCallback
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {

    @Serializable
    data class NearbyGame(val hostId:Long, val guestId:Long, val connectionsClient: ConnectionsClient) : Route

    @Serializable
    data object NearbyLobby : Route

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