package com.mdpgroup08.scpd.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CheckInRecord::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CheckInConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun checkInDao(): CheckInDao

    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {

            return instance ?: synchronized(this) {

                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scpd.db"
                )
                    .build()
                    .also { instance = it }
            }
        }
    }
}