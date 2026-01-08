package com.example.myapp.data.DAO

import androidx.room.*
import com.example.myapp.entity.ClassWithUsers
import com.example.myapp.entity.GymClass
import com.example.myapp.entity.UserClassCrossRef
import com.example.myapp.entity.UserWithClasses
import kotlinx.coroutines.flow.Flow

@Dao
interface GymClassDao {

    // La sintaxis moderna para operaciones de modificación suspendidas
    // es especificar el tipo de retorno, que puede ser Unit.

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGymClass(gymClass: GymClass): Unit

    @Update
    suspend fun updateGymClass(gymClass: GymClass): Unit

    @Delete
    suspend fun deleteGymClass(gymClass: GymClass): Unit

    @Query("SELECT * FROM gym_classes")
    suspend fun getAllGymClasses(): List<GymClass>

    @Query("SELECT * FROM gym_classes")
    fun getAllGymClassesFlow(): Flow<List<GymClass>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enrollUserInClass(crossRef: UserClassCrossRef): Unit

    @Transaction
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserWithClasses(userId: Int): UserWithClasses?

    @Transaction
    @Query("SELECT * FROM gym_classes WHERE classId = :classId")
    suspend fun getClassWithUsers(classId: Int): ClassWithUsers?
}