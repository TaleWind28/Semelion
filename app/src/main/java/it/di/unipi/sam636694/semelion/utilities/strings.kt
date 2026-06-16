package it.di.unipi.sam636694.semelion.utilities

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.di.unipi.sam636694.semelion.R

data class GameStrings(
    val queenHint: String,
    val kingHint: String,
    val waitingHint: String
)

@Composable
fun rememberGameStrings() = GameStrings(
    queenHint = stringResource(R.string.queenEffectResolverHint),
    kingHint = stringResource(R.string.kingEffectResolverHint),
    waitingHint = stringResource(R.string.opponentTurnNotice),
)