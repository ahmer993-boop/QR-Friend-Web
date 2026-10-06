package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY visitTime DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE bdoId = :bdoId ORDER BY visitTime DESC")
    fun getVisitsByBdo(bdoId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE tlId = :tlId OR bdoId IN (SELECT id FROM users WHERE tlId = :tlId) ORDER BY visitTime DESC")
    fun getVisitsByTl(tlId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE asmId = :asmId OR bdoId IN (SELECT id FROM users WHERE asmId = :asmId OR tlId IN (SELECT id FROM users WHERE asmId = :asmId)) ORDER BY visitTime DESC")
    fun getVisitsByAsm(asmId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE merchantId = :merchantId ORDER BY visitTime DESC")
    fun getVisitsByMerchant(merchantId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE visitDateString = :dateString ORDER BY visitTime DESC")
    fun getVisitsByDate(dateString: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE gpsStatus = 'GPS MISMATCH' OR verificationStatus = 'Needs Review' OR verificationStatus = 'Pending' ORDER BY visitTime DESC")
    fun getVerificationQueue(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    suspend fun getVisitById(id: Long): VisitEntity?

    @Query("SELECT * FROM visits WHERE isSynced = 0")
    fun getUnsyncedVisits(): Flow<List<VisitEntity>>

    @Query("SELECT COUNT(*) FROM visits WHERE bdoId = :bdoId AND visitDateString = :dateString")
    suspend fun countVisitsForBdoToday(bdoId: Long, dateString: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Query("UPDATE visits SET verificationStatus = :status, verificationNotes = :notes WHERE id = :id")
    suspend fun updateVerificationStatus(id: Long, status: String, notes: String)

    @Query("UPDATE visits SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markVisitsSynced(ids: List<Long>)
}
