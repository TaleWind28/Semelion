package it.di.unipi.sam636694.semelion.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.di.unipi.sam636694.semelion.R

// ── Colori ───────────────────────────────────────────────────
private val GreenPrimary   = Color(0xFF3DBE5A)
private val GreenDark      = Color(0xFF1F6B32)
private val GreenLight     = Color(0xFFE8F5E9)
private val GreenCard      = Color(0xFFDCF0DC)
private val BgPage         = Color(0xFFF0F7F0)
private val CardBg         = Color(0xFFECF4EC)
private val TextPrimary    = Color(0xFF1A1A1A)
private val TextSecondary  = Color(0xFF6B6B6B)
private val WinColor       = Color(0xFF2E7D32)
private val LossColor      = Color(0xFFC62828)
private val OrangeAccent   = Color(0xFFBF6020)
 
// ── Mock Data ─────────────────────────────────────────────────
data class UserProfile(
    val username: String,
    val role: String,
    val joinDate: String,
    val winRate: Int,
    val gamesPlayed: Int,
    val righePotenti: Int,
    val currentRank: String
)

data class UserData(
    val username:String,
    val winRate: Float,
    val gamesPlayed: Int,
    val winStreak: Int,
    val bestWinStreak:Int,
    val losses: Int,
    val wins: Int,
    val draws: Int
)

val mockProfile = UserData(
    username = "Tactician_Semelion",
    winRate = 64f,
    gamesPlayed = 128,
    winStreak = 40,
    bestWinStreak = 50,
    wins = 0,
    losses = 80,
    draws = 50
)
 
data class RecentMatch(
    val opponent: String,
    val date: String,
    val time: String,
    val isWin: Boolean,
    val rankChange: Int
)

val mockMatches = listOf(
    RecentMatch("Shadow_Pulse",  "Nov 24, 2023", "14:20", isWin = true,  rankChange = 15),
    RecentMatch("Card_King_99",  "Nov 23, 2023", "09:15", isWin = false, rankChange = -12),
    RecentMatch("Neon_Striker",  "Nov 22, 2023", "18:45", isWin = true,  rankChange = 18),
    RecentMatch("Ghost_Dealer",  "Nov 21, 2023", "11:30", isWin = true,  rankChange = 10),
)
 
// ════════════════════════════════════════════════════════════
//  SCHERMATA PROFILO
// ════════════════════════════════════════════════════════════
@Composable
fun ProfilePage(
    profile: UserData = mockProfile,
    matches: List<RecentMatch> = mockMatches,
    onEditProfile: (String) -> Unit = {},
    onViewAllMatches: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ProfileCard(profile = profile, onEdit = onEditProfile) }
        item { StatisticsSection(profile = profile) }
        item { RecentMatchesSection(matches = matches, onViewAll = onViewAllMatches) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
 
// ════════════════════════════════════════════════════════════
//  CARD PROFILO
// ════════════════════════════════════════════════════════════
@Composable
fun ProfileCard(profile: UserData, onEdit: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf(profile.username) }
    Log.d("nick","nick:$nicknameInput")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GreenLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Avatar con bordo verde e pulsante edit
            Box(contentAlignment = Alignment.BottomEnd) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, GreenPrimary, CircleShape)
                        .background(Color(0xFF2A3A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    // Sostituisci con AsyncImage o Image se hai una foto profilo
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Avatar",
                        tint = GreenPrimary,
                        modifier = Modifier.size(60.dp)
                    )
                }
 
                // Pulsante edit avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Modifica avatar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
 
            Spacer(Modifier.height(4.dp))

            if (isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            onEdit(nicknameInput)
                            isEditing = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Conferma")
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.username,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifica username", tint = TextSecondary)
                    }
                }

            // Nome utente + edit
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(6.dp)
//            ) {
//                Text(
//                    text = profile.username,
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = TextPrimary
//                )
//                IconButton(
//                    onClick = onEdit,
//                    modifier = Modifier.size(24.dp)
//                ) {
//                    Icon(
//                        imageVector = Icons.Filled.Edit,
//                        contentDescription = "Modifica username",
//                        tint = TextSecondary,
//                        modifier = Modifier.size(16.dp)
//                    )
//                }
            }
        }
    }
}
 
// ════════════════════════════════════════════════════════════
//  SEZIONE STATISTICHE
// ════════════════════════════════════════════════════════════
@Composable
fun StatisticsSection(profile: UserData) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Statistics",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        // Riga 1: Win Rate + Games Played
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "WIN RATE",
                value = "${profile.winRate}%",
                valueColor = GreenDark,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "GAMES PLAYED",
                value = "${profile.gamesPlayed}",
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Riga 1: Win Rate + Games Played
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "MATCHES WON",
                value = "${profile.wins}",
                valueColor = GreenDark,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                label = "MATCHES DRAWN",
                value = if (profile.draws < 0) "${profile.draws * -1}" else "${profile.draws}",
                valueColor = Color.DarkGray,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                label = "MATCHES LOST",
                value = "${profile.losses}",
                valueColor = Color.Red,
                modifier = Modifier.weight(1f)
            )
        }
 
        // Riga 2: Win Streak Attuale + Win Streak Migliore
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "CURRENT WIN STREAK",
                value = "${profile.winStreak}",
                valueColor = OrangeAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "BEST WIN STREAK",
                value = "${profile.bestWinStreak}",
                valueColor = Color.Green,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
 
@Composable
fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
 
@Composable
fun RankCard(rank: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GreenCard,
        modifier = modifier.height(110.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                Text(
                    text = "CURRENT RANK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = GreenDark
                )
                Text(
                    text = rank,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark,
                    lineHeight = 26.sp
                )
            }
            // Icona trofeo sfumata in basso a destra
            Icon(
                painter = painterResource(id = R.drawable.leaderboard_24px), // sostituisci con icona trofeo
                contentDescription = null,
                tint = GreenPrimary.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
 
// ════════════════════════════════════════════════════════════
//  SEZIONE PARTITE RECENTI
// ════════════════════════════════════════════════════════════
@Composable
fun RecentMatchesSection(
    matches: List<RecentMatch>,
    onViewAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Matches",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            TextButton(onClick = onViewAll) {
                Text(
                    text = "VIEW ALL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark,
                    letterSpacing = 0.5.sp
                )
            }
        }
 
        // Lista partite
        matches.forEach { match ->
            MatchRow(match = match)
        }
    }
}
 
@Composable
fun MatchRow(match: RecentMatch) {
    val accentColor = if (match.isWin) GreenPrimary else LossColor
 
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra laterale colorata
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(accentColor)
            )
 
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar avversario
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (match.isWin) GreenPrimary else Color(0xFFBDBDBD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
 
                // Nome e data
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "vs ${match.opponent}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${match.date} • ${match.time}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
 
                // Risultato
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (match.isWin) "WIN" else "LOSS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "${if (match.rankChange > 0) "+" else ""}${match.rankChange} Rank",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}