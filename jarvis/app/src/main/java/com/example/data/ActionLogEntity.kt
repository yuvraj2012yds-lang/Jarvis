package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val actionType: String, // e.g. "CODE_GENERATION", "TASK_ADDED", "SCHEDULE_UPDATED", "DOCUMENT_DRAFTED"
    val summary: String,
    val status: String = "SUCCESS", // "SUCCESS", "EXECUTING", "FAILED"
    val timestamp: Long = System.currentTimeMillis()
)
