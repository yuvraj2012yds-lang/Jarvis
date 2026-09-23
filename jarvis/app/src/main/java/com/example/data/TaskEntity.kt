package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val category: String = "General",
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
