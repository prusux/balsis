package com.balsis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a transcribed voice note stored locally.
 */
@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val senderName: String,
    val chatName: String,
    val summary: String,
    val fullText: String,
    val durationSec: Int = 0,
    val audioUri: String? = null,
    val isFavorite: Boolean = false
)
