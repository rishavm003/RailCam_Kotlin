package com.rishavmishra010.railcam.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PhotoRecord::class], version = 7, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "railcam_database"
                )
                    // Schema changes must ship a Migration (see app/schemas for the exported
                    // history) so upgrading the app never wipes the photo records. Only a
                    // downgrade, which cannot be migrated, falls back to recreating the DB.
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
