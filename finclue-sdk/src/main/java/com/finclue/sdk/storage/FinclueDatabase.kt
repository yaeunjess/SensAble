package com.finclue.sdk.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FlowSessionEntity::class, FieldMetricEntity::class],
    version = 1,
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
                ).build().also { instance = it }
            }
    }
}
