package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM user_feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<FeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long

    @Query("DELETE FROM user_feedback WHERE id = :id")
    suspend fun deleteFeedback(id: Long)
}
