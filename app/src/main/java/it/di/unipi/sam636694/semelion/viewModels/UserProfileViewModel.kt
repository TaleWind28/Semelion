package it.di.unipi.sam636694.semelion.viewModels

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.di.unipi.sam636694.semelion.R
import it.di.unipi.sam636694.semelion.database.MatchStatistics
import it.di.unipi.sam636694.semelion.database.Matches
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
    //stato interno della UI tramite il quale verranno effettuate modifiche
    private val _uiState = MutableStateFlow(ProfileUIState())

    //stato da esporre alla UI
    val uiState = _uiState.asStateFlow()

    init{
        viewModelScope.launch {
            val userStats = db.playerStatisticsDao().getStatsByUser(userId)
            //controllo se esiste un utente associato all'userId locale
            var player = db.userDao().getUserById(userId)
            //se non esiste lo inserisco nel database ed assegno l'utente appena creato alla variabile player
            if (player == null){
                db.userDao().insert(User(userId = userId, nickName = "Semelion User: $userId", avatar = R.drawable.avatar_1))
                player = db.userDao().getUserById(userId)
            }

            //recupero l'username dell'utente
            var username =player?.nickName ?: "Semelion User: $userId"
            //recupero le partite disputate dall'utente locale
            val matches = db.matchesDao().getMatchesByUser(userId)

            //mappo i match
            val recentMatches = matches.mapNotNull { match ->
                val matchStats = db.matchesDao().getMatchStats(match?.matchId ?: return@mapNotNull null)
                val opponentMatch = matchStats.firstOrNull { it.userId != userId } ?: return@mapNotNull null
                mapMatch(opponentMatch= opponentMatch)
            }

            //ottengo dati riguardanti le partite giocate dall'utente
            val data =
                //se l'utente non ha mai disputato partite creo dei dati di default
                if (userStats == null)
                    UserData(
                        username=player!!.nickName, //posso permettermelo perchè sono sicuro che player sia non null
                        winRate=0f,
                        gamesPlayed=0,
                        winStreak=0,
                        bestWinStreak=0,
                        losses = 0,
                        draws = 0,
                        wins = 0,
                        selectedAvatar = player.avatar
                    )
                else
                    UserData(
                        username = player!!.nickName, //posso permettermelo perchè sono sicuro che player sia non null
                        winRate=userStats.matchesWon.toFloat()/userStats.matchesPlayed.toFloat() * 100,
                        gamesPlayed=userStats.matchesPlayed,
                        winStreak=userStats.currentStreak,
                        bestWinStreak=userStats.bestStreak,
                        wins = userStats.matchesWon,
                        losses = userStats.matchesLost,
                        draws = userStats.matchesDrawn,
                        selectedAvatar = player.avatar
                    )

            //aggiorno lo stato della UI per permettere a quest'ultima di far scattare la recomposition
            _uiState.update {
                it.copy(
                    data = data,
                    matches = recentMatches,
                    isDataLoading = false
                )
            }
        }

    }

    //formatto le statistiche riguardanti le partite disputate dall'utente
    suspend fun mapMatch(opponentMatch: MatchStatistics): RecentMatch{
        val date = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(Date(opponentMatch.date))
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(opponentMatch.date))
        val opponent = db.userDao().getUserById(opponentMatch.userId)
        val opponentName = opponent?.nickName
        val opponentAvatar = opponent?.avatar

        Log.d("init","oppo:${opponentName?: opponentMatch.userId}\ndate:$date\ntime:$time\nisWin:${opponentMatch.winner}\n${opponentAvatar ?: R.drawable.avatar_1}")
        return RecentMatch(
            opponent = opponentName ?: opponentMatch.userId,
            date = date,
            time = time,
            isWin = opponentMatch.winner?.not(),
            opponentAvatar = opponentAvatar ?: R.drawable.avatar_1
        )
    }

    //callBack per permettere allìutente di modificare il nome
    suspend fun onEditNickname(nickname:String){
            val user = db.userDao().getUserById(userId) ?: return
            db.userDao().update(User(userId = userId, nickName = nickname, avatar = user.avatar))
            _uiState.update { it.copy(data = it.data.copy(username = nickname)) }
            Log.d("nick","nicck:$nickname")
    }

    //callback per permettere all'utente di modificare l'avatar
    suspend fun onEditAvatar(avatar:Int){
            val user = db.userDao().getUserById(userId) ?: return
            db.userDao().update(User(userId = userId, nickName = user.nickName,avatar= avatar))
            _uiState.update { it.copy(user = it.user.copy(avatar=avatar),data = it.data.copy(selectedAvatar = avatar)) }
    }

}

//data class che espone informazioni alla UI
data class ProfileUIState(
    val user: User = User(userId="",nickName="",avatar=R.drawable.avatar_1),
    val data: UserData = UserData(),
    val matches: List<RecentMatch> = emptyList(),
    val isDataLoading: Boolean = true
)