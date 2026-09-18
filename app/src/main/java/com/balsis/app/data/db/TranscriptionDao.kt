package com.balsis.app.data.db

import androidx.room.*
import com.balsis.app.data.model.TranscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    suspend fun getById(id: Long): TranscriptionEntity?

    @Query("""
        SELECT * FROM transcriptions 
        WHERE summary LIKE '%' || :query || '%' 
           OR fullText LIKE '%' || :query || '%' 
           OR senderName LIKE '%' || :query || '%' 
           OR chatName LIKE '%' || :query || '%' 
        ORDER BY createdAt DESC
    """)
    fun searchFlow(query: String): Flow<List<TranscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranscriptionEntity): Long

    @Update
    suspend fun update(entity: TranscriptionEntity)

    @Delete
    suspend fun delete(entity: TranscriptionEntity)

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
