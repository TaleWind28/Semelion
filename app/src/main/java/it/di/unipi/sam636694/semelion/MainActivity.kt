package it.di.unipi.sam636694.semelion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import it.di.unipi.sam636694.semelion.ui.theme.SemelionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 4. Nasconde sia la Status Bar (alto) che la Navigation Bar (basso)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            SemelionTheme {
                val snackBarHostState = remember {
                    SnackbarHostState()
                }
                val scope  = rememberCoroutineScope()
                ObserveAsEvents(flow = SnackBarController.events,snackBarHostState) { event ->
                    scope.launch {
                        snackBarHostState.currentSnackbarData?.dismiss()

                        val result  = snackBarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.action?.name,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed){
                            event.action?.action?.invoke()
                        }
                    }
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackBarHostState
                        )
                    },
                    topBar = { SemelionTopBar() },
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.White,
                    contentWindowInsets = WindowInsets(0,0,0,0)
                ) { padding ->
                    SemelionScreen(Modifier.fillMaxSize().padding(padding))
                }
            }
        }
    }
}