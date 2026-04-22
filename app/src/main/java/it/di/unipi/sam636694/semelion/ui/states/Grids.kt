package it.di.unipi.sam636694.semelion.ui.states

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Canvas
import android.graphics.Point
import android.util.Log
import android.view.View
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.RowOrder
import it.di.unipi.sam636694.semelion.SemelionGameViewModel
import it.di.unipi.sam636694.semelion.utilities.SnackBarController
import it.di.unipi.sam636694.semelion.utilities.SnackBarEvent
import it.di.unipi.sam636694.semelion.cardImageMap
import it.di.unipi.sam636694.semelion.getRowOrder
import kotlinx.coroutines.launch

@Composable
fun FinalGrid(state: GameUIState, model: SemelionGameViewModel) {
    //griglia di gioco
    Column(
        modifier = Modifier.fillMaxWidth(),
        ) {
        //preparazione "misure"
        val rows = state.grid.chunked(7)

        val attentionModifier = Modifier.border(
            color = Color(0xFF3BFF7C),
            width = 6.dp,
            shape = RoundedCornerShape(8.dp)
        )

        //utility per non duplicare codice
        fun playerModifier(isActive: Boolean): Modifier =
            if (isActive) attentionModifier else Modifier

        val playerConfigs = listOf(
            Triple(!state.p1Turn, listOf(rows[0], rows[1]), Pair(Color(0xFF009688), 180f)),
            Triple(state.p1Turn, listOf(rows[2], rows[3]), Pair(Color(0xFF9C27B0), 0f))
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            playerConfigs.forEachIndexed { index, (isActive, playerRows, style) ->
                Log.d("SemelionScreen","$index")
                if (index > 0) Spacer(modifier = Modifier.size(8.dp))

                Box(modifier = playerModifier(isActive)) {
                    Column(verticalArrangement = Arrangement.SpaceEvenly) {
                        playerRows.forEachIndexed { rowIndex, rowItems ->
                            CardRow(
                                rowIndex = rowIndex + (index * 2),
                                rowItems = rowItems,
                                model = model,
                                rowBackground = style.first.copy(alpha = if (index == 0) 0.15f else 0.08f),
                                phase = model.uiState.value.phase
                            )
                        }
                    }
                }
            }
        }
    }
}


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CardRow(rowIndex: Int, rowItems: List<CardUIStates>, model: SemelionGameViewModel, rowBackground: Color, phase: GamePhase) {
    //preparazione misure
    var cardSize by remember{ mutableStateOf(48.dp)}
    val density = LocalDensity.current

    val draggableState = remember {
        AnchoredDraggableState(initialValue = 0)
    }

    //risoluzione swipe left/right
    LaunchedEffect(draggableState.currentValue) {
        if (phase !is GamePhase.KingPending) return@LaunchedEffect
        when (draggableState.currentValue) {
            -1 -> {
                model.processIntent(GameIntent.KingDirectionChosen(rowIndex = rowIndex){ i:Int, inc:Int -> rowIndex*7 + i + inc})
                draggableState.animateTo(0) // ritorna al centro dopo lo swipe
            }
            1 -> {
                model.processIntent(GameIntent.KingDirectionChosen(rowIndex = rowIndex){ i:Int, inc:Int -> 7*rowIndex + (6-i) - inc})
                draggableState.animateTo(0)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = rowBackground,
        shadowElevation = 2.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    with(density) {
                        val totalSpacingPx = 0.50.dp.toPx() * 6  // 6 gap tra 7 carte
                        cardSize = ((size.width - totalSpacingPx) / 7).toDp()
                    }

                    draggableState.updateAnchors(
                    DraggableAnchors {
                        (-1) at -size.width.toFloat()
                        0 at 0f
                        1 at size.width.toFloat()
                    }
                ) }
                .anchoredDraggable(draggableState, Orientation.Horizontal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val columnSwipableStates = remember {
                List(rowItems.size){AnchoredDraggableState(0)}
            }

            rowItems.forEachIndexed { itemIndex, card ->

                val colState = columnSwipableStates[itemIndex]
                //risoluzione swipe up/down
                LaunchedEffect(colState.currentValue) {
                    if (phase !is GamePhase.QueenPending) return@LaunchedEffect
                    when (colState.currentValue) {
                        -1 -> {
                            model.processIntent(GameIntent.QueenDirectionChosen { i, inc -> itemIndex + 7 * i + inc })
                            colState.animateTo(0)
                        }
                        1 -> {
                            model.processIntent(GameIntent.QueenDirectionChosen { i, inc -> itemIndex + 7 * (3 - i) - inc })
                            colState.animateTo(0)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .onSizeChanged { size ->
                            colState.updateAnchors(
                                DraggableAnchors {
                                    (-1) at -size.height.toFloat()
                                    0 at 0f
                                    1 at size.height.toFloat()
                                }
                            )
                        }
                        .anchoredDraggable(colState, orientation = Orientation.Vertical)
                ) {

                    // CARTE
                    FinalCard(card = card, model = model, size = cardSize)

                }
            }
        }
    }
}
@Composable
fun FinalCard(card: CardUIStates, model: SemelionGameViewModel, size: Dp) {
    //context densità e size in pixel
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx().toInt()}

    val imageResId = if (card.isRevealed)
        (cardImageMap[card.name]) ?: R.drawable.purple_back
    else
        R.drawable.purple_back

    val view = LocalView.current

    val imageBitmap = ImageBitmap.imageResource(id = imageResId)

    val scope = rememberCoroutineScope()

    Image(
        modifier = Modifier
            .size(size)
            .pointerInput(card.name, card.isRevealed) {
                detectTapGestures(
                    onTap = {
                        if (!card.isRevealed) model.processIntent(GameIntent.CardClicked(cardId = card.name))
                    },
                    onLongPress = {
                        if (model.uiState.value.phase !is  GamePhase.PlayerTurn){
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
            ),
        painter = painterResource(id = imageResId),
        contentDescription = "Carta Semelion"
    )
}