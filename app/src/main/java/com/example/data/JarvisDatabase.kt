package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CustomCommandDao
import com.example.data.dao.FeedbackDao
import com.example.data.dao.NoteDao
import com.example.data.dao.ReminderDao
import com.example.data.model.CustomCommandEntity
import com.example.data.model.FeedbackEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [NoteEntity::class, ReminderEntity::class, CustomCommandEntity::class, FeedbackEntity::class],
    version = 2,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun customCommandDao(): CustomCommandDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default sample custom commands and notes
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.customCommandDao().insertCommand(
                                    CustomCommandEntity(
                                        triggerPhrase = "open my website",
                                        targetUrl = "https://github.com",
                                        description = "Opens GitHub portfolio"
                                    )
                                )
                                database.customCommandDao().insertCommand(
                                    CustomCommandEntity(
                                        triggerPhrase = "open ai news",
                                        targetUrl = "https://news.google.com/search?q=artificial+intelligence",
                                        description = "Latest AI News"
                                    )
                                )
                                database.noteDao().insertNote(
                                    NoteEntity(content = "JARVIS system initialized and operational.")
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
