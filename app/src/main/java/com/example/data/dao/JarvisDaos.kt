package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CustomCommandEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY triggerTime ASC")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)
}

@Dao
interface CustomCommandDao {
    @Query("SELECT * FROM custom_commands ORDER BY triggerPhrase ASC")
    fun getAllCommands(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands")
    suspend fun getCommandList(): List<CustomCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CustomCommandEntity): Long

    @Delete
    suspend fun deleteCommand(command: CustomCommandEntity)
}
