package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.QrRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrRequestDao {
    @Query("SELECT * FROM qr_requests ORDER BY createdAt DESC")
    fun getAllQrRequests(): Flow<List<QrRequestEntity>>

    @Query("SELECT * FROM qr_requests WHERE asmId = :asmId ORDER BY createdAt DESC")
    fun getQrRequestsByAsm(asmId: Long): Flow<List<QrRequestEntity>>

    @Query("SELECT * FROM qr_requests WHERE tlId = :tlId ORDER BY createdAt DESC")
    fun getQrRequestsByTl(tlId: Long): Flow<List<QrRequestEntity>>

    @Query("SELECT * FROM qr_requests WHERE bdoId = :bdoId ORDER BY createdAt DESC")
    fun getQrRequestsByBdo(bdoId: Long): Flow<List<QrRequestEntity>>

    @Query("SELECT * FROM qr_requests WHERE requestId = :requestId LIMIT 1")
    suspend fun findByRequestId(requestId: String): QrRequestEntity?

    @Query("SELECT * FROM qr_requests WHERE merchantId = :merchantId ORDER BY createdAt DESC")
    fun getQrRequestsByMerchant(merchantId: String): Flow<List<QrRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQrRequest(request: QrRequestEntity): Long

    @Update
    suspend fun updateQrRequest(request: QrRequestEntity)

    @Query("UPDATE qr_requests SET status = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM qr_requests")
    suspend fun countRequests(): Int
}
