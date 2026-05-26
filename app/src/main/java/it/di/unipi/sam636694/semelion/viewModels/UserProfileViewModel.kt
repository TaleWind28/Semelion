package it.di.unipi.sam636694.semelion.viewModels

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.database.SemelionDB
import it.di.unipi.sam636694.semelion.database.User
import it.di.unipi.sam636694.semelion.ui.screens.RecentMatch
import it.di.unipi.sam636694.semelion.ui.screens.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale


class UserProfileViewModel(
    val db: SemelionDB,
    val userId: String,
): ViewModel(){
    private val _uiState = MutableStateFlow(ProfileUIState())

    val uiState = _uiState.asStateFlow()

    init{
        viewModelScope.launch {
            val user = db.playerStatisticsDao().getStatsByUser(userId)

            var player = db.userDao().getUserById(userId)
            if (player == null) db.userDao().insert(User(userId = userId, nickName = "Semelion User: $userId", avatar = R.drawable.avatar_1))
            player = db.userDao().getUserById(userId)

            var username =player?.nickName ?: "not in db"


            val matches = db.matchesDao().getMatchesByUser(userId)

            //mappo i match
            val recentMatches = matches.mapNotNull { match ->
                val matchStats = db.matchesDao().getMatchStats(match?.matchId ?: return@mapNotNull null)
                val opponentMatch = matchStats.firstOrNull { it.userId != userId } ?: return@mapNotNull null
                val date = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(Date(opponentMatch.date))
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(opponentMatch.date))
                val opponentName = db.userDao().getUserById(opponentMatch.userId)?.nickName

                RecentMatch(
                    opponent = opponentName ?: opponentMatch.userId,
                    date = date,
                    time = time,
                    isWin = opponentMatch.winner
                )
            }

            val data =
                if (user == null)
                    UserData(userId,0f,0,0,0, losses = 0, draws = 0, wins = 0, selectedAvatar = player!!.avatar)
                else
                    UserData(
                        username = username,
                        winRate=user.matchesWon.toFloat()/user.matchesPlayed.toFloat() * 100,
                        gamesPlayed=user.matchesPlayed,
                        winStreak=user.currentStreak,
                        bestWinStreak=user.bestStreak,
                        wins = user.matchesWon,
                        losses = user.matchesLost,
                        draws = user.matchesDrawn,
                        selectedAvatar = player!!.avatar
                    )

            _uiState.update {
                it.copy(
                    data = data,
                    matches = recentMatches,
                    isDataLoading = false
                )
            }
        }

    }

    suspend fun onEditNickname(nickname:String){
            val user = db.userDao().getUserById(userId) ?: return
            db.userDao().update(User(userId = userId, nickName = nickname, avatar = user.avatar))
            _uiState.update { it.copy(data = it.data.copy(username = nickname)) }
            Log.d("nick","nicck:$nickname")
    }

    suspend fun onEditAvatar(avatar:Int){
            val user = db.userDao().getUserById(userId) ?: return
            db.userDao().update(User(userId = userId, nickName = user.nickName,avatar= avatar))
            _uiState.update { it.copy(user = it.user.copy(avatar=avatar),data = it.data.copy(selectedAvatar = avatar)) }
    }


}

data class ProfileUIState(
    val user: User = User(userId="",nickName="",avatar=R.drawable.avatar_1),
    val data: UserData = UserData(),
    val matches: List<RecentMatch> = emptyList(),
    val isDataLoading: Boolean = true
)