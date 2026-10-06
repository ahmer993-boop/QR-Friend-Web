package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TlDao {
    @Query("SELECT * FROM tls ORDER BY name ASC")
    fun getAllTls(): Flow<List<TlEntity>>

    @Query("SELECT * FROM tls WHERE regionId = :regionId ORDER BY name ASC")
    fun getTlsByRegion(regionId: Long): Flow<List<TlEntity>>

    @Query("SELECT * FROM tls WHERE id = :id LIMIT 1")
    suspend fun getTlById(id: Long): TlEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTl(tl: TlEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTls(tls: List<TlEntity>)

    @Update
    suspend fun updateTl(tl: TlEntity)

    @Query("DELETE FROM tls WHERE id = :id")
    suspend fun deleteTl(id: Long)
}
