package com.example.myapp.data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myapp.entity.Class
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {

    @Insert
    fun insertClass(classEntity: Class): Long

    @Query("SELECT * FROM classes WHERE userId = :userId")
    fun getClassesForUser(userId: Int): List<Class>
}