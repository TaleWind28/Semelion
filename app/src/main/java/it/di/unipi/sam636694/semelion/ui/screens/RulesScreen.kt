package it.di.unipi.sam636694.semelion.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.di.unipi.sam636694.semelion.utilities.GreenAccent
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.utilities.TextPrimary
import it.di.unipi.sam636694.semelion.utilities.TextSecondary

// ── Colori tema ──────────────────────────────────────────────
private val GreenLight   = Color(0xFFE8F5E9)
private val GreenMedium  = Color(0xFF66BB6A)
private val GreenDark    = Color(0xFF2E7D32)
private val RedText      = Color(0xFFD32F2F)
private val CardBg       = Color(0xFFFFFFFF)
private val PageBg       = Color(0xFFF1F8F1)

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
    }
}

// ════════════════════════════════════════════════════════════
//  HEADER
// ════════════════════════════════════════════════════════════
@Composable
fun RulesHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "MANUALE UFFICIALE",
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
        title = "Scopo del Gioco"
    ) {
        RoundedCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Il giocatore vince la partita quando riesce a comporre 2 \"righe potenti\".",
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
                                text = "RIGA POTENTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Carte dello stesso seme in ordinamento Crescente A-7 oppure Decrescente 7-A.",
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
        iconRes = R.drawable.play_circle_24px, // sostituisci con la tua icona "play"
        title = "Svolgimento del Gioco"
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
                        text = "FORMULA AZIONI TURNO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "(carte in posizione corretta / 2) + 1",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Azione 1: Rivela Carta
            ActionItem(
                iconRes = R.drawable.undereye_24px, // sostituisci con icona "eye"
                title = "Rivela Carta:",
                description = "Scopri una carta coperta sul tavolo.",
                subItems = emptyList()
            )

            // Azione 2: Scambio
            ActionItem(
                iconRes = R.drawable.swap_horiz_24px, // sostituisci con icona "swap"
                title = "Scambio:",
                description = "Scambia due carte rispettando i vincoli:",
                subItems = listOf(
                    SubItem("Almeno una delle due deve andare in posizione corretta.", isRed = false),
                    SubItem("Almeno una delle due deve andare in posizione corretta.", isRed = false),
                    SubItem("Almeno una delle due deve essere nel proprio lato del tavolo.", isRed = false),
                    SubItem("Non possono far parte di una riga potente.", isRed = true)
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
        title = "Posizione Corretta"
    ) {
        RoundedCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Citazione
                Text(
                    text = "\"La posizione di una carta è corretta se è nella posizione pari al suo valore(es: il 3 in posizione 3) in ordine crescente, oppure se è nella corrispettiva in ordine decrescente.\"",
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
                    OrderBox(label = "CRESCENTE", arrow = "A → 7", modifier = Modifier.weight(1f))
                    OrderBox(label = "DECRESCENTE", arrow = "7 → A", modifier = Modifier.weight(1f))
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
        SpecialCard("J", "JACK", "Scambio di un numero random di carte sul tavolo, determinato dal valore della prima carta del mazzo scoperta diminuito di 1."),
        SpecialCard("Q", "QUEEN", "Sposta una colonna verticalmente."),
        SpecialCard("K", "KING", "Sposta una riga orizzontalmente."),
        SpecialCard("★", "JOLLY", "È sempre considerato in posizione, quando compare la carta che sostituisce viene rimosso", isDashed = true),
        SpecialCard("7", "SETTE", "Termina il turno immediatamente se è scoperto e non si trova ai bordi.", isRed = true)
    )

    SectionBlock(
        iconRes = R.drawable.star_shine_24px, // sostituisci con icona "sparkles"
        title = "Carte Speciali"
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