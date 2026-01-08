package com.example.myapp.data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myapp.entity.User

@Dao
interface UserDao {

    @Insert
    fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    fun login(username: String, password: String): User?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUserById(userId: Int): User?

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    fun usernameExists(username: String): Boolean
}