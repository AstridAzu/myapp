package com.example.myapp.data.database

import android.content.Context
import androidx.room.Room
import com.example.myapp.data.AppDatabase

object DatabaseBuilder {
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                // Añadido para prevenir crashes por cambios en la base de datos
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}