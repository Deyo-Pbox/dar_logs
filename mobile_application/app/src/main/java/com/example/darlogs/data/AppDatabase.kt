package com.example.darlogs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(
    entities = [
        RecordEntity::class, 
        StatsEntity::class, 
        RouteUserEntity::class, 
        NotificationEntity::class, 
        UserEntity::class, 
        AuditLogEntity::class
    ], 
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dar_logs_database"
                )
                .addMigrations(MIGRATION_8_9)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("PRAGMA journal_mode=WAL")
                        db.execSQL("PRAGMA synchronous=NORMAL")
                    }
                })
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_logs_syncStatus ON activity_logs(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_logs_isArchived ON activity_logs(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_logs_municipality ON activity_logs(municipality)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_logs_id ON activity_logs(id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_is_read ON notifications(is_read)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_users_username ON users(username)")
            }
        }
    }
}
