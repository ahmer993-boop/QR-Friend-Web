package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY name ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE tlId = :tlId AND role = 'BDO' ORDER BY name ASC")
    fun getBdosByTl(tlId: Long): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    fun getUserByUsername(username: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'TL' AND asmId = :asmId ORDER BY name ASC")
    fun getTlsByAsm(asmId: Long): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'BDO' AND (tlId IN (SELECT id FROM users WHERE asmId = :asmId AND role = 'TL') OR asmId = :asmId) ORDER BY name ASC")
    fun getBdosByAsm(asmId: Long): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE regionId = :regionId ORDER BY name ASC")
    fun getUsersByRegion(regionId: Long): Flow<List<UserEntity>>

    @Query("UPDATE users SET status = :status, updatedAt = :timestamp WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET tlId = :tlId, asmId = :asmId, regionId = :regionId, updatedAt = :timestamp WHERE id = :userId")
    suspend fun updateReporting(userId: Long, tlId: Long?, asmId: Long?, regionId: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET passwordHash = :passwordHash, updatedAt = :timestamp WHERE id = :userId")
    suspend fun updatePassword(userId: Long, passwordHash: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)
}
