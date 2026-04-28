package it.di.unipi.sam636694.semelion.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import it.di.unipi.sam636694.semelion.ui.states.GameUIState

@Entity(tableName = "Partite")
data class Matches(
    @PrimaryKey(autoGenerate = true)
    val matchId: Long= 0,
    val gameMode: GameModes,
    val gameState: GameUIState
)

@Entity(tableName = "Utenti")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0,
    val nickName: String,
)

@Entity(tableName = "Partecipazioni", primaryKeys = ["matchId","userId"], foreignKeys =[
    ForeignKey(
        entity = Matches::class,
        parentColumns = ["matchId"],
        childColumns = ["matchId"],
        onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )
],
    indices = [Index("matchId"), Index("userId")]                                                                                                                           )
data class Participations(
    val matchId: Long= 0,
    val userId: Long= 0,
    val role: String
)

@Entity(tableName = "StatistichePartite", primaryKeys = ["matchId","userId"], foreignKeys =[
    ForeignKey(
        entity = Matches::class,
        parentColumns = ["matchId"],
        childColumns = ["matchId"],
        onDelete = ForeignKey.CASCADE
    ),                                      
    ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )
],
    indices = [Index("matchId"), Index("userId")])
data class MatchStatistics(
    val userId: Long= 0,
    val matchId: Long= 0,
    val outcome: String,
    val winner: String?,
    val figureRevealed:Int,
    val totalActions: Int,
)

@Entity(tableName = "StatisticheGiocatori", foreignKeys = [ForeignKey(
    entity = User::class,
    parentColumns = ["userId"],
    childColumns = ["userId"],
    onDelete = ForeignKey.CASCADE
)])
data class PlayerStatistics(
    @PrimaryKey(autoGenerate = true)
    val userId: Long= 0,
    val matchesPlayed: Int,
    val matchesWon: Int,
    val matchesLost: Int
)

enum class GameModes{
    ScreenSharing,
    P2P
}

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromGameUIState(value: GameUIState): String = gson.toJson(value)

    @TypeConverter
    fun toGameUIState(value: String): GameUIState = gson.fromJson(value, GameUIState::class.java)

    @TypeConverter
    fun fromGameModes(value: GameModes): String = value.name

    @TypeConverter
    fun toGameModes(value: String): GameModes = GameModes.valueOf(value)
}