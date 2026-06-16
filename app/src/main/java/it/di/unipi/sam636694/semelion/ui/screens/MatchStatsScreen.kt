package it.di.unipi.sam636694.semelion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.ui.theme.BackgroundLight
import it.di.unipi.sam636694.semelion.ui.theme.GreenDark
import it.di.unipi.sam636694.semelion.ui.theme.GreenPrimary
import it.di.unipi.sam636694.semelion.ui.theme.LoserTheme
import it.di.unipi.sam636694.semelion.ui.theme.TextDark
import it.di.unipi.sam636694.semelion.ui.theme.TextMuted
import it.di.unipi.sam636694.semelion.ui.theme.WinnerTheme

data class DisplayPlayerStats(
    val name: String,
    val timePlayed: String,
    val figuresRevealed: Int,
    val totalMoves: Int,
    val isFirstPlayer: Boolean,
    val avatarRes: Int? = null,
    val isWinner: Boolean
)

@Composable
fun MatchStatScreen(
    modifier: Modifier = Modifier,
    winnerStats:DisplayPlayerStats,
    loserStats:DisplayPlayerStats,
    onNewGame: () -> Unit,
    onHome:()-> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        //VICTORY banner
        Banner()

        Spacer(modifier = Modifier.height(24.dp))

        //Avatar vincitore
        WinnerAvatar(
            avatarRes = winnerStats.avatarRes,
        )

        Spacer(modifier = Modifier.height(12.dp))

        //winnerName
        Text(
            text = winnerStats.name,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(24.dp))

        //label
        Text(
            text = "PERFORMANCE SUMMARY",
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        //Card vincitore
        PlayerResultCard(winnerStats)

        Spacer(modifier = Modifier.height(12.dp))

        //Card sconfitto
        PlayerResultCard(loserStats)

        Spacer(modifier = Modifier.height(28.dp))

        //pulsanti per uscire
        BottomButtons(
            onNewGame = onNewGame,
            onBack = onHome
        )
    }
}

@Composable
private fun Banner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GreenPrimary)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.winnerBanner),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            color = GreenDark,
            letterSpacing = 3.sp
        )
    }
}

@Composable
private fun WinnerAvatar(avatarRes: Int?) {
    Box(contentAlignment = Alignment.BottomCenter) {
        // Cerchio verde con avatar
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color(0xFFB8E8B8))
                .border(4.dp, GreenPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = avatarRes ?: R.drawable.avatar_1),
                contentDescription = "Avatar vincitore",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
            )
        }

    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    textColor: Color = TextDark,
    labelColor: Color = TextMuted
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = labelColor,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun BottomButtons(
    onNewGame: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onNewGame,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
        ) {
            Text(
                text = "⚔ New Game",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, Color(0xFFCCCCCC))
        ) {
            Text(
                text = "🏠 Back Home",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
        }
    }
}

@Composable
fun PlayerResultCard(
    stats: DisplayPlayerStats,
    modifier: Modifier = Modifier
) {
    val theme = if (stats.isWinner) WinnerTheme else LoserTheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = theme.borderStroke,
        colors = CardDefaults.cardColors(containerColor = theme.containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header: avatar + nome + tempo ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4A5A4A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = stats.avatarRes ?: R.drawable.avatar_1),
                            contentDescription = "${stats.name}'s avatar",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stats.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.nameColor
                        )
                        Text(
                            text = theme.statusText,
                            fontSize = 11.sp,
                            color = theme.statusColor,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TIME PLAYED",
                        fontSize = 10.sp,
                        color = theme.timeLabelColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stats.timePlayed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.timeValueColor
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = theme.dividerColor,
                thickness = 1.dp
            )

            // ── Statistiche ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(
                    label = "FIGURES\nREVEALED",
                    value = "%02d".format(stats.figuresRevealed),
                    textColor = theme.statValueColor,
                    labelColor = theme.statLabelColor
                )
                StatColumn(
                    label = "TOTAL\nMOVES",
                    value = "%02d".format(stats.totalMoves),
                    textColor = theme.statValueColor,
                    labelColor = theme.statLabelColor
                )
                StatColumn(
                    label = "FIRST\nPLAYER",
                    value = if (stats.isFirstPlayer) "Yes" else "No",
                    textColor = theme.statValueColor,
                    labelColor = theme.statLabelColor
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
fun BattleResultScreenPreview() {

    val stats = Pair(
        DisplayPlayerStats(
            name = "Pino",
            timePlayed = "04:12",
            figuresRevealed = 4,
            totalMoves = 10,
            isFirstPlayer = true,
            avatarRes = R.drawable.avatar_3,
            isWinner = true
        ),
        DisplayPlayerStats(
            name = "Pippo",
            timePlayed = "05:45",
            figuresRevealed = 0,
            totalMoves = 12,
            isFirstPlayer = false,
            avatarRes = null,
            isWinner = false,
        )
    )

    MatchStatScreen(
        winnerStats = stats.first,
        loserStats = stats.second,
        onHome = {},
        onNewGame = {}
    )
}
