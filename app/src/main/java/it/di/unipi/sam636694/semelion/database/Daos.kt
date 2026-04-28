package it.di.unipi.sam636694.semelion.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao{
    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM Utenti WHERE userId == :userId")
    suspend fun getUserById(userId: Long): User?

    @Query("SELECT * FROM utenti WHERE  nickName == :nickname")
    suspend fun getUserByNickname(nickname:String): User?

    @Query("SELECT * FROM Utenti")
    fun getAllUsers(): Flow<List<User>>

}

@Dao
interface MatchesDao {

    @Insert
    suspend fun insert(match: Matches): Long

    @Update
    suspend fun update(match: Matches)

    @Delete
    suspend fun delete(match: Matches)

    @Query("SELECT * FROM Partite WHERE matchId = :matchId")
    suspend fun getMatchById(matchId: Long): Matches?

    // per riprendere una partita interrotta
    @Query("SELECT * FROM Partite WHERE matchId IN (SELECT matchId FROM Partecipazioni WHERE userId = :userId)")
    fun getMatchesByUser(userId: Long): Flow<List<Matches>>
}

@Dao
interface ParticipationsDao {

    @Insert
    suspend fun insert(participation: Participations)

    @Delete
    suspend fun delete(participation: Participations)

    @Query("SELECT * FROM Partecipazioni WHERE matchId = :matchId")
    suspend fun getParticipationsByMatch(matchId: Long): List<Participations>

    @Query("SELECT * FROM Partecipazioni WHERE userId = :userId")
    suspend fun getParticipationsByUser(userId: Long): List<Participations>
}

@Dao
interface MatchStatisticsDao {

    @Insert
    suspend fun insert(stats: MatchStatistics)

    @Update
    suspend fun update(stats: MatchStatistics)

    @Query("SELECT * FROM StatistichePartite WHERE matchId = :matchId")
    suspend fun getStatsByMatch(matchId: Long): List<MatchStatistics>

    @Query("SELECT * FROM StatistichePartite WHERE userId = :userId")
    fun getStatsByUser(userId: Long): Flow<List<MatchStatistics>>
}

@Dao
interface PlayerStatisticsDao {

    @Insert
    suspend fun insert(stats: PlayerStatistics)

    @Update
    suspend fun update(stats: PlayerStatistics)

    @Query("SELECT * FROM StatisticheGiocatori WHERE userId = :userId")
    suspend fun getStatsByUser(userId: Long): PlayerStatistics?
}