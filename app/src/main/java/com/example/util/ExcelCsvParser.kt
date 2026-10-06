package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.UserEntity
import java.io.File
import java.io.FileOutputStream

enum class RowActionType(val label: String) {
    NEW("New Merchant"),
    UPDATE("Update Existing"),
    DUPLICATE("Duplicate in File"),
    INVALID("Invalid Record")
}

data class ParsedMerchantRow(
    val rowNumber: Int,
    val merchantId: String,
    val merchantName: String,
    val businessName: String,
    val shopName: String,
    val cnic: String = "",
    val mobile: String,
    val alternateMobile: String = "",
    val address: String,
    val city: String,
    val regionName: String,
    val asmName: String = "",
    val tlName: String,
    val bdoUsernameOrName: String, // Kept for backwards compatibility
    val assignedUserId: String = bdoUsernameOrName,
    val assignedUser: UserEntity? = null,
    val assignedUserDisplay: String = "",
    val latitude: Double?,
    val longitude: Double?,
    val qrId: String,
    val merchantCategory: String,
    val merchantStatus: String,
    val qrStatus: String,
    val actionType: RowActionType,
    val isValid: Boolean,
    val validationErrors: List<String>
)

data class ImportValidationSummary(
    val totalRows: Int,
    val newCount: Int,
    val updateCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val rows: List<ParsedMerchantRow>
) {
    val validCount: Int get() = newCount + updateCount
}

object ExcelCsvParser {

    /**
     * Parses raw CSV, TSV (tab-separated from Excel copy/paste), or semicolon-separated text.
     * Flexibly detects column headers in ANY order, with full support for the "User ID" column
     * so merchants are directly assigned to that agent upon import.
     */
    fun parseAndValidate(
        rawContent: String,
        existingMerchantIds: Set<String>,
        validBdosMap: Map<String, Long> = emptyMap(),
        defaultRegionId: Long = 1L,
        defaultTlId: Long = 1L,
        usersList: List<UserEntity> = emptyList()
    ): ImportValidationSummary {
        val lines = rawContent.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ImportValidationSummary(0, 0, 0, 0, 0, emptyList())
        }

        val rows = mutableListOf<ParsedMerchantRow>()
        val seenInFile = mutableSetOf<String>()

        var newCount = 0
        var updateCount = 0
        var duplicateCount = 0
        var invalidCount = 0

        // User lookup maps for fast, lenient resolution
        val usersByUsername = usersList.associateBy { it.username.trim().lowercase() }
        val usersById = usersList.associateBy { it.id.toString() }
        val usersByEmployeeId = usersList.filter { it.employeeId.isNotBlank() }.associateBy { it.employeeId.trim().lowercase() }
        val usersByName = usersList.associateBy { it.name.trim().lowercase() }

        // Determine delimiter of the first line
        val firstLine = lines.first()
        val delimiter = detectDelimiter(firstLine)

        // Check if first line is a header
        val headerTokens = splitRow(firstLine, delimiter).map { it.trim().lowercase() }
        val isHeader = isHeaderLine(headerTokens)

        val colMap: Map<String, Int>
        val dataLines: List<String>

        if (isHeader) {
            colMap = buildHeaderColumnMap(headerTokens)
            dataLines = lines.drop(1)
        } else {
            // Default index-based mapping
            colMap = buildDefaultColumnMap(splitRow(firstLine, delimiter))
            dataLines = lines
        }

        dataLines.forEachIndexed { index, line ->
            val rowNumber = if (isHeader) index + 2 else index + 1
            val rowDelimiter = detectDelimiter(line)
            val parts = splitRow(line, rowDelimiter)
            if (parts.isEmpty() || parts.all { it.isBlank() }) return@forEachIndexed

            fun getVal(key: String, fallbackIndex: Int = -1, default: String = ""): String {
                val idx = colMap[key] ?: fallbackIndex
                return if (idx in parts.indices) parts[idx].trim() else default
            }

            val mId = getVal("MERCHANT_ID", 0)
            val userIdRaw = getVal("USER_ID", 9)
            val mName = getVal("MERCHANT_NAME", 1)
            val bName = getVal("SHOP_NAME", 2)
            val mobile = getVal("MOBILE", 3)
            val address = getVal("ADDRESS", 4)
            val city = getVal("CITY", 5, "Lahore")
            val regionName = getVal("REGION", 6, "North Region")
            val asmName = getVal("ASM", 7, "ASM North")
            val tlName = getVal("TL", 8, "Ahmed Khan")
            val latStr = getVal("LATITUDE", 10)
            val lonStr = getVal("LONGITUDE", 11)
            val qrId = getVal("QR_ID", 12)
            val category = getVal("CATEGORY", 13, "General Store")
            val status = getVal("STATUS", 14, "New")
            val qrStatus = getVal("QR_STATUS", 15, "Not Deployed")

            val errors = mutableListOf<String>()

            // 1. Merchant ID validation
            if (mId.isBlank()) {
                errors.add("Missing Merchant ID")
            } else if (seenInFile.contains(mId.lowercase())) {
                errors.add("Duplicate Merchant ID in file: '$mId'")
            }

            // 2. User ID (Agent Assignment) validation
            val cleanUserId = userIdRaw.lowercase()
            var matchedUser: UserEntity? = null

            if (cleanUserId.isBlank()) {
                errors.add("Missing User ID: Please specify the agent's User ID to directly assign the merchant")
            } else {
                // Try resolving user from usersList
                matchedUser = usersByUsername[cleanUserId]
                    ?: usersById[cleanUserId]
                    ?: usersByEmployeeId[cleanUserId]
                    ?: usersByName[cleanUserId]
                    ?: usersList.firstOrNull { it.username.contains(cleanUserId, ignoreCase = true) || it.name.contains(cleanUserId, ignoreCase = true) }

                // Fallback check in validBdosMap if usersList is empty or not passed
                if (matchedUser == null && !validBdosMap.containsKey(cleanUserId) && !validBdosMap.containsKey(userIdRaw)) {
                    errors.add("User ID '$userIdRaw' not found in system. Please verify the agent's User ID")
                }
            }

            // 3. Name validation
            if (mName.isBlank() && bName.isBlank()) {
                errors.add("Missing Merchant Name / Shop Name")
            }

            // 4. Mobile validation
            if (mobile.isBlank()) {
                errors.add("Missing Mobile Number")
            }

            // 5. GPS check
            val lat = latStr.toDoubleOrNull()
            val lon = lonStr.toDoubleOrNull()
            if (latStr.isNotBlank() && (lat == null || lat < -90.0 || lat > 90.0)) {
                errors.add("Invalid Latitude: '$latStr'")
            }
            if (lonStr.isNotBlank() && (lon == null || lon < -180.0 || lon > 180.0)) {
                errors.add("Invalid Longitude: '$lonStr'")
            }

            val isDuplicateInFile = mId.isNotBlank() && seenInFile.contains(mId.lowercase())
            val hasErrors = errors.isNotEmpty()

            val actionType: RowActionType
            if (isDuplicateInFile) {
                actionType = RowActionType.DUPLICATE
                duplicateCount++
            } else if (hasErrors) {
                actionType = RowActionType.INVALID
                invalidCount++
            } else if (existingMerchantIds.contains(mId.lowercase()) || existingMerchantIds.contains(mId)) {
                actionType = RowActionType.UPDATE
                updateCount++
            } else {
                actionType = RowActionType.NEW
                newCount++
            }

            if (mId.isNotBlank()) seenInFile.add(mId.lowercase())

            val finalShopName = if (bName.isNotBlank()) bName else mName
            val finalMerchantName = if (mName.isNotBlank()) mName else bName

            val assignedDisplay = when {
                matchedUser != null -> "${matchedUser.name} (${matchedUser.username}, ${matchedUser.role})"
                userIdRaw.isNotBlank() -> userIdRaw
                else -> "Unassigned"
            }

            rows.add(
                ParsedMerchantRow(
                    rowNumber = rowNumber,
                    merchantId = mId,
                    merchantName = finalMerchantName,
                    businessName = finalShopName,
                    shopName = finalShopName,
                    mobile = mobile,
                    address = address,
                    city = city,
                    regionName = regionName,
                    asmName = asmName,
                    tlName = tlName,
                    bdoUsernameOrName = userIdRaw,
                    assignedUserId = userIdRaw,
                    assignedUser = matchedUser,
                    assignedUserDisplay = assignedDisplay,
                    latitude = lat ?: 31.5204,
                    longitude = lon ?: 74.3587,
                    qrId = qrId,
                    merchantCategory = category,
                    merchantStatus = if (status.isNotBlank()) status else "New",
                    qrStatus = if (qrStatus.isNotBlank()) qrStatus else "Not Deployed",
                    actionType = actionType,
                    isValid = actionType == RowActionType.NEW || actionType == RowActionType.UPDATE,
                    validationErrors = errors
                )
            )
        }

        return ImportValidationSummary(
            totalRows = rows.size,
            newCount = newCount,
            updateCount = updateCount,
            duplicateCount = duplicateCount,
            invalidCount = invalidCount,
            rows = rows
        )
    }

    private fun detectDelimiter(line: String): String {
        val tabs = line.count { it == '\t' }
        val commas = line.count { it == ',' }
        val semis = line.count { it == ';' }
        return when {
            tabs >= commas && tabs >= semis && tabs > 0 -> "\t"
            semis >= commas && semis > 0 -> ";"
            else -> ","
        }
    }

    private fun isHeaderLine(tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return false
        val headerKeywords = listOf("merchant", "id", "user", "agent", "shop", "name", "bdo", "mobile", "phone", "address")
        return tokens.any { token ->
            headerKeywords.any { kw -> token.contains(kw) }
        }
    }

    private fun buildHeaderColumnMap(headers: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        headers.forEachIndexed { index, rawHeader ->
            val h = rawHeader.trim().lowercase()
            when {
                h in listOf("user id", "userid", "user_id", "agent id", "agent_id", "bdo", "bdo id", "bdo_id", "bdo username", "username", "assigned user", "assigned agent", "assigned to", "agent", "bdo name", "field agent", "user", "agent_username") -> {
                    if (!map.containsKey("USER_ID")) map["USER_ID"] = index
                }
                h in listOf("merchant id", "merchant_id", "mid", "m_id", "merchant code", "merchantid", "m id", "id") && !map.containsKey("MERCHANT_ID") -> {
                    map["MERCHANT_ID"] = index
                }
                h in listOf("merchant name", "merchant_name", "owner name", "owner", "contact name", "person name", "full name") -> {
                    if (!map.containsKey("MERCHANT_NAME")) map["MERCHANT_NAME"] = index
                }
                h in listOf("shop name", "shop_name", "business name", "business_name", "store name", "store_name", "shop", "store", "outlet") -> {
                    if (!map.containsKey("SHOP_NAME")) map["SHOP_NAME"] = index
                }
                h in listOf("mobile", "mobile number", "mobile_number", "phone", "phone number", "contact", "contact number", "cell", "phone_number") -> {
                    if (!map.containsKey("MOBILE")) map["MOBILE"] = index
                }
                h in listOf("address", "shop address", "location", "street address", "address line") -> {
                    if (!map.containsKey("ADDRESS")) map["ADDRESS"] = index
                }
                h in listOf("city", "town", "district") -> {
                    if (!map.containsKey("CITY")) map["CITY"] = index
                }
                h in listOf("region", "region name", "zone") -> {
                    if (!map.containsKey("REGION")) map["REGION"] = index
                }
                h in listOf("asm", "asm name", "area manager", "area sales manager") -> {
                    if (!map.containsKey("ASM")) map["ASM"] = index
                }
                h in listOf("tl", "tl name", "team lead", "team leader", "supervisor") -> {
                    if (!map.containsKey("TL")) map["TL"] = index
                }
                h in listOf("latitude", "lat") -> {
                    if (!map.containsKey("LATITUDE")) map["LATITUDE"] = index
                }
                h in listOf("longitude", "long", "lon", "lng") -> {
                    if (!map.containsKey("LONGITUDE")) map["LONGITUDE"] = index
                }
                h in listOf("qr id", "qr_id", "qr code", "qr", "qrid") -> {
                    if (!map.containsKey("QR_ID")) map["QR_ID"] = index
                }
                h in listOf("category", "merchant category", "type", "business type") -> {
                    if (!map.containsKey("CATEGORY")) map["CATEGORY"] = index
                }
                h in listOf("merchant status", "status", "merchant_status") -> {
                    if (!map.containsKey("STATUS")) map["STATUS"] = index
                }
                h in listOf("qr status", "qr_status") -> {
                    if (!map.containsKey("QR_STATUS")) map["QR_STATUS"] = index
                }
            }
        }

        // Fallback fuzzy checks if key columns are missing
        if (!map.containsKey("USER_ID")) {
            val idx = headers.indexOfFirst { it.contains("user") || it.contains("bdo") || it.contains("agent") }
            if (idx != -1) map["USER_ID"] = idx
        }
        if (!map.containsKey("MERCHANT_ID")) {
            val idx = headers.indexOfFirst { (it.contains("merchant") && it.contains("id")) || it == "mid" || it == "id" }
            if (idx != -1) map["MERCHANT_ID"] = idx
        }
        if (!map.containsKey("SHOP_NAME")) {
            val idx = headers.indexOfFirst { it.contains("shop") || it.contains("business") || it.contains("store") }
            if (idx != -1) map["SHOP_NAME"] = idx
        }
        if (!map.containsKey("MERCHANT_NAME")) {
            val idx = headers.indexOfFirst { it.contains("name") && !it.contains("shop") && !it.contains("business") }
            if (idx != -1) map["MERCHANT_NAME"] = idx
        }
        if (!map.containsKey("MOBILE")) {
            val idx = headers.indexOfFirst { it.contains("mobile") || it.contains("phone") || it.contains("contact") }
            if (idx != -1) map["MOBILE"] = idx
        }

        return map
    }

    private fun buildDefaultColumnMap(sampleRow: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        // If row 0 starts with User ID format (e.g. bdo_ or username), map col 0 to USER_ID and 1 to MERCHANT_ID
        val firstToken = sampleRow.getOrNull(0)?.lowercase() ?: ""
        if (firstToken.startsWith("bdo_") || firstToken.startsWith("user_") || firstToken.startsWith("agent")) {
            map["USER_ID"] = 0
            map["MERCHANT_ID"] = 1
            map["MERCHANT_NAME"] = 2
            map["SHOP_NAME"] = 3
            map["MOBILE"] = 4
            map["ADDRESS"] = 5
            map["CITY"] = 6
            map["REGION"] = 7
            map["ASM"] = 8
            map["TL"] = 9
            map["LATITUDE"] = 10
            map["LONGITUDE"] = 11
            map["QR_ID"] = 12
            map["CATEGORY"] = 13
            map["STATUS"] = 14
            map["QR_STATUS"] = 15
        } else {
            // Standard format: Merchant ID at col 0, BDO/User ID at col 9
            map["MERCHANT_ID"] = 0
            map["MERCHANT_NAME"] = 1
            map["SHOP_NAME"] = 2
            map["MOBILE"] = 3
            map["ADDRESS"] = 4
            map["CITY"] = 5
            map["REGION"] = 6
            map["ASM"] = 7
            map["TL"] = 8
            map["USER_ID"] = 9
            map["LATITUDE"] = 10
            map["LONGITUDE"] = 11
            map["QR_ID"] = 12
            map["CATEGORY"] = 13
            map["STATUS"] = 14
            map["QR_STATUS"] = 15
        }
        return map
    }

    private fun splitRow(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch.toString() == delimiter && !inQuotes) {
                result.add(cleanCell(cur.toString()))
                cur = StringBuilder()
            } else {
                cur.append(ch)
            }
        }
        result.add(cleanCell(cur.toString()))
        return result
    }

    private fun cleanCell(cell: String): String {
        var str = cell.trim()
        if (str.startsWith("\"") && str.endsWith("\"") && str.length >= 2) {
            str = str.substring(1, str.length - 1).replace("\"\"", "\"")
        }
        return str.trim()
    }

    /**
     * Converts validated rows into MerchantEntity objects directly assigned to the agent.
     */
    fun toEntities(
        validRows: List<ParsedMerchantRow>,
        validBdosMap: Map<String, Long> = emptyMap(),
        defaultRegionId: Long = 1L,
        defaultTlId: Long = 1L,
        bdoToHierarchyMap: Map<Long, Triple<Long, Long, Long>> = emptyMap(),
        usersList: List<UserEntity> = emptyList(),
        regions: List<RegionEntity> = emptyList(),
        tls: List<TlEntity> = emptyList()
    ): List<MerchantEntity> {
        val now = System.currentTimeMillis()
        val usersById = usersList.associateBy { it.id }
        val regionsById = regions.associateBy { it.id }
        val tlsById = tls.associateBy { it.id }

        return validRows.map { r ->
            val user = r.assignedUser
                ?: usersById[validBdosMap[r.bdoUsernameOrName.lowercase()] ?: 0L]

            val bdoId = user?.id ?: validBdosMap[r.bdoUsernameOrName.lowercase()] ?: 0L
            val bdoName = user?.name ?: r.bdoUsernameOrName
            val bdoCode = user?.employeeId?.ifBlank { user.username } ?: r.bdoUsernameOrName

            // Automatic hierarchy assignment from the user or mapped values
            val hierarchy = bdoToHierarchyMap[bdoId]
            val regionId = user?.regionId ?: hierarchy?.first ?: defaultRegionId
            val asmId = user?.asmId ?: hierarchy?.second ?: 0L
            val tlId = user?.tlId ?: hierarchy?.third ?: defaultTlId

            val regionName = r.regionName.ifBlank { regionsById[regionId]?.name ?: "North Region" }
            val tlName = r.tlName.ifBlank { tlsById[tlId]?.name ?: "" }
            val asmName = r.asmName.ifBlank { usersById[asmId]?.name ?: "" }

            MerchantEntity(
                merchantId = r.merchantId,
                merchantName = r.merchantName,
                businessName = r.businessName,
                shopName = r.shopName,
                mobile = r.mobile,
                address = r.address,
                city = r.city,
                regionId = regionId,
                regionName = regionName,
                asmId = asmId,
                asmName = r.asmName.ifBlank { asmName },
                tlId = tlId,
                tlCode = "",
                tlName = r.tlName.ifBlank { tlName },
                bdoId = bdoId,
                bdoCode = bdoCode,
                bdoName = bdoName,
                latitude = r.latitude ?: 0.0,
                longitude = r.longitude ?: 0.0,
                qrId = r.qrId,
                qrStatus = r.qrStatus,
                merchantStatus = r.merchantStatus,
                merchantCategory = r.merchantCategory,
                registrationDate = if (r.merchantStatus.equals("Active", true) || r.merchantStatus.equals("Onboarded", true)) DateUtils.getTodayDateString() else "",
                createdAt = now,
                updatedAt = now
            )
        }
    }

    /**
     * Generates a sample CSV template with standard columns including "User ID".
     * If existing users are available, uses active user credentials so the sample immediately validates!
     */
    fun getSampleMerchantCsv(users: List<UserEntity> = emptyList()): String {
        val bdoUsers = users.filter { it.role == "BDO" }.ifEmpty { users }
        val id1 = bdoUsers.getOrNull(0)?.username ?: "bdo_bilal"
        val id2 = bdoUsers.getOrNull(1)?.username ?: "bdo_hassan"
        val id3 = bdoUsers.getOrNull(2)?.username ?: "ali"

        return """User ID,Merchant ID,Merchant Name,Shop Name,Mobile,Address,City,Region,ASM,TL,Latitude,Longitude,QR ID,Category,Merchant Status,QR Status
$id1,M1001,Muhammad Bilal,Bilal General Store,03001234567,Shop 12 Main Commercial Bazar,Lahore,North Region,Kashif Raza,Ahmed Khan,31.5245,74.3590,QR-LHR-001,Grocery,Active,Deployed
$id2,M1002,Hassan Tariq,Tariq Sweets & Bakers,03007654321,Shop 4 Commercial Area,Lahore,North Region,Kashif Raza,Ahmed Khan,31.5215,74.3510,QR-LHR-002,Bakery,Active,Deployed
$id3,M1003,Sheikh Imran,Sheikh Electronics,03211122334,Hall Road Electronics Market,Lahore,North Region,Kashif Raza,Ahmed Khan,31.5360,74.3530,QR-LHR-003,Electronics,New,Not Deployed
$id1,M1004,Dr. Abdul Ghani,Ghani Medicos,03335554433,Model Town Block C,Lahore,North Region,Kashif Raza,Ahmed Khan,31.5165,74.3495,QR-LHR-004,Pharmacy,Onboarded,Deployed
$id2,M1005,Zahid Hussain,Zahid Mobile Telecom,03450001122,Old Anarkali Bazaar,Lahore,North Region,Kashif Raza,Imran Malik,31.5204,74.3587,,Mobile,New,Not Deployed
"""
    }

    /**
     * Saves sample template CSV to device cache and triggers Android Chooser
     * so user can save to Downloads, Drive, or open directly in Microsoft Excel / Google Sheets.
     */
    fun downloadOrShareSampleTemplate(context: Context, sampleCsv: String) {
        try {
            val fileName = "merchant_upload_sample.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(sampleCsv.toByteArray(Charsets.UTF_8))
            }

            // Also copy to Downloads folder if possible
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null && downloadsDir.exists()) {
                    val publicFile = File(downloadsDir, fileName)
                    publicFile.writeText(sampleCsv)
                }
            } catch (_: Exception) {}

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Merchant Upload Sample Template")
                putExtra(Intent.EXTRA_TEXT, "Here is the sample format for uploading merchants into QR Friend.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Download / Open Sample Template")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Toast.makeText(context, "Sample template ready to save or open in Excel!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open share chooser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
