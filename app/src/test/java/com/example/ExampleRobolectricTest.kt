package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.ExcelCsvParser
import com.example.util.GeoUtils
import com.example.util.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("QR Friend", appName)
    }

    @Test
    fun `test haversine distance calculation and threshold`() {
        // Merchant at Mall Road, Lahore: 31.5580, 74.3290
        val merchantLat = 31.5580
        val merchantLon = 74.3290

        // Agent at same spot (within 10 meters)
        val closeLat = 31.55805
        val closeLon = 74.32905
        val closeDist = GeoUtils.calculateHaversineDistance(merchantLat, merchantLon, closeLat, closeLon)
        assertTrue(closeDist < 50.0)
        assertEquals("VALID", GeoUtils.getGpsStatus(closeDist, 100.0))

        // Agent 500 meters away
        val farLat = 31.5625
        val farLon = 74.3290
        val farDist = GeoUtils.calculateHaversineDistance(merchantLat, merchantLon, farLat, farLon)
        assertTrue(farDist > 100.0)
        assertEquals("GPS MISMATCH", GeoUtils.getGpsStatus(farDist, 100.0))
    }

    @Test
    fun `test password hashing with secure salt`() {
        val hash = SecurityUtils.hashPassword("admin123")
        assertNotNull(hash)
        assertTrue(SecurityUtils.verifyPassword("admin123", hash))
        assertFalse(SecurityUtils.verifyPassword("wrongpass", hash))
    }

    @Test
    fun `test csv merchant parser`() {
        val sampleCsv = """
            Merchant ID,Merchant Name,Shop Name,Mobile,Address,City,Region,ASM,TL,BDO,Latitude,Longitude,QR ID,Category,Merchant Status,QR Status
            MERCH-TEST-001,Muhammad Ali,Test Mart,03001234567,Shop 4 Hall Road Lahore,Lahore,North Region,ASM North,Ahmed Khan,ali,31.559,74.331,QR-01,General,New,Not Deployed
        """.trimIndent()

        val bdoMap = mapOf("ali" to 2L)
        val summary = ExcelCsvParser.parseAndValidate(
            rawContent = sampleCsv,
            existingMerchantIds = emptySet(),
            validBdosMap = bdoMap,
            defaultRegionId = 1L,
            defaultTlId = 1L
        )
        assertEquals(1, summary.totalRows)
        assertEquals(1, summary.validCount)
        assertEquals("MERCH-TEST-001", summary.rows[0].merchantId)
        assertEquals("Test Mart", summary.rows[0].shopName)
    }

    @Test
    fun `test excel csv with User ID column and direct agent assignment`() {
        val csvWithUserId = """
            User ID,Merchant ID,Merchant Name,Shop Name,Mobile,Address,City,Region,ASM,TL,Latitude,Longitude,QR ID,Category,Merchant Status,QR Status
            bdo_bilal,M1001,Muhammad Bilal,Bilal General Store,03001234567,Shop 12 Main Bazar,Lahore,North Region,ASM North,Ahmed Khan,31.5245,74.3590,QR-LHR-001,Grocery,Active,Deployed
        """.trimIndent()

        val dummyUser = com.example.data.local.entity.UserEntity(
            id = 5L,
            username = "bdo_bilal",
            passwordHash = "hash",
            role = "BDO",
            name = "Muhammad Bilal",
            mobile = "03001234567",
            regionId = 1L,
            tlId = 2L
        )

        val summary = ExcelCsvParser.parseAndValidate(
            rawContent = csvWithUserId,
            existingMerchantIds = emptySet(),
            usersList = listOf(dummyUser)
        )

        assertEquals(1, summary.totalRows)
        assertEquals(1, summary.validCount)
        val row = summary.rows[0]
        assertTrue(row.isValid)
        assertEquals("bdo_bilal", row.assignedUserId)
        assertEquals("M1001", row.merchantId)
        assertEquals("Bilal General Store", row.shopName)

        val entities = ExcelCsvParser.toEntities(
            validRows = summary.rows,
            usersList = listOf(dummyUser)
        )
        assertEquals(1, entities.size)
        assertEquals(5L, entities[0].bdoId)
        assertEquals("Muhammad Bilal", entities[0].bdoName)
    }

    @Test
    fun `test excel csv with unknown User ID rejects row`() {
        val csvWithBadUser = """
            User ID,Merchant ID,Merchant Name,Shop Name,Mobile,Address,City
            non_existent_agent,M1002,Test Store,Test Mart,03001112233,Bazar,Lahore
        """.trimIndent()

        val summary = ExcelCsvParser.parseAndValidate(
            rawContent = csvWithBadUser,
            existingMerchantIds = emptySet(),
            usersList = emptyList()
        )

        assertEquals(1, summary.totalRows)
        assertEquals(0, summary.validCount)
        assertFalse(summary.rows[0].isValid)
        assertTrue(summary.rows[0].validationErrors.any { it.contains("User ID") })
    }
}

