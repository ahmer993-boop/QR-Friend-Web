package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AppSettingDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.MerchantDao
import com.example.data.local.dao.QrRequestDao
import com.example.data.local.dao.RegionDao
import com.example.data.local.dao.TargetDao
import com.example.data.local.dao.TlDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.VisitDao
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.QrRequestEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.TargetEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity

@Database(
    entities = [
        UserEntity::class,
        RegionEntity::class,
        TlEntity::class,
        MerchantEntity::class,
        VisitEntity::class,
        TargetEntity::class,
        TransactionEntity::class,
        AuditLogEntity::class,
        AppSettingEntity::class,
        QrRequestEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun regionDao(): RegionDao
    abstract fun tlDao(): TlDao
    abstract fun merchantDao(): MerchantDao
    abstract fun visitDao(): VisitDao
    abstract fun targetDao(): TargetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun qrRequestDao(): QrRequestDao

    companion object {
        const val DATABASE_NAME = "qr_merchant_field_force.db"

        /**
         * Migration from Schema 1 to 2:
         * Adds audit_logs, app_settings, and targets tables with indices.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `audit_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `userName` TEXT NOT NULL,
                        `action` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `dateStr` TEXT NOT NULL,
                        `timeStr` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `recordAffected` TEXT NOT NULL,
                        `previousValue` TEXT NOT NULL,
                        `newValue` TEXT NOT NULL,
                        `metadata` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `app_settings` (
                        `key` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `targets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bdoId` INTEGER NOT NULL,
                        `bdoName` TEXT NOT NULL,
                        `targetVisits` INTEGER NOT NULL,
                        `targetQrs` INTEGER NOT NULL,
                        `dateString` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_targets_bdoId_dateString` ON `targets` (`bdoId`, `dateString`)")
            }
        }

        /**
         * Migration from Schema 2 to 3:
         * Adds qr_requests table with indexes to support replacement/reissue workflows.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `qr_requests` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `merchantId` TEXT NOT NULL,
                        `merchantName` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL,
                        `bdoId` INTEGER NOT NULL,
                        `bdoName` TEXT NOT NULL,
                        `requestType` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `oldQrId` TEXT NOT NULL,
                        `newQrId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `requestDate` TEXT NOT NULL,
                        `requestTime` TEXT NOT NULL,
                        `resolvedDate` TEXT NOT NULL,
                        `approvedBy` TEXT NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_qr_requests_merchantId` ON `qr_requests` (`merchantId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_qr_requests_bdoId` ON `qr_requests` (`bdoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_qr_requests_status` ON `qr_requests` (`status`)")
            }
        }

        /**
         * Safe forward migration hook for Version 4 (if new columns or tables are added).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Preserves all existing tables and data
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Explicit schema migrations to guarantee zero data loss during app updates
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    // STRICT REQUIREMENT: fallbackToDestructiveMigration is deliberately omitted
                    // so app updates NEVER wipe merchant records, visit logs, or user data.
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Enable Write-Ahead Logging (WAL) for safe multi-thread performance and clean checkpoints
                            db.enableWriteAheadLogging()
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
