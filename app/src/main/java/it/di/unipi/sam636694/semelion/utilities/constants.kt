package it.di.unipi.sam636694.semelion.utilities

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent

val JOLLY_COLOR = listOf("black","red")
const val DELAY_TIME: Long = 500
val SNACKBAR_DELAY_TIME:Long = 2000
const val UNCOVER_DECK_SIZE = 8
val POSITION_VALUES = Pair(first = {rid:Int,pos:Int -> 7*(rid+1)-pos},second= {rid:Int,pos:Int -> pos+1-(7*rid)})
val SEMELION_FIGURES = listOf(
    Pair(10,"D"),
    Pair(9,"C"),
    Pair(9,"P"),
    Pair(9,"F"),
    Pair(8,"C"),
    Pair(8,"P")

    )

// ─── Colori del tema ──────────────────────────────────────────────────────────
val GreenAccent   = Color(0xFF3BFF7C)
val TextPrimary   = Color(0xFF111111)
val TextSecondary = Color(0xFF888888)

val Pergamena = Color(0xFFF5EFE0)
val SabbiaCalda = Color(0xFFEDE0C8)
val BeigeMiddio = Color(0xFFD6C9A8)
val OcraMorbida = Color(0xFFC8B98A)
val Scuro = Color(0xFF3D3526)
