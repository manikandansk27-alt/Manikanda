package com.example.data.repository

import com.example.data.database.VideoProjectDao
import com.example.data.database.VideoProjectEntity
import com.example.data.model.ProjectData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VideoProjectRepository(private val videoProjectDao: VideoProjectDao) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(ProjectData::class.java)

    val allProjects: Flow<List<Pair<Int, Pair<String, ProjectData>>>> = videoProjectDao.getAllProjectsFlow().map { entities ->
        entities.mapNotNull { entity ->
            try {
                val data = adapter.fromJson(entity.projectDataJson)
                if (data != null) {
                    entity.id to (entity.name to data)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveProject(id: Int, name: String, data: ProjectData): Int = withContext(Dispatchers.IO) {
        val json = adapter.toJson(data)
        val entity = VideoProjectEntity(
            id = if (id == 0) 0 else id,
            name = name,
            updatedAt = System.currentTimeMillis(),
            projectDataJson = json
        )
        videoProjectDao.insertProject(entity).toInt()
    }

    suspend fun getProjectById(id: Int): ProjectData? = withContext(Dispatchers.IO) {
        val entity = videoProjectDao.getProjectById(id) ?: return@withContext null
        try {
            adapter.fromJson(entity.projectDataJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteProject(id: Int) = withContext(Dispatchers.IO) {
        videoProjectDao.deleteProjectById(id)
    }
}
