package com.dutchman.resumeiq.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dutchman.resumeiq.data.local.dao.QuestionDao
import com.dutchman.resumeiq.data.local.entity.QuestionEntity

@Database(
    entities = [QuestionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract val questionDao: QuestionDao
    
    companion object {
        const val DATABASE_NAME = "resumeiq_db"
    }
}
