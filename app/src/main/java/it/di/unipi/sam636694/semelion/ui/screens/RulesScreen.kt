package it.di.unipi.sam636694.semelion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.di.unipi.sam636694.semelion.utilities.GreenAccent
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.ui.theme.CardBg
import it.di.unipi.sam636694.semelion.ui.theme.GreenDark
import it.di.unipi.sam636694.semelion.ui.theme.GreenLight
import it.di.unipi.sam636694.semelion.ui.theme.GreenMedium
import it.di.unipi.sam636694.semelion.ui.theme.PageBg
import it.di.unipi.sam636694.semelion.ui.theme.RedText
import it.di.unipi.sam636694.semelion.utilities.TextPrimary
import it.di.unipi.sam636694.semelion.utilities.TextSecondary

// ── Colori tema ──────────────────────────────────────────────


// ════════════════════════════════════════════════════════════
//  SCHERMATA PRINCIPALE
// ════════════════════════════════════════════════════════════
@Composable
fun SemelionRules(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { RulesHeader() }
        item { Purpose() }
        item { HowToPlay() }
        item { ValidPosition() }
        item { SpecialCards() }
        item { UncoverDeck() }
    }
}

// ════════════════════════════════════════════════════════════
//  HEADER
// ════════════════════════════════════════════════════════════
@Composable
fun RulesHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.semelionManual),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            color = GreenDark
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Semelion",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
    }
}

// ════════════════════════════════════════════════════════════
//  SCOPO DEL GIOCO
// ════════════════════════════════════════════════════════════
@Composable
fun Purpose() {
    SectionBlock(
        iconRes = R.drawable.stars_24px, // sostituisci con la tua icona "target"
        title = stringResource(R.string.semelionPurpose)
    ) {
        RoundedCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.semelionWinCon),
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                // Box definizione "riga potente"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = GreenAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.semelionPowerRow),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.semelionPowerRowPosition),
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  SVOLGIMENTO DEL GIOCO
// ════════════════════════════════════════════════════════════
@Composable
fun HowToPlay() {
    SectionBlock(
        iconRes = R.drawable.play_circle_24px,
        title = stringResource(R.string.semelionPlaying)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Formula azioni turno
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenAccent)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text =stringResource(R.string.semelionActionsTitle),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text =stringResource(R.string.semelionActionsFormula),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Azione 1: Rivela Carta
            ActionItem(
                iconRes = R.drawable.undereye_24px,
                title = stringResource(R.string.semelionRevealTitle),
                description = stringResource(R.string.semelionRevealAction),
                subItems = emptyList()
            )

            // Azione 2: Scambio
            ActionItem(
                iconRes = R.drawable.swap_horiz_24px,
                title = stringResource(R.string.semelionSwapTitle),
                description = stringResource(R.string.semelionSwapAction),
                subItems = listOf(
                    SubItem(stringResource(R.string.semelionSwapCorrectnessRule), isRed = false),
                    SubItem(stringResource(R.string.semelionSwapFairnessRule), isRed = false),
                    SubItem(stringResource(R.string.semelionSwapPowerRowRule), isRed = true)
                )
            )
        }
    }
}

data class SubItem(val text: String, val isRed: Boolean)

@Composable
fun ActionItem(
    iconRes: Int,
    title: String,
    description: String,
    subItems: List<SubItem>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = GreenDark,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = buildAnnotatedString {
                    append("$title ")
                    append(description)
                },
                fontSize = 14.sp,
                color = TextPrimary
            )
            subItems.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = if (item.isRed) Icons.Outlined.Delete else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (item.isRed) RedText else GreenMedium,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = item.text,
                        fontSize = 13.sp,
                        color = if (item.isRed) RedText else TextPrimary,
                        fontWeight = if (item.isRed) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  POSIZIONE CORRETTA
// ════════════════════════════════════════════════════════════
@Composable
fun ValidPosition() {
    SectionBlock(
        iconRes = R.drawable.info_24px, // sostituisci con icona "grid"
        title = stringResource(R.string.semelionCorrectPositionTitle)
    ) {
        RoundedCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Citazione
                Text(
                    text = stringResource(R.string.semelionCorrectPositionCitation),
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                // Frecce ascendente / discendente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrderBox(label = stringResource(R.string.semelionCorrectPositionOrderAscending), arrow = stringResource(R.string.semelionCorrectPositionOrderAscendingArrow), modifier = Modifier.weight(1f))
                    OrderBox(label = stringResource(R.string.semelionCorrectPositionOrderDescending), arrow = stringResource(R.string.semelionCorrectPositionOrderDescendingArrow), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun OrderBox(label: String, arrow: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.5.dp, Color(0xFFBDBDBD), RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = arrow,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  CARTE SPECIALI
// ════════════════════════════════════════════════════════════
data class SpecialCard(
    val letter: String,
    val name: String,
    val description: String,
    val isDashed: Boolean = false,
    val isRed: Boolean = false
)

@Composable
fun SpecialCards() {
    val cards = listOf(
        SpecialCard("J", stringResource(R.string.semelionJack), stringResource(R.string.semelionJackEffect)),
        SpecialCard("Q", stringResource(R.string.semelionQueen), stringResource(R.string.semelionQueenEffect)),
        SpecialCard("K", stringResource(R.string.semelionKing), stringResource(R.string.semelionKingEffect)),
        SpecialCard("★", stringResource(R.string.semelionJolly), stringResource(R.string.semelionJollyEffect), isDashed = true),
        SpecialCard("7", stringResource(R.string.semelionSeven), stringResource(R.string.semelionSevenEffect), isRed = true)
    )

    SectionBlock(
        iconRes = R.drawable.star_shine_24px, // sostituisci con icona "sparkles"
        title = stringResource(R.string.semelionSpecialCards)
    ) {
        RoundedCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                cards.forEach { card ->
                    SpecialCardRow(card)
                    if (card != cards.last()) {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialCardRow(card: SpecialCard) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Badge carta
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (card.isDashed)
                        Modifier.border(2.dp, Color(0xFF9E9E9E), RoundedCornerShape(8.dp))
                    else if (card.isRed)
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, RedText, RoundedCornerShape(8.dp))
                    else
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenDark)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = card.letter,
                fontSize = if (card.letter.length == 1) 22.sp else 16.sp,
                fontWeight = FontWeight.Black,
                color = when {
                    card.isRed  -> RedText
                    card.isDashed -> Color(0xFF757575)
                    else        -> Color.White
                }
            )
        }

        // Testo
        Column {
            Text(
                text = card.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (card.isRed) RedText else GreenDark
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = card.description,
                fontSize = 13.sp,
                color = TextPrimary
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  COMPONENTI HELPER
// ════════════════════════════════════════════════════════════

/** Blocco sezione con icona + titolo verde e contenuto */
@Composable
fun SectionBlock(
    iconRes: Int,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Intestazione sezione
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = GreenDark,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GreenDark
            )
        }
        content()
    }
}

/** Card bianca con angoli arrotondati e leggera ombra */
@Composable
fun RoundedCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun UncoverDeck(){
    SectionBlock(
        iconRes = R.drawable.play_circle_24px,
        title = stringResource(R.string.semelionRulesUncoverDeckTitle)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Formula azioni turno
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenAccent)
                    .padding(14.dp)
            ) {
                Column{
                    Text(
                        text =stringResource(R.string.semelionRulesUncoverDeckComposition),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text =stringResource(R.string.semelionRulesUncoverDeck),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

        }
    }
}
