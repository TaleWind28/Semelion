package it.di.unipi.sam636694.semelion

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget

import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.di.unipi.sam636694.semelion.ui.theme.CardUIStates
import it.di.unipi.sam636694.semelion.ui.theme.GameUIState
import androidx.compose.foundation.draganddrop.dragAndDropSource




import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.rotate

import it.di.unipi.sam636694.semelion.R


@Composable
fun SemelionScreen(
    modifier: Modifier = Modifier,
    viewModel: SemelionGameViewModel = viewModel()
){
    val state by viewModel.uiState.collectAsState()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            val turnString = if (state.p1Turn) "Turno del giocatore 1" else "Turno del giocatore 2"
            Text(text = turnString)
            Text(
                text = "il giocatore 2 ha: ${state.p2Actions - state.p2ActionsUsed} azioni Rimanenti",
                modifier = Modifier.align(Alignment.CenterHorizontally).rotate(180f)
            )

            FinalGrid(state = state, model = viewModel)

            Text(
                text = "il giocatore 1 ha: ${state.p1Actions - state.p1ActionsUsed} azioni Rimanenti",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}





@Composable
fun DraggableCard(card: CardUIStates, model:SemelionGameViewModel, size: Dp){
    val imageResId = if (card.isRevealed) (cardImageMap[card.name])?: R.drawable.gray_back else R.drawable.gray_back
    //ancora poco soddisfacente ma può portare gioie
    val interactionModifier = if (card.isRevealed)
        Modifier
            .dragAndDropSource(
                transferData = {
                    return@dragAndDropSource DragAndDropTransferData(
                        clipData = ClipData.newPlainText(card.name, card.name)
                    )


                }
            )
    else
        Modifier
            .clickable{
                model.cardClicked(card.name)
            }


    Image(
        modifier = interactionModifier.size(size).dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event.mimeTypes()
                    .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
            },
            target = remember(card.name) {
                object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        val text = (event.toAndroidDragEvent()
                            .clipData?.getItemAt(0)?.text ?: "") as String
                        model.swapCards(text, card.name)
                        return true
                    }
                }
            }
        ),
        painter = painterResource(id = imageResId),
        contentDescription = "Carta Semelion"
    )


}

@Composable
fun FinalGrid(state: GameUIState, model: SemelionGameViewModel) {

    //griglia di gioco
    Column(
        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally),

    ) {
        //preparazione "misure"
        val rows = state.grid.chunked(7)

        val attentionModifier = Modifier.border(color = Color.Yellow, width = 2.dp, shape = RoundedCornerShape(8.dp))

        //utility per non duplicare codice
        fun playerModifier(isActive: Boolean, color: Color): Modifier =
            if (isActive) attentionModifier.background(color) else Modifier.background(color)

        Column(
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Box(modifier = playerModifier(!state.p1Turn, Color.Red)) {
                CardRows(rows = listOf(rows[0], rows[1]), model = model, rotation = 180f)
            }

            Spacer(modifier = Modifier.size(2.dp))

            Box(modifier = playerModifier(state.p1Turn, Color.Green)) {
                CardRows(rows = listOf(rows[2], rows[3]), model = model, rotation = 0f)
            }
        }
    }
}

@Composable
fun CardRows(rows:List<List<CardUIStates>>,model: SemelionGameViewModel,rotation: Float){
    //preparazione misure
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val labelWidth = 32.dp
    val cardSize = (screenWidthDp - labelWidth*2 ) / 7

    //definizione funzione per evitare duplicazioni di codice
    @Composable
    fun RowLabel(rowOrder: RowOrder, showOn: Float) {
        if (rotation == showOn) {
            Column(
                modifier = Modifier.width(labelWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (arrow, label,color) = when (rowOrder) {
                    RowOrder.CRESCENT ->  Triple("→", "ASC",  Color.Blue)
                    RowOrder.DECRESCENT -> Triple("←", "DESC",  Color.Red)
                    RowOrder.BOTH -> Triple("↔", "BOTH",  Color.Black)
                }

                Text(text = arrow, fontSize = 20.sp, color = color)
                Text(text = label, fontSize = 10.sp, color = color, modifier =  Modifier.rotate(rotation))
            }
        } else {
            Spacer(modifier = Modifier.width(labelWidth))
        }
    }


    Column{
        rows.forEachIndexed { rowIndex, rowItems ->
            val rowOrder = rowItems.getRowOrder(rowIndex)

            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically
            ){

                RowLabel(rowOrder = rowOrder, showOn = 180f)

                rowItems.forEach { card ->
                    DraggableCard(card = card, model = model, size = cardSize)
                }

                RowLabel(rowOrder = rowOrder, showOn = 0f)

            }
        }
    }

}




