package com.selkicx.manualbooth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.selkicx.manualbooth.data.local.entity.FinalOutputEntity
import com.selkicx.manualbooth.data.local.entity.SessionEntity
import com.selkicx.manualbooth.data.local.entity.SessionPhotoEntity
import com.selkicx.manualbooth.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeById(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    /** Retained template/photo mode source for Next Customer (spec rule #23 / section 22). */
    @Query("SELECT * FROM sessions WHERE state != :completed ORDER BY startTime DESC LIMIT 1")
    suspend fun getActive(completed: SessionState = SessionState.COMPLETED): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun observeHistory(): Flow<List<SessionEntity>>

    /** Used to generate the next display number (e.g. "#0049"). */
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity): Long

    @Query("UPDATE sessions SET state = :state WHERE id = :sessionId")
    suspend fun updateState(sessionId: Long, state: SessionState)
}

@Dao
interface SessionPhotoDao {
    /** Session gallery updates live as camera photos arrive (spec section 14). */
    @Query("SELECT * FROM session_photos WHERE sessionId = :sessionId ORDER BY receiveTimestamp ASC")
    fun observeBySession(sessionId: Long): Flow<List<SessionPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: SessionPhotoEntity): Long

    @Query("UPDATE session_photos SET fullResPath = :path, isFullResReady = 1 WHERE id = :id")
    suspend fun markFullResReady(id: Long, path: String)
}

@Dao
interface FinalOutputDao {
    @Query("SELECT * FROM final_outputs WHERE sessionId = :sessionId ORDER BY renderedAt DESC LIMIT 1")
    suspend fun getLatestForSession(sessionId: Long): FinalOutputEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(finalOutput: FinalOutputEntity): Long
}
