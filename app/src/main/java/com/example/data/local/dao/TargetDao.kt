package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDao {
    @Query("SELECT * FROM targets WHERE bdoId = :bdoId ORDER BY dateString DESC")
    fun getTargetsByBdo(bdoId: Long): Flow<List<TargetEntity>>

    @Query("SELECT * FROM targets WHERE bdoId = :bdoId AND dateString = :dateString LIMIT 1")
    fun getTargetByBdoAndDate(bdoId: Long, dateString: String): Flow<TargetEntity?>

    @Query("SELECT * FROM targets WHERE bdoId = :bdoId AND dateString = :dateString LIMIT 1")
    suspend fun findTargetByBdoAndDate(bdoId: Long, dateString: String): TargetEntity?

    @Query("SELECT * FROM targets ORDER BY dateString DESC")
    fun getAllTargets(): Flow<List<TargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: TargetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<TargetEntity>)

    @Update
    suspend fun updateTarget(target: TargetEntity)
}
