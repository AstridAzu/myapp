package com.example.myapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapp.data.DAO.GymClassDao
import com.example.myapp.data.DAO.UserDao
import com.example.myapp.entity.GymClass
import com.example.myapp.entity.User
import com.example.myapp.entity.UserClassCrossRef

@Database(
    entities = [User::class, GymClass::class, UserClassCrossRef::class],
    version = 4, // Incremented version for the new schedule field
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gymClassDao(): GymClassDao
}