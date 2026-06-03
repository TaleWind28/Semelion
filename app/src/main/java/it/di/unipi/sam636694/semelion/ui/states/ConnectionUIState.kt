package it.di.unipi.sam636694.semelion.ui.states

data class ConnectionUiState(
    val connectedEndpointId: String? = null,
    val isSearching: Boolean = false,
    val isHost: Boolean = false,
    val status: String = "Scegli un ruolo per iniziare",
    val sent: Boolean = false,
    val received: Boolean = false,
    val gameStarted: Boolean = false,
    val discoveredEndpoints: List<DiscoveredEndpoint> = emptyList(),
)

data class DiscoveredEndpoint(
    val endpointId:String,
    val endpointName:String,
    val avatarIndex: Int,
    val foundAt: Long = System.currentTimeMillis()
)