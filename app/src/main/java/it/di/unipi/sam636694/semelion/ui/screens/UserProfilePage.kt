package it.di.unipi.sam636694.semelion.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.ui.theme.BgPage
import it.di.unipi.sam636694.semelion.ui.theme.CardBg
import it.di.unipi.sam636694.semelion.ui.theme.DrawColor
import it.di.unipi.sam636694.semelion.ui.theme.GreenDark
import it.di.unipi.sam636694.semelion.ui.theme.GreenLight
import it.di.unipi.sam636694.semelion.ui.theme.GreenPrimary
import it.di.unipi.sam636694.semelion.ui.theme.LossColor
import it.di.unipi.sam636694.semelion.ui.theme.OrangeAccent
import it.di.unipi.sam636694.semelion.ui.theme.TextPrimary
import it.di.unipi.sam636694.semelion.ui.theme.TextSecondary
import it.di.unipi.sam636694.semelion.ui.theme.WinColor
import it.di.unipi.sam636694.semelion.utilities.avatars
import kotlin.math.roundToInt

data class UserData(
    val username:String = "",
    val winRate: Float = 0f,
    val gamesPlayed: Int = 0,
    val winStreak: Int = 0,
    val bestWinStreak:Int = 0,
    val losses: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val selectedAvatar: Int = R.drawable.avatar_1
)

data class RecentMatch(
    val opponent: String,
    val date: String,
    val time: String,
    val isWin: Boolean?,
    val opponentAvatar:Int = R.drawable.avatar_1,
)
 
// ════════════════════════════════════════════════════════════
//  SCHERMATA PROFILO
// ════════════════════════════════════════════════════════════
@Composable
fun ProfilePage(
    profile: UserData = UserData(),
    matches: List<RecentMatch> = emptyList(),
    onEditProfile: (String) -> Unit = {},
    onAvatarChosen: (Int) -> Unit = {},
    onViewAllMatches: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ProfileCard(profile = profile, onEdit = onEditProfile, onAvatarChosen = onAvatarChosen) }
        item { StatisticsSection(profile = profile) }
        item { RecentMatchesSection(matches = matches, onViewAll = onViewAllMatches) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
 
// ════════════════════════════════════════════════════════════
//  CARD PROFILO
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCard(profile: UserData, onEdit: (String) -> Unit,onAvatarChosen: (Int) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf(profile.username) }
    var editAvatar by remember { mutableStateOf(false) }



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
                    Image(painter = painterResource(profile.selectedAvatar), contentDescription = "Semelion Avatar")
                }
 
                // Pulsante edit avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary)
                        .clickable(onClick={ editAvatar = true})
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Cambia Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (editAvatar) {
                BasicAlertDialog(onDismissRequest = { editAvatar = false }) {

                    Surface(color = Color.White) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(text = stringResource(R.string.semelionProfileChooseAvatar), modifier = Modifier.size(60.dp), color = Color.Black)
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                items(avatars) { res ->
                                    Image(
                                        painter = painterResource(res),
                                        contentDescription = "Semelion_Avatar",
                                        modifier = Modifier.clickable(
                                            onClick = {
                                                onAvatarChosen(res)
                                                editAvatar = false
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
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
            text = stringResource(R.string.semelionProfileStatistics),
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
                label = stringResource(R.string.semelionProfileWinRate),
                value = "${profile.winRate.roundToInt()}%",
                valueColor = GreenDark,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.semelionProfileMatchesPlayed),
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
                label = stringResource(R.string.semelionProfileMatchesWon),
                value = "${profile.wins}",
                valueColor = WinColor,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                label = stringResource(R.string.semelionProfileMatchesDrawn),
                value = if (profile.draws < 0) "${profile.draws * -1}" else "${profile.draws}",
                valueColor = DrawColor,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                label = stringResource(R.string.semelionProfileMatchesLost),
                value = "${profile.losses}",
                valueColor = LossColor,
                modifier = Modifier.weight(1f)
            )
        }
 
        // Riga 2: Win Streak Attuale + Win Streak Migliore
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = stringResource(R.string.semelionProfileCurrentStreak),
                value = "${profile.winStreak}",
                valueColor = OrangeAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.semelionProfileBestStreak),
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
                text = stringResource(R.string.semelionProfileRecentMatches),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
 
        // Lista partite
        matches.forEach { match ->
            MatchRow(match = match)
        }
    }
}
 
@Composable
fun MatchRow(match: RecentMatch) {
    val accentColor =
        if (match.isWin == null) DrawColor
        else if (match.isWin) WinColor
        else LossColor

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
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                   Image(painterResource(match.opponentAvatar), contentDescription = "Opponent Avatar")
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
                        text = if (match.isWin == true) stringResource(R.string.semelionProfileWin) else if (match.isWin == false) stringResource(R.string.semelionProfileLoss) else stringResource(R.string.semelionProfileDraw),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}