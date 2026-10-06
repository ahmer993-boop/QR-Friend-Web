package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants ORDER BY id DESC")
    fun getAllMerchants(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants ORDER BY id DESC")
    suspend fun getAllMerchantsSync(): List<MerchantEntity>

    @Query("SELECT * FROM merchants WHERE bdoId = :bdoId ORDER BY id DESC")
    fun getMerchantsByBdo(bdoId: Long): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE tlId = :tlId ORDER BY id DESC")
    fun getMerchantsByTl(tlId: Long): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE asmId = :asmId ORDER BY id DESC")
    fun getMerchantsByAsm(asmId: Long): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE regionId = :regionId ORDER BY id DESC")
    fun getMerchantsByRegion(regionId: Long): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE bdoId = :bdoId ORDER BY id DESC")
    suspend fun getMerchantsByBdoSync(bdoId: Long): List<MerchantEntity>

    @Query("SELECT * FROM merchants WHERE merchantId = :merchantId LIMIT 1")
    fun getMerchantByMerchantId(merchantId: String): Flow<MerchantEntity?>

    @Query("SELECT * FROM merchants WHERE merchantId = :merchantId LIMIT 1")
    suspend fun findMerchantByMerchantId(merchantId: String): MerchantEntity?

    @Query("SELECT * FROM merchants WHERE id = :id LIMIT 1")
    suspend fun getMerchantById(id: Long): MerchantEntity?

    @Query("SELECT COUNT(*) FROM merchants")
    suspend fun countMerchants(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: MerchantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchants(merchants: List<MerchantEntity>)

    @Update
    suspend fun updateMerchant(merchant: MerchantEntity)

    @Query("UPDATE merchants SET merchantStatus = :status, qrStatus = :qrStatus, qrId = CASE WHEN :qrId != '' THEN :qrId ELSE qrId END, updatedAt = :timestamp WHERE merchantId = :merchantId")
    suspend fun updateStatusAndQr(merchantId: String, status: String, qrStatus: String, qrId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE merchants SET bdoId = :bdoId, tlId = :tlId, regionId = :regionId, updatedAt = :timestamp WHERE id = :id")
    suspend fun reassignMerchant(id: Long, bdoId: Long, tlId: Long, regionId: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteMerchant(merchant: MerchantEntity)

    @Query("DELETE FROM merchants WHERE id = :id")
    suspend fun deleteMerchant(id: Long)
}
