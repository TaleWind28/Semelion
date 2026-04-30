package it.di.unipi.sam636694.semelion.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Matches::class,
        User::class,
        Participations::class,
        MatchStatistics::class,
        PlayerStatistics::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SemelionDB : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun matchesDao(): MatchesDao
    abstract fun participationsDao(): ParticipationsDao
    abstract fun playerStatisticsDao(): PlayerStatisticsDao

    abstract fun matchStatisticsDao(): MatchStatisticsDao

    companion object {
        @Volatile
        private var INSTANCE: SemelionDB? = null

        fun getDatabase(context: Context): SemelionDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SemelionDB::class.java,
                    "semelion_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

}