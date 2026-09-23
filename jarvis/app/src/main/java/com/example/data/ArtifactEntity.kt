package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artifacts")
data class ArtifactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: String, // "CODE", "DOCUMENT", "EMAIL", "TABLE"
    val language: String = "", // e.g. "python", "kotlin", "typescript", "markdown"
    val content: String,
    val previewDetails: String = "", // summary or extra metadata (e.g., recipients for email)
    val timestamp: Long = System.currentTimeMillis()
)
