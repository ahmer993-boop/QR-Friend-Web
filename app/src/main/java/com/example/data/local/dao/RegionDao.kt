package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegionDao {
    @Query("SELECT * FROM regions ORDER BY name ASC")
    fun getAllRegions(): Flow<List<RegionEntity>>

    @Query("SELECT * FROM regions WHERE id = :id LIMIT 1")
    suspend fun getRegionById(id: Long): RegionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegion(region: RegionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(regions: List<RegionEntity>)

    @Update
    suspend fun updateRegion(region: RegionEntity)

    @Query("DELETE FROM regions WHERE id = :id")
    suspend fun deleteRegion(id: Long)
}
