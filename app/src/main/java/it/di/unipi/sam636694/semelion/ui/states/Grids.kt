package it.di.unipi.sam636694.semelion.ui.states

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import it.di.unipi.sam636694.semelion.utilities.CardSize
import it.di.unipi.sam636694.semelion.utilities.Direction
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.utilities.SnackBarController
import it.di.unipi.sam636694.semelion.utilities.SnackBarEvent
import it.di.unipi.sam636694.semelion.utilities.cardImageMap
import it.di.unipi.sam636694.semelion.viewModels.gameModels.BaseGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.NearbyGameViewModel
import it.di.unipi.sam636694.semelion.viewModels.gameModels.SemelionGameViewModel
import kotlinx.coroutines.launch

@Composable
fun FinalGrid(state: GameUIState, model: BaseGameViewModel,cardSize: CardSize = CardSize.SMALL) {
    Log.d("coinFlip", "FinalGrid ricevuto p1Turn=${state.p1Turn}")
    Box(modifier= Modifier.wrapContentWidth()) {

        val playerConfigs = produceConfigs(state = state, viewModel = model)
        val attentionModifier ={style:Color ->
            Modifier.border(
            color = style,
            width = 6.dp,
            shape = RoundedCornerShape(8.dp)
            )
        }

        //utility per non duplicare codice
        fun playerModifier(isActive: Boolean, style:Color): Modifier =
            if (isActive) attentionModifier(style) else Modifier

        Column(
            modifier = Modifier.wrapContentWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            playerConfigs.forEachIndexed { index, (isActive, playerRows, style) ->
                Log.d("SemelionScreen","$index")
                if (index > 0) Spacer(modifier = Modifier.size(8.dp))

                Box(modifier = playerModifier(isActive,style.first)) {
                    Column(verticalArrangement = Arrangement.SpaceEvenly) {
                        playerRows.forEachIndexed { rowIndex, rowItems ->
                            CardRow(
                                rowIndex = rowIndex + (index * 2),
                                rowItems = rowItems,
                                model = model,
                                rowBackground = style.first.copy(alpha = if (index == 0) 0.15f else 0.08f),
                                phase = state.phase,
                                enabled= true,
                                grid=state.grid,
                                cardSize = cardSize.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun produceConfigs(state: GameUIState,viewModel: BaseGameViewModel):List<Triple<Boolean,List<List<CardUIStates>>,Pair<Color,Float>>>{
    val rows = state.grid.chunked(7)
    return when(viewModel){
        is SemelionGameViewModel -> {
            Log.d("coinFlip","turno:${state.p1Turn}")
            listOf(
                //g2
                //rosso 0xFFE53935
                Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(Color(0xFFE53935), 180f)),
                //g1
                //blu 0xFF2196F3
                Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(Color(0xFF2196F3), 0f))
            )
        }
        is NearbyGameViewModel ->{
            if (viewModel.connectionState.value.isHost){
                listOf(
                    //guest
                    //arancione 0xFFFF9800
                    Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(Color(0xFFFF9800), 180f)),
                    //host
                    //verde 0xFF3BFF7C
                    Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(Color(0xFF3BFF7C), 0f))
                    )
            }else{
                listOf(
                    //host
                    //arancione 0xFFFF9800
                    Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(Color(0xFFFF9800), 0f)),
                    //guest
                    //verde 0xFF3BFF7C
                    Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(Color(0xFF3BFF7C), 180f))
                )
            }

        }
        else -> {
             listOf(
                Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(Color(0xFF009688), 180f)),
                //g1
                Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(Color(0xFF9C27B0), 0f))
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CardRow(rowIndex: Int, rowItems: List<CardUIStates>, model: BaseGameViewModel, rowBackground: Color, phase: GamePhase, enabled:Boolean,grid: List<CardUIStates>, cardSize:Dp) {
    //preparazione misure
    val density = LocalDensity.current

    val draggableState = remember {
        AnchoredDraggableState(initialValue = 0)
    }

    //risoluzione swipe left/right
    LaunchedEffect(draggableState.currentValue) {
        if (phase !is GamePhase.KingPending || !enabled) return@LaunchedEffect
        when (draggableState.currentValue) {
            -1 -> {
                model.processIntent(GameIntent.KingDirectionChosen(rowIndex = rowIndex, direction = Direction.LEFT))
                draggableState.animateTo(0) // ritorna al centro dopo lo swipe
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
                    if (phase !is GamePhase.QueenPending || !enabled) return@LaunchedEffect
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
                    //FinalCard(card = card, model = model, size = cardSize, enabled= enabled)
//                    val (cardRow, cardCol) = remember(card.name, grid) {
//                        gridPositionOf(card.name, grid)
//                    }
                    RevealCard(card = card, model = model, size = cardSize, enabled= enabled)
                    //AnimationCard(card=card, model = model, size = cardSize, enabled = enabled,row=cardRow,col=cardCol)
                }
            }
        }
    }
}
// Calcola riga e colonna di una carta nella griglia flat (28 carte, 7 per riga)
fun gridPositionOf(cardName: String, grid: List<CardUIStates>): Pair<Int, Int> {
    val index = grid.indexOfFirst { it.name == cardName }
    return if (index < 0) Pair(0, 0) else Pair(index / 7, index % 7)
}

@Composable
fun FinalCard(card: CardUIStates, model: BaseGameViewModel, size: Dp,enabled:Boolean) {
    //context densità e size in pixel
    val density = LocalDensity.current
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
                        Log.d("grid","fase:${model.uiState.value.phase}")
                        if(model.uiState.value.phase is GamePhase.WaitingForOpponent){
                            scope.launch {
                                SnackBarController.sendEvent(
                                    event = SnackBarEvent(
                                        message = "Attendi che l'avversario finisca il suo turno"
                                    )
                                )
                            }
                            return@detectTapGestures
                        }
                        if (model.uiState.value.phase !is GamePhase.PlayerTurn){
                            scope.launch {
                                SnackBarController.sendEvent(
                                    event = SnackBarEvent(
                                        message = "Risolvi Prima l'effetto della figura"
                                    )
                                )
                            }
                            return@detectTapGestures
                        }
                        if (!card.isRevealed) model.processIntent(GameIntent.CardClicked(cardId = card.name))
                    },
                    onLongPress = {
                        Log.d("grid","fase:${model.uiState.value.phase}")
                        if(model.uiState.value.phase is GamePhase.WaitingForOpponent){
                            scope.launch {
                                SnackBarController.sendEvent(
                                    event = SnackBarEvent(
                                        message = "Attendi che l'avversario finisca il suo turno"
                                    )
                                )
                            }
                            return@detectTapGestures
                        }

                        if (model.uiState.value.phase !is GamePhase.PlayerTurn){
                            scope.launch {
                                SnackBarController.sendEvent(
                                    event = SnackBarEvent(
                                        message = "Risolvi Prima l'effetto della figura"
                                    )
                                )
                            }
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

@Composable
fun RevealCard(card: CardUIStates, model: BaseGameViewModel, size: Dp,enabled:Boolean){
    AnimatedContent(
        targetState = card.isRevealed,
        transitionSpec = {
            (scaleIn(initialScale = 0.75f) + fadeIn(tween(180))) togetherWith
                    (scaleOut(targetScale = 0.75f) + fadeOut(tween(120)))
        },
        label = "card_reveal_${card.name}"
    ) { _ ->
        FinalCard(card = card, model = model,size=size,enabled = true)
    }

}