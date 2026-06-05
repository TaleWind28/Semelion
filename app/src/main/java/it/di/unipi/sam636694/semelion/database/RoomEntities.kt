package it.di.unipi.sam636694.semelion.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import it.di.unipi.sam636694.semelion.ui.states.GamePhase
import it.di.unipi.sam636694.semelion.ui.states.GameUIState
import java.lang.reflect.Type

@Entity(tableName = "Partite")
data class Matches(
    @PrimaryKey(autoGenerate = true)
    val matchId: Long= 0,
    val gameMode: GameModes,
    val gameState: GameUIState,
    val isCompleted: Boolean
)

@Entity(tableName = "Utenti")
data class User(
    @PrimaryKey
    val userId: String,
    val nickName: String,
    val avatar: Int
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
    val userId: String,
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
    val userId: String,
    val matchId: Long= 0,
    val outcome: String,
    val winner: Boolean?,
    val wasFirstPLayer: Boolean,
    val figureRevealed:Int,
    val totalActions: Int,
    val date: Long,
)

@Entity(tableName = "StatisticheGiocatori", foreignKeys = [ForeignKey(
    entity = User::class,
    parentColumns = ["userId"],
    childColumns = ["userId"],
    onDelete = ForeignKey.CASCADE
)])
data class PlayerStatistics(
    @PrimaryKey(autoGenerate = false)
    val userId: String,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val matchesLost: Int= 0,
    val matchesDrawn: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

enum class GameModes{
    ScreenSharing,
    NearBy
}


class Converters {
    private val gson = GsonBuilder()
        .registerTypeAdapter(GamePhase::class.java, GamePhaseAdapter())
        .create()

    @TypeConverter
    fun fromGameUIState(value: GameUIState): String = gson.toJson(value)

    @TypeConverter
    fun toGameUIState(value: String): GameUIState = gson.fromJson(value, GameUIState::class.java)

    @TypeConverter
    fun fromGameModes(value: GameModes): String = value.name

    @TypeConverter
    fun toGameModes(value: String): GameModes = GameModes.valueOf(value)
}

class GamePhaseAdapter : JsonSerializer<GamePhase>, JsonDeserializer<GamePhase> {
    override fun serialize(src: GamePhase, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src::class.simpleName)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GamePhase {
        // gestisce sia {"phase":"PlayerTurn"} che {"phase":{}} (vecchi record nel db)
        val name = if (json.isJsonPrimitive) json.asString else json.asJsonObject.get("type")?.asString
        return when (name) {
            "PlayerTurn" -> GamePhase.PlayerTurn
            "QueenPending" -> GamePhase.QueenPending
            "KingPending" -> GamePhase.KingPending
            "JackMadness" -> GamePhase.JackMadness
            "Validation" -> GamePhase.Validation
            "GameOver" -> GamePhase.GameOver
            "WaitingForOpponent" -> GamePhase.WaitingForOpponent
            else -> GamePhase.Loading
        }
    }
}