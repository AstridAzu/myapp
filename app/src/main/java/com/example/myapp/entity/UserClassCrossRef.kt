package com.example.myapp.entity

import androidx.room.Entity

@Entity(primaryKeys = ["userId", "classId"])
data class UserClassCrossRef(
    val userId: Int,
    val classId: Int
)