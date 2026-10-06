package com.example.ui.bdo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MerchantEntity
import com.example.ui.admin.QrBadge
import com.example.ui.admin.StatusBadge
import com.example.ui.theme.*

@Composable
fun BdoMerchantsScreen(
    merchants: List<MerchantEntity>,
    onStartVisit: (MerchantEntity) -> Unit,
    onViewDetail: (MerchantEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedQrFilter by remember { mutableStateOf("All") } // "All", "Deployed", "Not Deployed"

    val filteredMerchants = merchants.filter { m ->
        val matchesQuery = searchQuery.isBlank() ||
                m.shopName.contains(searchQuery, ignoreCase = true) ||
                m.merchantId.contains(searchQuery, ignoreCase = true) ||
                m.merchantName.contains(searchQuery, ignoreCase = true) ||
                m.address.contains(searchQuery, ignoreCase = true)

        val matchesQr = when (selectedQrFilter) {
            "Deployed" -> m.qrStatus == "Deployed" || m.qrStatus == "Active"
            "Not Deployed" -> m.qrStatus == "Not Deployed"
            else -> true
        }

        matchesQuery && matchesQr
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(16.dp)
            .testTag("bdo_merchants_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Assigned Merchants",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextHeadings
                    )
                )
                Text(
                    text = "${filteredMerchants.size} of ${merchants.size} shops in your territory",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search shop, ID, or area...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bdo_merchant_search_field"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandGreen,
                focusedLabelColor = BrandGreenDark,
                cursorColor = BrandGreen
            )
        )

        // Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Deployed", "Not Deployed").forEach { filter ->
                val isSelected = selectedQrFilter == filter
                Button(
                    onClick = { selectedQrFilter = filter },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) ButtonPrimaryBg else CardWhite,
                        contentColor = if (isSelected) ButtonPrimaryText else TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = if (!isSelected) BorderStroke(1.dp, BorderColor) else null,
                    elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 1.dp) else null
                ) {
                    Text(text = filter, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Merchants List
        if (filteredMerchants.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No merchants found for your search query.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMerchants, key = { it.id }) { merchant ->
                    BdoMerchantFullCard(
                        merchant = merchant,
                        onVisit = { onStartVisit(merchant) },
                        onView = { onViewDetail(merchant) }
                    )
                }
            }
        }
    }
}

@Composable
fun BdoMerchantFullCard(
    merchant: MerchantEntity,
    onVisit: () -> Unit,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bdo_card_${merchant.merchantId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = merchant.shopName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextHeadings
                        )
                    )
                    Text(
                        text = "ID: ${merchant.merchantId} • Owner: ${merchant.merchantName}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(status = merchant.merchantStatus)
                    QrBadge(status = merchant.qrStatus)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = merchant.mobile,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = BrandGreenDark)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = merchant.city,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = merchant.address,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onView,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ButtonSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonSecondaryText)
                ) {
                    Text("View History", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onVisit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimaryBg,
                        contentColor = ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("visit_btn_${merchant.merchantId}")
                ) {
                    Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Field Visit", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
