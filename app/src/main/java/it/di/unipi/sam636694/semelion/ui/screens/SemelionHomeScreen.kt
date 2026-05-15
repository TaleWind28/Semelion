package it.di.unipi.sam636694.semelion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.NavKey
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.Route

// ── Colori ───────────────────────────────────────────────────
private val GreenPrimary  = Color(0xFF3DBE5A)
private val GreenDark     = Color(0xFF1F6B32)
private val GreenLight    = Color(0xFFEAF4EA)
private val BgPage        = Color(0xFFF0F7F0)
private val CardBg        = Color(0xFFE8F0E8)
private val TextPrimary   = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B6B6B)
private val CardBlue      = Color(0xFF2B4FC7)
private val CardRed       = Color(0xFFC72B2B)
private val CardWhite     = Color(0xFFFFFFFF)

// ════════════════════════════════════════════════════════════
//  SCHERMATA PRINCIPALE
// ════════════════════════════════════════════════════════════
@Composable
fun SemelionHome(
    modifier: Modifier = Modifier,
    destinations: Map<String, () -> Unit>,
    navigationFun: (route: NavKey) -> Unit
) {
    val buttonList = listOf(
        IconButton(
            "Regole",
            "Master the Tactical mechanics",
            "icona di regole",
            R.drawable.article_24px,
            goTo = { navigationFun(Route.RulesPage) }
        ),
        IconButton(
            "Profilo",
            "Game preferences & Audio",
            "icona impostazioni",
            R.drawable.account_circle_24px, // sostituisci con icona settings
            goTo = { navigationFun(Route.ProfilePage) }
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPage)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GreetingsBox(onQuickPlay = destinations["Quick Play"] ?: {}, onConnections = destinations["Connections"] ?: {})
        Spacer(Modifier.height(4.dp))
        buttonList.forEach { button ->
            DestinationButton(button = button)
        }
    }
}

// ════════════════════════════════════════════════════════════
//  GREETINGS BOX — Hero card con carte e Quick Play
// ════════════════════════════════════════════════════════════
@Composable
fun GreetingsBox(
    modifier: Modifier = Modifier,
    onQuickPlay: () -> Unit,
    onConnections: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GreenLight,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Testo benvenuto
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "WELCOME BACK",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = GreenDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ready for your\nnext turn?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 34.sp
                )
            }

            // Tre carte a ventaglio
            CardFanDisplay()

            // Pulsante Quick Play
            Button(
                onClick = onQuickPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Quick Play",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Pulsante Quick Play
            Button(
                onClick = onConnections,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Connections",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }


        }
    }
}

// ════════════════════════════════════════════════════════════
//  TRE CARTE A VENTAGLIO
// ════════════════════════════════════════════════════════════
@Composable
fun CardFanDisplay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Carta sinistra — blu, ruotata a sinistra
        PlayingCard(
            color = CardBlue,
            modifier = Modifier
                .offset(x = (-70).dp, y = 10.dp)
                .rotate(-15f)
                .zIndex(1f)
        )
        // Carta destra — rossa, ruotata a destra
        PlayingCard(
            color = CardRed,
            modifier = Modifier
                .offset(x = 70.dp, y = 10.dp)
                .rotate(15f)
                .zIndex(1f)
        )
        // Carta centrale — bianca, in primo piano
        PlayingCard(
            color = CardWhite,
            isCenter = true,
            modifier = Modifier
                .zIndex(2f)
        )
    }
}

@Composable
fun PlayingCard(
    color: Color,
    isCenter: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isWhite = color == CardWhite
    val dotColor = if (isWhite) Color.Transparent else Color.White.copy(alpha = 0.25f)
    val textColor = if (isWhite) GreenDark else Color.White

    Box(
        modifier = modifier
            .width(110.dp)
            .height(155.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .then(
                if (isCenter) Modifier.border(2.dp, GreenPrimary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .padding(10.dp)
    ) {
        if (!isWhite) {
            // Pattern puntini per le carte colorate
            DotPattern()
        }

        if (isCenter) {
            // Carta centrale: mostra "A" + foglia
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("A", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("∂", fontSize = 10.sp, color = textColor)
                }
                Icon(
                    painter = painterResource(id = R.drawable.article_24px), // sostituisci con foglia
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text("∂", fontSize = 10.sp, color = textColor)
                    Text("A", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }
    }
}

@Composable
fun DotPattern() {
    // Pattern puntini semplice con Box griglia
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  DESTINATION BUTTON — restyled
// ════════════════════════════════════════════════════════════
data class IconButton(
    val title: String,
    val subtitle: String,
    val contentDescription: String,
    val resId: Int,
    val goTo: () -> Unit
)

@Composable
fun DestinationButton(modifier: Modifier = Modifier, button: IconButton) {
    Surface(
        onClick = { button.goTo() },
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icona in box bianco
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(id = button.resId),
                        contentDescription = button.contentDescription,
                        tint = GreenDark,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Testo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = button.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = button.subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // Freccia destra
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}