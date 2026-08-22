package com.aistudio.pingring.pgrng.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.aistudio.pingring.pgrng.data.model.AlertEntity
import com.aistudio.pingring.pgrng.data.model.AlertStatus
import com.aistudio.pingring.pgrng.data.model.PairedContactEntity
import com.aistudio.pingring.pgrng.data.model.UserEntity

class Converters {
    @TypeConverter
    fun fromAlertStatus(status: AlertStatus): String = status.name

    @TypeConverter
    fun toAlertStatus(value: String): AlertStatus = try {
        AlertStatus.valueOf(value)
    } catch (e: Exception) {
        AlertStatus.PENDING
    }
}

@Database(
    entities = [
        UserEntity::class,
        PairedContactEntity::class,
        AlertEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pingring_database.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
