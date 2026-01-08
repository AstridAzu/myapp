package com.example.myapp.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ClassWithUsers(
    @Embedded val gymClass: GymClass,
    @Relation(
        parentColumn = "classId",
        entityColumn = "userId",
        associateBy = Junction(UserClassCrossRef::class)
    )
    val users: List<User>
)

data class UserWithClasses(
    @Embedded val user: User,
    @Relation(
        parentColumn = "userId",
        entityColumn = "classId",
        associateBy = Junction(UserClassCrossRef::class)
    )
    val classes: List<GymClass>
)