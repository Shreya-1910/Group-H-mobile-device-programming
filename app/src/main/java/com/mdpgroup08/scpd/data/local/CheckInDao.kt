package com.mdpgroup08.scpd.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {

    // Prevent duplicate check-ins for the same account and event.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: CheckInRecord)

    @Query("SELECT * FROM check_in_records WHERE id = :id")
    suspend fun getById(id: String): CheckInRecord?

    // Observe records belonging only to the current account.
    @Query("""
        SELECT * FROM check_in_records
        WHERE accountId = :accountId
        ORDER BY createdAt DESC
    """)
    fun observeAll(accountId: String): Flow<List<CheckInRecord>>

    // Retrieve records awaiting upload.
    @Query("""
        SELECT * FROM check_in_records
        WHERE accountId = :accountId
        AND status = 'PENDING'
    """)
    suspend fun getPending(accountId: String): List<CheckInRecord>

    // Update the status after server verification or sync failure.
    @Query("""
        UPDATE check_in_records
        SET status = :status,
            reason = :reason
        WHERE id = :id
    """)
    suspend fun updateStatus(
        id: String,
        status: CheckInStatus,
        reason: String?
    )

    // Number of check-ins waiting to sync.
    @Query("""
        SELECT COUNT(*) FROM check_in_records
        WHERE accountId = :accountId
        AND status IN ('PENDING', 'SYNCING')
    """)
    fun observeWaitingCount(accountId: String): Flow<Int>
}