package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerTime: Long,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val triggerPhrase: String,
    val targetUrl: String,
    val description: String = ""
)

@Entity(tableName = "user_feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rating: Int = 5,
    val category: String = "General",
    val comment: String = "",
    val language: String = "en",
    val voiceGender: String = "MALE",
    val timestamp: Long = System.currentTimeMillis()
)

