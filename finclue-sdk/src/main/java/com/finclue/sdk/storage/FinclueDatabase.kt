package com.finclue.sdk.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FlowSessionEntity::class, FieldMetricEntity::class, PredictionHistoryEntity::class],
    version = 2,
    exportSchema = false,
)
internal abstract class FinclueDatabase : RoomDatabase() {
    abstract fun finclueDao(): FinclueDao

    companion object {
        @Volatile private var instance: FinclueDatabase? = null

        fun getInstance(context: Context): FinclueDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinclueDatabase::class.java,
                    "finclue_on_device.db",
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prediction_history` (
                        `contextKey` TEXT NOT NULL,
                        `normalizedText` TEXT NOT NULL,
                        `displayText` TEXT NOT NULL,
                        `selectionCount` INTEGER NOT NULL,
                        `lastSelectedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`contextKey`, `normalizedText`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
