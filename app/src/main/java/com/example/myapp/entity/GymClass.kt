package com.example.myapp.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gym_classes")
data class GymClass(
    @PrimaryKey(autoGenerate = true)
    val classId: Int = 0,
    val name: String,
    val description: String,
    val schedule: String,
    val creatorId: Int = 0 // ID del usuario que creó la clase
)