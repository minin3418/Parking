package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParkingBlocker
import com.example.data.model.ParkingSpot
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardAmber
import com.example.ui.theme.YardGreen
import com.example.ui.theme.YardRed

@Composable
fun SpotsListView(
    spots: List<ParkingSpot>,
    blockers: List<ParkingBlocker>,
    searchQuery: String,
    filterMode: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSpotClick: (ParkingSpot) -> Unit,
    modifier: Modifier = Modifier
) {
    val blockedSpotIds = blockers.flatMap { it.blockedSpots }.toSet()

    val filteredSpots = spots.filter { spot ->
        val matchesFilter = when (filterMode) {
            "FREE" -> spot.isFree
            "OCCUPIED" -> !spot.isFree
            "BLOCKED" -> blockedSpotIds.contains(spot.id)
            else -> true
        }
        val query = searchQuery.trim().lowercase()
        val matchesSearch = if (query.isEmpty()) true else {
            spot.id.toString().contains(query) ||
                    (spot.occupiedBy?.carNumber?.lowercase()?.contains(query) == true) ||
                    (spot.occupiedBy?.apt?.lowercase()?.contains(query) == true) ||
                    (spot.occupiedBy?.phone?.lowercase()?.contains(query) == true)
        }
        matchesFilter && matchesSearch
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Search & Filter header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Поиск по номеру места, авто или кв.") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = DarkTextSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_spots"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YardGreen,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = DarkTextPrimary,
                    unfocusedTextColor = DarkTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { onFilterChange("ALL") },
                        label = { Text("Все (${spots.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YardGreen,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    val freeCount = spots.count { it.isFree }
                    FilterChip(
                        selected = filterMode == "FREE",
                        onClick = { onFilterChange("FREE") },
                        label = { Text("Свободные ($freeCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YardGreen,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    val occCount = spots.count { !it.isFree }
                    FilterChip(
                        selected = filterMode == "OCCUPIED",
                        onClick = { onFilterChange("OCCUPIED") },
                        label = { Text("Занятые ($occCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YardRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    val blkCount = spots.count { blockedSpotIds.contains(it.id) }
                    FilterChip(
                        selected = filterMode == "BLOCKED",
                        onClick = { onFilterChange("BLOCKED") },
                        label = { Text("Подпёртые ($blkCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YardAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        if (filteredSpots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Места не найдены", color = DarkTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSpots, key = { it.id }) { spot ->
                    val isBlocked = blockedSpotIds.contains(spot.id)
                    SpotListItemCard(
                        spot = spot,
                        isBlocked = isBlocked,
                        onClick = { onSpotClick(spot) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SpotListItemCard(
    spot: ParkingSpot,
    isBlocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("spot_card_${spot.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isBlocked) YardAmber else if (spot.isFree) YardGreen.copy(alpha = 0.5f) else DarkBorder
            )
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            if (spot.isFree) YardGreen.copy(alpha = 0.2f) else YardRed.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "№${spot.id}",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (spot.isFree) YardGreen else YardRed,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    if (spot.isFree) {
                        Text(
                            text = "Свободное место",
                            fontWeight = FontWeight.Bold,
                            color = YardGreen,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Нажмите, чтобы занять",
                            fontSize = 12.sp,
                            color = DarkTextSecondary
                        )
                    } else {
                        val occ = spot.occupiedBy!!
                        Text(
                            text = occ.carNumber,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Кв. ${occ.apt ?: "—"}",
                                fontSize = 12.sp,
                                color = DarkTextSecondary
                            )
                            if (!occ.leaveTime.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• до ${occ.leaveTime}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YardGreen
                                )
                            } else if (!occ.parkedAt.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${formatRelativeTime(occ.parkedAt)}",
                                    fontSize = 12.sp,
                                    color = YardAmber
                                )
                            }
                        }
                    }
                }
            }

            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .background(YardAmber.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = YardAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Подпёрли", color = YardAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (spot.isFree) {
                Text("Свободно", color = YardGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
