package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val time: String, // e.g. "09:00 AM"
    val title: String,
    val description: String = "",
    val location: String = "Main Workspace",
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
