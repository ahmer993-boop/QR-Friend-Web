package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.AmberSubtle
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RedSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.ExcelCsvParser
import com.example.util.ImportValidationSummary
import com.example.util.ParsedMerchantRow
import com.example.util.XlsxReader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExcelUploadScreen(
    existingMerchants: List<MerchantEntity>,
    users: List<UserEntity>,
    regions: List<RegionEntity>,
    tls: List<TlEntity>,
    onImportMerchants: (List<MerchantEntity>, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var rawInputText by remember { mutableStateOf("") }
    var validationSummary by remember { mutableStateOf<ImportValidationSummary?>(null) }
    var importCompletedMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFormatGuideDialog by remember { mutableStateOf(false) }

    val bdos = users.filter { it.role == "BDO" }.ifEmpty { users }
    val bdoMap = users.flatMap { b ->
        listOf(
            b.username.lowercase() to b.id,
            b.id.toString() to b.id,
            b.name.lowercase() to b.id
        )
    }.toMap()
    val existingIds = existingMerchants.map { it.merchantId }.toSet()
    val defaultRegionId = regions.firstOrNull()?.id ?: 1L
    val defaultTlId = tls.firstOrNull()?.id ?: 1L

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isProcessingFile by remember { mutableStateOf(false) }

    fun getFileName(uri: Uri): String {
        var name = "selected_file.xlsx"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex) ?: name
                    }
                }
            }
        } catch (_: Exception) {
            val path = uri.path
            if (path != null) {
                val cut = path.lastIndexOf('/')
                if (cut != -1) {
                    name = path.substring(cut + 1)
                }
            }
        }
        return name
    }

    fun processSelectedFile(uri: Uri) {
        isProcessingFile = true
        errorMessage = null
        try {
            val parsedContent = XlsxReader.readUriToCsvText(context, uri)
            if (parsedContent.isNotBlank()) {
                rawInputText = parsedContent
                validationSummary = ExcelCsvParser.parseAndValidate(
                    rawContent = parsedContent,
                    existingMerchantIds = existingIds,
                    validBdosMap = bdoMap,
                    defaultRegionId = defaultRegionId,
                    defaultTlId = defaultTlId,
                    usersList = users
                )
                importCompletedMessage = null
                errorMessage = null
                Toast.makeText(context, "File processed successfully! Review records below.", Toast.LENGTH_SHORT).show()
            } else {
                errorMessage = "Selected file appears to be empty or unreadable."
                Toast.makeText(context, "Selected file is empty or unreadable.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            errorMessage = "Error reading file: ${e.message}"
            Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isProcessingFile = false
        }
    }

    // File picker launcher for .xlsx, .csv, .tsv, .txt
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = getFileName(uri)
            errorMessage = null
            Toast.makeText(context, "File attached: ${selectedFileName}. Click 'Process & Upload File' to parse.", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(text: String, label: String = "Merchant Data") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        val text = item?.text?.toString() ?: ""
        if (text.isNotBlank()) {
            rawInputText = text
            validationSummary = ExcelCsvParser.parseAndValidate(
                rawContent = text,
                existingMerchantIds = existingIds,
                validBdosMap = bdoMap,
                defaultRegionId = defaultRegionId,
                defaultTlId = defaultTlId,
                usersList = users
            )
            importCompletedMessage = null
            errorMessage = null
            Toast.makeText(context, "Pasted from clipboard and validated!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("excel_upload_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Merchant Excel & CSV Upload",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    )
                }
                Text(
                    text = "Upload or paste merchant data. Specify the agent's User ID in each row to directly assign merchants.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }

            OutlinedButton(
                onClick = { showFormatGuideDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("view_format_guide_button")
            ) {
                Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Format Guide", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Quick Actions Banner (Download Sample, Upload File, Paste)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Setup & Data Source",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                )

                // Action buttons row
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Download Sample Template Button
                    Button(
                        onClick = {
                            val sampleCsv = ExcelCsvParser.getSampleMerchantCsv(users)
                            copyToClipboard(sampleCsv, "Sample Template CSV")
                            ExcelCsvParser.downloadOrShareSampleTemplate(context, sampleCsv)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("download_sample_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Sample Template", style = MaterialTheme.typography.labelSmall)
                    }

                    // 2. Upload / Select File Button (Excel .xlsx / .csv / .tsv)
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("upload_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedFileUri != null) "Change File" else "Choose File (.xlsx / .csv)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldDark
                        )
                    }

                    // 3. Paste from Clipboard
                    OutlinedButton(
                        onClick = { pasteFromClipboard() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("paste_clipboard_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste from Clipboard", style = MaterialTheme.typography.labelSmall)
                    }

                    // 4. Load Sample Dataset
                    OutlinedButton(
                        onClick = {
                            val sample = ExcelCsvParser.getSampleMerchantCsv(users)
                            rawInputText = sample
                            validationSummary = ExcelCsvParser.parseAndValidate(
                                rawContent = sample,
                                existingMerchantIds = existingIds,
                                validBdosMap = bdoMap,
                                defaultRegionId = defaultRegionId,
                                defaultTlId = defaultTlId,
                                usersList = users
                            )
                            importCompletedMessage = null
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("load_sample_csv_button")
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Sample In Editor", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Selected File Banner with Upload / Process Button
                if (selectedFileUri != null) {
                    Surface(
                        color = EmeraldSubtle,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = EmeraldDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Selected File:",
                                            style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                                        )
                                        Text(
                                            text = selectedFileName ?: "attached_file.xlsx",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        selectedFileUri = null
                                        selectedFileName = null
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove selected file",
                                        tint = SlateMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        selectedFileUri?.let { uri ->
                                            processSelectedFile(uri)
                                        }
                                    },
                                    enabled = !isProcessingFile,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("process_file_button")
                                ) {
                                    if (isProcessingFile) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Processing File...",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Upload & Process File",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isProcessingFile
                                ) {
                                    Text("Change", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Active Field Agents for direct assignment
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Registered Field Agents (User IDs for Excel Assignment):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                    }

                    if (users.isEmpty()) {
                        Text("No active user accounts found in database.", style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted))
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(users) { u ->
                                Surface(
                                    color = if (u.role == "BDO") EmeraldSubtle else PrimaryBlueSubtle,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.clickable {
                                        copyToClipboard(u.username, "User ID: ${u.username}")
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${u.username} (${u.role})",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (u.role == "BDO") EmeraldDark else PrimaryBlue
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy User ID",
                                            modifier = Modifier.size(10.dp),
                                            tint = SlateMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input field for pasting / editing CSV data
                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = {
                        rawInputText = it
                        if (it.isNotBlank()) {
                            validationSummary = ExcelCsvParser.parseAndValidate(
                                rawContent = it,
                                existingMerchantIds = existingIds,
                                validBdosMap = bdoMap,
                                defaultRegionId = defaultRegionId,
                                defaultTlId = defaultTlId,
                                usersList = users
                            )
                        } else {
                            validationSummary = null
                        }
                        importCompletedMessage = null
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("csv_input_field"),
                    placeholder = {
                        Text(
                            "Paste CSV or tab-separated Excel rows here...\n" +
                                    "Columns: User ID, Merchant ID, Merchant Name, Shop Name, Mobile, Address, City, Region, ASM, TL, Lat, Lon, QR ID, Category, Status, QR Status"
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                // Action row below editor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rawInputText.isNotBlank()) {
                        TextButton(
                            onClick = {
                                rawInputText = ""
                                validationSummary = null
                                importCompletedMessage = null
                                errorMessage = null
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Editor", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            validationSummary = ExcelCsvParser.parseAndValidate(
                                rawContent = rawInputText,
                                existingMerchantIds = existingIds,
                                validBdosMap = bdoMap,
                                defaultRegionId = defaultRegionId,
                                defaultTlId = defaultTlId,
                                usersList = users
                            )
                            importCompletedMessage = null
                            errorMessage = null
                        },
                        enabled = rawInputText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("validate_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate & Check Agent IDs", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Error message banner if any
        if (errorMessage != null) {
            Surface(
                color = RedSubtle,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = RedDanger)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = RedDanger)
                    )
                }
            }
        }

        // Success Confirmation Message
        if (importCompletedMessage != null) {
            Surface(
                color = EmeraldSubtle,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = importCompletedMessage!!,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                    )
                }
            }
        }

        // Validation Summary Cards
        if (validationSummary != null) {
            val sum = validationSummary!!

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Validation & Assignment Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )

                        Surface(
                            color = if (sum.validCount > 0) EmeraldSubtle else RedSubtle,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${sum.validCount} of ${sum.totalRows} Ready",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (sum.validCount > 0) EmeraldDark else RedDanger
                                )
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryBadge(label = "Total Rows", count = sum.totalRows, color = Navy900, bg = Color(0xFFF1F5F9))
                        SummaryBadge(label = "New Merchants", count = sum.newCount, color = EmeraldDark, bg = EmeraldSubtle)
                        SummaryBadge(label = "Updates", count = sum.updateCount, color = PrimaryBlue, bg = PrimaryBlueSubtle)
                        SummaryBadge(label = "Duplicates in File", count = sum.duplicateCount, color = RedDanger, bg = RedSubtle)
                        SummaryBadge(label = "Invalid Rows", count = sum.invalidCount, color = RedDanger, bg = RedSubtle)
                    }

                    HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (sum.validCount > 0)
                                "${sum.validCount} valid merchants will be created and directly assigned to their agent User IDs."
                            else
                                "No valid records. Fix the highlighted errors below before importing.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (sum.validCount > 0) EmeraldDark else RedDanger
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val validRows = sum.rows.filter { it.isValid }
                                val entities = ExcelCsvParser.toEntities(
                                    validRows = validRows,
                                    validBdosMap = bdoMap,
                                    defaultRegionId = defaultRegionId,
                                    defaultTlId = defaultTlId,
                                    usersList = users,
                                    regions = regions,
                                    tls = tls
                                )
                                onImportMerchants(entities) {
                                    importCompletedMessage = "Successfully imported ${entities.size} merchants and assigned directly to their agent User IDs!"
                                    rawInputText = ""
                                    validationSummary = null
                                    selectedFileUri = null
                                    selectedFileName = null
                                }
                            },
                            enabled = sum.validCount > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("confirm_import_button")
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Import & Assign (${sum.validCount})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Preview of rows
            Text(
                text = "Row-by-Row Review & Agent Assignment (${sum.rows.size} rows)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sum.rows, key = { "${it.rowNumber}_${it.merchantId}" }) { row ->
                    ParsedRowCard(row = row)
                }
            }
        }
    }

    // Format & Columns Guide Dialog
    if (showFormatGuideDialog) {
        AlertDialog(
            onDismissRequest = { showFormatGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excel / CSV Format Specification", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "To successfully import and assign merchants, ensure your Excel or CSV has the following columns:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Navy900)
                    )

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Required Columns:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                            )
                            Text("• User ID: Agent login ID (e.g. bdo_bilal, ali, 3) - Directly assigns merchant to this user.", style = MaterialTheme.typography.bodySmall)
                            Text("• Merchant ID: Unique ID (e.g. M1001, M1002)", style = MaterialTheme.typography.bodySmall)
                            Text("• Merchant Name / Shop Name: Store or owner name", style = MaterialTheme.typography.bodySmall)
                            Text("• Mobile: Primary contact number", style = MaterialTheme.typography.bodySmall)
                            Text("• Address & City: Physical shop location", style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Optional Columns:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SlateMuted)
                            )
                            Text("• Region, ASM, TL: Organizational hierarchy", style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted))
                            Text("• Latitude, Longitude: Coordinates for GPS verification", style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted))
                            Text("• QR ID, Category, Merchant Status, QR Status", style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted))
                        }
                    }

                    Text(
                        text = "Sample Header Line:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "User ID,Merchant ID,Merchant Name,Shop Name,Mobile,Address,City,Region,ASM,TL,Latitude,Longitude,QR ID,Category,Merchant Status,QR Status",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sampleCsv = ExcelCsvParser.getSampleMerchantCsv(users)
                        copyToClipboard(sampleCsv, "Sample CSV Template")
                        ExcelCsvParser.downloadOrShareSampleTemplate(context, sampleCsv)
                        showFormatGuideDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download Sample Template")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormatGuideDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SummaryBadge(label: String, count: Int, color: Color, bg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = color)
            )
        }
    }
}

@Composable
fun ParsedRowCard(row: ParsedMerchantRow) {
    Surface(
        color = if (row.isValid) EmeraldSubtle.copy(alpha = 0.5f) else RedSubtle,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (row.isValid) EmeraldSuccess else RedDanger),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (row.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Row ${row.rowNumber}: ${row.shopName} (${row.merchantId})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                }

                Surface(
                    color = if (row.isValid) EmeraldSubtle else RedSubtle,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (row.isValid) "VALID (${row.actionType.label})" else "INVALID",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (row.isValid) EmeraldDark else RedDanger
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Assigned Agent Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentInd,
                    contentDescription = null,
                    tint = if (row.assignedUser != null) EmeraldDark else PrimaryBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Directly Assigned Agent: ${row.assignedUserDisplay}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (row.assignedUser != null) EmeraldDark else Navy900
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mobile: ${row.mobile} • City: ${row.city} • Category: ${row.merchantCategory} • GPS: ${row.latitude ?: "N/A"}, ${row.longitude ?: "N/A"}",
                style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
            )

            if (!row.isValid) {
                Spacer(modifier = Modifier.height(4.dp))
                row.validationErrors.forEach { err ->
                    Text(
                        text = "❌ $err",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RedDanger)
                    )
                }
            }
        }
    }
}
