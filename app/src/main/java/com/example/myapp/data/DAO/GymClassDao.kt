package com.example.myapp.data.DAO

import androidx.room.*
import com.example.myapp.entity.ClassWithUsers
import com.example.myapp.entity.GymClass
import com.example.myapp.entity.UserClassCrossRef
import com.example.myapp.entity.UserWithClasses
import kotlinx.coroutines.flow.Flow

@Dao
interface GymClassDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGymClass(gymClass: GymClass)

    @Update
    fun updateGymClass(gymClass: GymClass)

    @Query("SELECT * FROM gym_classes")
    fun getAllGymClasses(): List<GymClass>

    @Query("SELECT * FROM gym_classes")
    fun getAllGymClassesFlow(): Flow<List<GymClass>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun enrollUserInClass(crossRef: UserClassCrossRef)

    @Transaction
    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserWithClasses(userId: Int): UserWithClasses?

    @Transaction
    @Query("SELECT * FROM gym_classes WHERE classId = :classId")
    fun getClassWithUsers(classId: Int): ClassWithUsers?
}