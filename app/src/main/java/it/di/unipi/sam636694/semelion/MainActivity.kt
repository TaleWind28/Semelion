package it.di.unipi.sam636694.semelion

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.database.User
import it.di.unipi.sam636694.semelion.utilities.ObserveAsEvents
import it.di.unipi.sam636694.semelion.utilities.SnackBarController
import it.di.unipi.sam636694.semelion.ui.theme.SemelionTheme
import it.di.unipi.sam636694.semelion.utilities.AudioPlayer
import kotlinx.coroutines.launch
import androidx.core.content.edit
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val db by lazy { SemelionDB.getDatabase(this) }

//    private val prefs by lazy { this.getSharedPreferences("setting", Context.MODE_PRIVATE) }
    // ✅ CORRETTO
    private lateinit var prefs: SharedPreferences

    private val player by lazy {
        AudioPlayer(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
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
                    Log.d("mainActivity","1")
                    var deviceUser = prefs.getString("semelion_local_userId", null)
                    if (deviceUser == null){
                        deviceUser = UUID.randomUUID().toString()
                        //modifico le preferenze
                        prefs.edit {
                            putString("semelion_local_userId", deviceUser)
                        }
                    }




                    Log.d("mainActivity","PreObserve:${prefs.getString("semelion_local_userId",null)}")


                    ObserveAsEvents(flow = SnackBarController.events, snackBarHostState) { event ->
                        scope.launch {
                            snackBarHostState.currentSnackbarData?.dismiss()

                            val result = snackBarHostState.showSnackbar(
                                message = event.message,
                                actionLabel = event.action?.name,
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                event.action?.action?.invoke()
                            }
                        }
                    }

                    SemelionNavigation(snackBarHostState,db, player,deviceUser)
                }
            }
        }
}
