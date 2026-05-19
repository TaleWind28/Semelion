package it.di.unipi.sam636694.semelion.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao{
    @Insert
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM Utenti WHERE userId == :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM Utenti WHERE  nickName == :nickname")
    suspend fun getUserByNickname(nickname:String): User?

    @Query("SELECT * FROM Utenti")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT COUNT(*) FROM Utenti")
    suspend fun getUserCount(): Int

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

    @Query("SELECT * FROM Partite WHERE isCompleted=:completion")
    suspend fun getSuspendedMatch(completion: Boolean= false): Matches?

    @Query("SELECT COALESCE(MAX(matchId), 0) + 1 FROM Partite")
    suspend fun getNextMatchId(): Long

    // per riprendere una partita interrotta
    @Query("SELECT * FROM Partite WHERE matchId IN (SELECT matchId FROM Partecipazioni WHERE userId = :userId)")
    suspend fun getMatchesByUser(userId: String): List<Matches?>

    @Query("SELECT *" +
            "FROM Partite m " +
            "JOIN STATISTICHEPARTITE ms ON ms.matchId = m.matchId" +
            " WHERE m.matchId =:matchId"
    )
    suspend fun getMatchStats(matchId:Long):List<MatchStatistics>

    @Query("SELECT COUNT(*) FROM Partite WHERE isCompleted=:completion")
    suspend fun getSuspendedCount(completion: Boolean=false): Int

    @Query("DELETE FROM Partite WHERE Partite.matchId != (SELECT MAX(Partite.matchId) FROM Partite)")
    suspend fun deleteAllExceptLast(): Int
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
    suspend fun getParticipationsByUser(userId: String): List<Participations>
}

@Dao
interface MatchStatisticsDao {

    @Insert
    suspend fun insert(stats: MatchStatistics)

    @Update
    suspend fun update(stats: MatchStatistics)

    @Upsert
    suspend fun upsert(stats: MatchStatistics)

    @Query("SELECT * FROM StatistichePartite WHERE matchId = :matchId")
    suspend fun getStatsByMatch(matchId: Long): List<MatchStatistics>

    @Query("SELECT * FROM StatistichePartite WHERE userId = :userId")
    fun getStatsByUser(userId: String): MatchStatistics

    @Query(value =
        "SELECT * " +
            " FROM UTENTI u JOIN PARTECIPAZIONI p on p.userId = u.userId " +
            " JOIN PARTITE m on p.matchId = m.matchId " +
            " JOIN StatistichePartite st on st.matchId = m.matchId" +
        " WHERE m.matchId = :matchId"
    )
    suspend fun getPlayerStatsFromMatch(matchId: Long): List<MatchStatistics?>
}

@Dao
interface PlayerStatisticsDao {

    @Insert
    suspend fun insert(stats: PlayerStatistics)

    @Update
    suspend fun update(stats: PlayerStatistics)

    @Query("SELECT * FROM StatisticheGiocatori WHERE userId = :userId")
    suspend fun getStatsByUser(userId: String): PlayerStatistics?

}