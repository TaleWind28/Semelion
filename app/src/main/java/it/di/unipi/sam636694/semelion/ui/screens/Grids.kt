package it.di.unipi.sam636694.semelion.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Canvas
import android.graphics.Point
import android.util.Log
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import it.di.unipi.sam636694.semelion.utilities.Direction
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import it.di.unipi.sam636694.semelion.ui.theme.Arancione
import it.di.unipi.sam636694.semelion.ui.theme.Blu
import it.di.unipi.sam636694.semelion.ui.theme.GreenAccent
import it.di.unipi.sam636694.semelion.ui.theme.Rosso
import it.di.unipi.sam636694.semelion.utilities.GameStrings
import it.di.unipi.sam636694.semelion.ui.snackbar.SnackBarController
import it.di.unipi.sam636694.semelion.ui.snackbar.SnackBarEvent
import it.di.unipi.sam636694.semelion.utilities.cardImageMap
import it.di.unipi.sam636694.semelion.utilities.rememberGameStrings
import it.di.unipi.sam636694.semelion.viewModels.gameModels.BaseGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.NearbyGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.SemelionGameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun FinalGrid(state: GameUIState, model: BaseGameViewModel) {
    //per garantire una buona visualizzazione su qualsiasi dispositivo, in entrambe le modalità uso una BoxWithCostraint
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
            //Calcolo width
            val widthBasedSize = (maxWidth - 16.dp)/7 // Padding orizzontale di CardRow
            //Calcolo Height
            //divido l'altezza massima per il numero di righe
            val maxRowHeight =  (maxHeight - 60.dp)/4 // Padding verticale

            // Convertiamo l'altezza massima della riga nella larghezza corrispondente della carta.
            val heightBasedSize = maxRowHeight * 0.75f

            // In portrait le carte usano width mentre in landscape usano height
            val dynamicCardSize = minOf(widthBasedSize, heightBasedSize)

            val playerConfigs = produceConfigs(state = state, viewModel = model)
            val attentionModifier = { style: Color ->
                Modifier.border(
                    color = style,
                    width = 6.dp,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            //utility per non duplicare codice
            fun playerModifier(isActive: Boolean, style: Color): Modifier =
                if (isActive) attentionModifier(style) else Modifier

            Column(
                modifier = Modifier.wrapContentWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                playerConfigs.forEachIndexed { index, (isActive, playerRows, style) ->
                    if (index > 0) Spacer(modifier = Modifier.size(8.dp))
                    Box(modifier = playerModifier(isActive, style.first)) {
                        Column(verticalArrangement = Arrangement.SpaceEvenly) {
                            playerRows.forEachIndexed { rowIndex, rowItems ->
                                CardRow(
                                    rowIndex = rowIndex + (index * 2),
                                    rowItems = rowItems,
                                    model = model,
                                    rowBackground = style.first.copy(alpha = if (index == 0) 0.15f else 0.08f),
                                    phase = state.phase,
                                    cardSize = dynamicCardSize
                                )
                            }
                        }
                    }
                }
            }
        }
    }
fun produceConfigs(state: GameUIState, viewModel: BaseGameViewModel):List<Triple<Boolean,List<List<CardUIStates>>,Pair<Color,Float>>>{
    val rows = state.grid.chunked(7)
    return when(viewModel){
        is SemelionGameViewModel -> {
            listOf(
                //g2
                Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(Rosso, 180f)),
                //g1
                Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(Blu, 0f))
            )
        }
        is NearbyGameViewModel ->{
            if (viewModel.connectionManager.connectionState.value.isHost){
                listOf(
                    //guest
                    Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(Arancione, 180f)),
                    //host
                    Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(GreenAccent, 0f))
                    )
            }else{
                listOf(
                    //host
                    Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(Arancione, 0f)),
                    //guest
                    Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(GreenAccent, 180f))
                )
            }

        }
        else -> {
             listOf(
                 //g2
                Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(Arancione, 180f)),
                //g1
                Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(GreenAccent, 0f))
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CardRow(rowIndex: Int, rowItems: List<CardUIStates>, model: BaseGameViewModel, rowBackground: Color, phase: GamePhase, cardSize:Dp) {

    val draggableState = remember {
        AnchoredDraggableState(initialValue = 0)
    }

    //risoluzione swipe left/right
    LaunchedEffect(draggableState.currentValue) {
        //lo swipe può essere solo effettuato se è stato rivelato un re
        if (phase !is GamePhase.KingPending) return@LaunchedEffect
        when (draggableState.currentValue) {
            -1 -> {
                model.processIntent(GameIntent.KingDirectionChosen(rowIndex = rowIndex, direction = Direction.LEFT))
                draggableState.animateTo(0)
            }
            1 -> {
                model.processIntent(GameIntent.KingDirectionChosen(rowIndex = rowIndex, direction = Direction.RIGHT))
                draggableState.animateTo(0)
            }
        }
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = rowBackground,
        shadowElevation = 2.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .onSizeChanged { size ->
                    draggableState.updateAnchors(
                    newAnchors = DraggableAnchors {
                        (-1) at -size.width.toFloat()
                        0 at 0f
                        1 at size.width.toFloat()
                    }
                ) }
                .anchoredDraggable(draggableState, Orientation.Horizontal),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            val columnSwipableStates = remember { List(rowItems.size){AnchoredDraggableState(0)} }

            rowItems.forEachIndexed { itemIndex, card ->

                val colState = columnSwipableStates[itemIndex]

                //risoluzione swipe up/down
                LaunchedEffect(colState.currentValue) {
                    if (phase !is GamePhase.QueenPending) return@LaunchedEffect
                    when (colState.currentValue) {
                        //verso l'alto
                        -1 -> {
                            model.processIntent(GameIntent.QueenDirectionChosen(colIndex=itemIndex,direction = Direction.UP))
                            colState.animateTo(0)
                        }
                        //verso il basso
                        1 -> {
                            model.processIntent(GameIntent.QueenDirectionChosen(colIndex=itemIndex,direction = Direction.DOWN))
                            colState.animateTo(0)
                        }
                    }
                }
                Box(modifier =
                    Modifier.onSizeChanged { size ->

                        colState.updateAnchors(
                            newAnchors = DraggableAnchors {
                                (-1) at -size.height.toFloat()
                                0 at 0f
                                1 at size.height.toFloat()
                            }
                        )
                    }
                    .anchoredDraggable(colState, orientation = Orientation.Vertical))
                {
                    // CARTE
                    RevealCard(card = card, model = model, size = cardSize)
                }
            }
        }
    }
}

@Composable
fun FinalCard(card: CardUIStates, model: BaseGameViewModel, size: Dp) {
    //context densità e size in pixel
    val density = LocalDensity.current
    val strings = rememberGameStrings()
    val sizePx = with(density) { size.toPx().toInt()}

    val imageResId = if (card.isRevealed)
        (cardImageMap[card.name]) ?: R.drawable.semelion_back
    else
        R.drawable.semelion_back

    val view = LocalView.current

    val imageBitmap = ImageBitmap.imageResource(id = imageResId)

    val scope = rememberCoroutineScope()

    Image(
        modifier = Modifier
            .size(size)
            .pointerInput(card.name, card.isRevealed) {
                detectTapGestures(
                    onTap = {
                        if (!card.isRevealed && model.uiState.value.phase is GamePhase.PlayerTurn)
                            model.processIntent(GameIntent.CardClicked(cardId = card.name))
                        else comunicateUnexpectedAction(model.uiState.value.phase,scope,strings)
                    },
                    onLongPress = {
                        Log.d("grid","fase:${model.uiState.value.phase}")
                        if (model.uiState.value.phase !is GamePhase.PlayerTurn) {
                            comunicateUnexpectedAction(model.uiState.value.phase, scope,strings)
                            return@detectTapGestures
                        }

                        //il 42 fa ridere ma è stato calcolato a mano
                        val scaled = imageBitmap.asAndroidBitmap().scale(sizePx / 2 + 42, sizePx, false)
                        val shadow = object : View.DragShadowBuilder() {
                            override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
                                outShadowSize.set(scaled.width, scaled.height)
                                outShadowTouchPoint.set(scaled.width / 2, scaled.height / 2)
                            }

                            override fun onDrawShadow(canvas: Canvas) {
                                canvas.drawBitmap(scaled, 0f, 0f, null)
                            }
                        }
                        val clipData = ClipData.newPlainText(card.name, card.name)

                        view.startDragAndDrop(clipData, shadow, card.name, 0)
                    }
                )

            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = remember(card.name) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val text = (event.toAndroidDragEvent()
                                .clipData?.getItemAt(0)?.text ?: "") as String
                            if (text == card.name) return false
                            model.processIntent(GameIntent.SwapCards(text,card.name))
                            return true
                        }
                    }
                }
            )
            ,
        painter = painterResource(id = imageResId),
        contentDescription = "Carta Semelion"
    )
}

fun comunicateUnexpectedAction(gamePhase: GamePhase, scope: CoroutineScope,strings: GameStrings) {
    when (gamePhase) {
        is GamePhase.QueenPending -> scope.launch {
            SnackBarController.sendEvent(
                event = SnackBarEvent(
                    message = strings.queenHint
                )
            )
        }

        is GamePhase.KingPending -> scope.launch {
            SnackBarController.sendEvent(
                event = SnackBarEvent(
                    message = strings.kingHint
                )
            )
        }

        is GamePhase.WaitingForOpponent -> scope.launch {
            SnackBarController.sendEvent(
                event = SnackBarEvent(
                    message = strings.waitingHint
                )
            )
        }

        else -> {}
    }
}
@Composable
fun RevealCard(card: CardUIStates, model: BaseGameViewModel, size: Dp){
    AnimatedContent(
        targetState = card.isRevealed,
        transitionSpec = {
            (scaleIn(initialScale = 0.75f) + fadeIn(tween(180))) togetherWith
                    (scaleOut(targetScale = 0.75f) + fadeOut(tween(120)))
        },
        label = "card_reveal_${card.name}"
    ) { isRevealed ->
        //Se il parametro della lambda non viene usato il compilatore dà errore, che non blocca l'esecuzione però mi dava noia.
        //ps: la lambda lo richiede per forza però non ha senso aggiungere un parametro a una funzione che prende la classe dalla quale deriva suddetto parametro
        Log.d("uselessErrorRemover","$isRevealed")
        FinalCard(card = card, model = model,size=size)
    }

}