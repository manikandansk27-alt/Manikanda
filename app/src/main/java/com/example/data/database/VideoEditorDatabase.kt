package com.example.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val projectDataJson: String // holds Moshi-serialized ProjectData JSON
)

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY updatedAt DESC")
    fun getAllProjectsFlow(): Flow<List<VideoProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProjectEntity): Long

    @Query("DELETE FROM video_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Int): VideoProjectEntity?
}

@Database(entities = [VideoProjectEntity::class], version = 1, exportSchema = false)
abstract class VideoEditorDatabase : RoomDatabase() {
    abstract val videoProjectDao: VideoProjectDao

    companion object {
        @Volatile
        private var INSTANCE: VideoEditorDatabase? = null

        fun getDatabase(context: Context): VideoEditorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoEditorDatabase::class.java,
                    "video_editor_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
