package com.example.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RedSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.DateUtils
import com.example.util.GeoUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapViewScreen(
    visits: List<VisitEntity>,
    merchants: List<MerchantEntity>,
    users: List<UserEntity>,
    onSelectVisit: (VisitEntity) -> Unit
) {
    var selectedVisit by remember { mutableStateOf(visits.find { it.gpsStatus == "GPS MISMATCH" } ?: visits.firstOrNull()) }
    var showOnlyExceptions by remember { mutableStateOf(false) }

    val displayedVisits = if (showOnlyExceptions) {
        visits.filter { it.gpsStatus == "GPS MISMATCH" }
    } else {
        visits
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("map_view_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GPS Map & Verification Visualizer",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                )
                Text(
                    text = "Registered Merchant coordinates vs BDO Visit location with Haversine distance lines",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }
        }

        // Map Legend and Filter toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legend
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LegendItem(color = PrimaryBlue, label = "Registered Merchant")
                    LegendItem(color = EmeraldSuccess, label = "Valid Visit (≤100m)")
                    LegendItem(color = RedDanger, label = "GPS Mismatch (>100m)")
                }

                Surface(
                    color = if (showOnlyExceptions) RedSubtle else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { showOnlyExceptions = !showOnlyExceptions }
                ) {
                    Text(
                        text = if (showOnlyExceptions) "Show All" else "Exceptions Only",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (showOnlyExceptions) RedDanger else Navy900
                        )
                    )
                }
            }
        }

        // Interactive Map Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("interactive_map_canvas"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // dark tactical navy map
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Canvas with grid and nodes
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(displayedVisits) {
                            detectTapGestures { offset ->
                                // Hit detection to select nearest visit
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()

                                val nearest = displayedVisits.minByOrNull { v ->
                                    val (vx, vy) = mapCoordsToScreen(v.latitude, v.longitude, width, height)
                                    val dist = kotlin.math.hypot(offset.x - vx, offset.y - vy)
                                    dist
                                }
                                if (nearest != null) {
                                    selectedVisit = nearest
                                }
                            }
                        }
                ) {
                    drawMapGrid(size.width, size.height)

                    // Draw all visits and lines
                    displayedVisits.forEach { visit ->
                        val (vx, vy) = mapCoordsToScreen(visit.latitude, visit.longitude, size.width, size.height)
                        val (mx, my) = mapCoordsToScreen(visit.merchantLatitude, visit.merchantLongitude, size.width, size.height)

                        val isSelected = selectedVisit?.id == visit.id
                        val isMismatch = visit.gpsStatus == "GPS MISMATCH"
                        val lineColor = if (isMismatch) RedDanger else EmeraldDark

                        // Draw distance connector line between registered merchant and actual visit
                        drawLine(
                            color = lineColor.copy(alpha = if (isSelected) 0.95f else 0.4f),
                            start = Offset(mx, my),
                            end = Offset(vx, vy),
                            strokeWidth = if (isSelected) 3.5f else 2.0f,
                            pathEffect = if (isMismatch) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
                        )

                        // Draw Registered Merchant Pin (Blue ring)
                        drawCircle(
                            color = PrimaryBlueLight,
                            radius = if (isSelected) 10f else 7f,
                            center = Offset(mx, my)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 4f else 3f,
                            center = Offset(mx, my)
                        )

                        // Draw BDO Visit Pin (Green if Valid, Red if Mismatch)
                        val visitColor = if (isMismatch) RedDanger else EmeraldSuccess
                        drawCircle(
                            color = visitColor,
                            radius = if (isSelected) 12f else 8f,
                            center = Offset(vx, vy)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 5f else 3f,
                            center = Offset(vx, vy)
                        )

                        // Draw halo around selected
                        if (isSelected) {
                            drawCircle(
                                color = visitColor.copy(alpha = 0.35f),
                                radius = 24f,
                                center = Offset(vx, vy),
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }
                }

                // Map Overlay Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live GPS Layer: ${displayedVisits.size} Visits plotted",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Horizontal Quick-Selector Bar for Visits
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedVisits) { visit ->
                val merchant = merchants.find { it.merchantId == visit.merchantId }
                val isSelected = selectedVisit?.id == visit.id
                val isMismatch = visit.gpsStatus == "GPS MISMATCH"

                Surface(
                    color = when {
                        isSelected -> if (isMismatch) RedSubtle else PrimaryBlueSubtle
                        else -> MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { selectedVisit = visit }
                        .testTag("map_item_${visit.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isMismatch) RedDanger else EmeraldSuccess)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = merchant?.shopName ?: visit.merchantId,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Navy900
                            )
                        )
                    }
                }
            }
        }

        // Selected Pin Comparison Popup Card
        if (selectedVisit != null) {
            val v = selectedVisit!!
            val m = merchants.find { it.merchantId == v.merchantId }
            val bdo = users.find { it.id == v.bdoId }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_visit_detail_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = m?.shopName ?: v.merchantId,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            )
                            Text(
                                text = "BDO: ${bdo?.name ?: "BDO"} • Visit Time: ${DateUtils.formatDateTime(v.visitTime)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                            )
                        }

                        // Status Badge
                        Surface(
                            color = if (v.gpsStatus == "VALID") EmeraldSubtle else RedSubtle,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${v.gpsStatus} (${GeoUtils.formatDistance(v.distanceMeters)})",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (v.gpsStatus == "VALID") EmeraldDark else RedDanger
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Comparison details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "🔵 Registered Location", style = MaterialTheme.typography.labelSmall.copy(color = PrimaryBlue, fontWeight = FontWeight.Bold))
                            Text(
                                text = GeoUtils.formatCoordinates(v.merchantLatitude, v.merchantLongitude),
                                style = MaterialTheme.typography.bodySmall.copy(color = Navy900)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "🔴 Actual Visit GPS", style = MaterialTheme.typography.labelSmall.copy(color = if (v.gpsStatus == "VALID") EmeraldDark else RedDanger, fontWeight = FontWeight.Bold))
                            Text(
                                text = "${GeoUtils.formatCoordinates(v.latitude, v.longitude)} (±${v.gpsAccuracy.toInt()}m)",
                                style = MaterialTheme.typography.bodySmall.copy(color = Navy900)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSelectVisit(v) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.testTag("map_view_visit_button")
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Audit & Photo Details", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = Navy900))
    }
}

private fun DrawScope.drawMapGrid(width: Float, height: Float) {
    val step = 60f
    val gridColor = Color(0xFF1E293B)

    var x = 0f
    while (x < width) {
        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
        x += step
    }

    var y = 0f
    while (y < height) {
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
        y += step
    }
}

/**
 * Normalizes latitude and longitude to canvas coordinates
 */
private fun mapCoordsToScreen(lat: Double, lon: Double, width: Float, height: Float): Pair<Float, Float> {
    // Normalization bounds around Lahore / Karachi area
    val minLat = 24.5
    val maxLat = 32.0
    val minLon = 66.5
    val maxLon = 75.0

    val normX = ((lon - minLon) / (maxLon - minLon)).coerceIn(0.08, 0.92).toFloat()
    val normY = (1.0 - ((lat - minLat) / (maxLat - minLat))).coerceIn(0.08, 0.92).toFloat()

    return Pair(normX * width, normY * height)
}
