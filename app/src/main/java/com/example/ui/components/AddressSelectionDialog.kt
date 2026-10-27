package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.YardItem
import com.example.data.model.YardTemplateType
import com.example.data.model.YardTemplates
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardCyan
import com.example.ui.theme.YardGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddressSelectionDialog(
    currentAddress: String,
    currentYardId: String,
    knownYards: List<YardItem>,
    alwaysAskOnLaunch: Boolean,
    canDismiss: Boolean,
    onSelectYard: (address: String, template: YardTemplateType) -> Unit,
    onRemoveYard: (yardId: String) -> Unit,
    onToggleAlwaysAsk: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var searchInput by remember { mutableStateOf(currentAddress) }
    var selectedTemplate by remember { mutableStateOf(YardTemplateType.STANDARD_2ROWS) }
    var alwaysAskState by remember { mutableStateOf(alwaysAskOnLaunch) }

    val filteredYards = remember(searchInput, knownYards) {
        val query = searchInput.trim().lowercase()
        if (query.isBlank()) {
            knownYards
        } else {
            knownYards.filter {
                it.address.lowercase().contains(query) ||
                it.name.lowercase().contains(query) ||
                it.city.lowercase().contains(query)
            }
        }
    }

    val searchKey = YardTemplates.addressKey(searchInput)
    val exactMatch = knownYards.firstOrNull {
        it.address.trim().equals(searchInput.trim(), ignoreCase = true) ||
            (searchKey.isNotBlank() && YardTemplates.addressKey(it.address) == searchKey)
    }
    val isNewAddress = searchInput.isNotBlank() && exactMatch == null

    Dialog(
        onDismissRequest = {
            if (canDismiss) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground.copy(alpha = 0.94f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(24.dp))
                    .testTag("dialog_address_selection"),
                color = DarkSurface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(YardGreen.copy(alpha = 0.25f), YardCyan.copy(alpha = 0.25f))
                                        )
                                    )
                                    .border(BorderStroke(1.dp, YardGreen.copy(alpha = 0.6f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = YardGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Выбор двора",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary
                                )
                                Text(
                                    text = "Наберите адрес дома для перехода к схеме",
                                    fontSize = 12.sp,
                                    color = DarkTextSecondary
                                )
                                Text(
                                    text = "Версия ${com.example.BuildConfig.VERSION_NAME}",
                                    fontSize = 11.sp,
                                    color = DarkTextMuted
                                )
                            }
                        }

                        if (canDismiss) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("button_close_address_dialog")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = DarkTextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Address Search / Input Field
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_yard_address"),
                        placeholder = {
                            Text("Например: Владивосток, Карбышева 22", color = DarkTextMuted, fontSize = 14.sp)
                        },
                        label = { Text("Адрес двора") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = YardGreen)
                        },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { searchInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = DarkTextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YardGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedLabelColor = YardGreen,
                            unfocusedLabelColor = DarkTextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick City / Sample chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Быстрый выбор:",
                            fontSize = 11.sp,
                            color = DarkTextMuted,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        QuickAddressChip(
                            label = "Карбышева 22",
                            onClick = { searchInput = "ул. Карбышева, 22 / 22А" }
                        )
                        QuickAddressChip(
                            label = "Океанский 100",
                            onClick = { searchInput = "Океанский проспект, 100" }
                        )
                        QuickAddressChip(
                            label = "Светланская 45",
                            onClick = { searchInput = "ул. Светланская, 45" }
                        )
                        QuickAddressChip(
                            label = "Русская 46",
                            onClick = { searchInput = "ул. Русская, 46" }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable content: existing yards and new yard template builder
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // If new address typed, show template picker card
                        if (isNewAddress) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(BorderStroke(1.dp, YardCyan.copy(alpha = 0.7f)), RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                tint = YardCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Новый двор: «${searchInput.trim()}»",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = YardCyan,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Адрес появится в общем списке у всех жителей. Выберите стартовую схему (администратор сможет её перестроить):",
                                            fontSize = 12.sp,
                                            color = DarkTextSecondary
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        YardTemplateType.values().forEach { template ->
                                            TemplateOptionItem(
                                                template = template,
                                                isSelected = selectedTemplate == template,
                                                onSelect = { selectedTemplate = template }
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = if (searchInput.isBlank()) "Сохранённые и доступные дворы" else "Результаты поиска",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (filteredYards.isEmpty() && !isNewAddress) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Дворы не найдены. Введите новый адрес выше.",
                                        fontSize = 13.sp,
                                        color = DarkTextMuted
                                    )
                                }
                            }
                        }

                        items(filteredYards, key = { it.id }) { yard ->
                            val isCurrent = yard.id == currentYardId || yard.address.trim().equals(searchInput.trim(), ignoreCase = true)
                            YardListItemCard(
                                yard = yard,
                                isCurrent = isCurrent,
                                onSelect = {
                                    searchInput = yard.address
                                    onSelectYard(yard.address, YardTemplateType.STANDARD_2ROWS)
                                },
                                onRemove = if (!yard.isDefault) {
                                    { onRemoveYard(yard.id) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom settings and Open Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                alwaysAskState = !alwaysAskState
                                onToggleAlwaysAsk(alwaysAskState)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = alwaysAskState,
                            onCheckedChange = {
                                alwaysAskState = it
                                onToggleAlwaysAsk(it)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = YardGreen,
                                uncheckedColor = DarkTextMuted,
                                checkmarkColor = Color.Black
                            )
                        )
                        Text(
                            text = "Спрашивать адрес при каждом запуске приложения",
                            fontSize = 12.sp,
                            color = DarkTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val canOpen = searchInput.isNotBlank()
                    Button(
                        onClick = {
                            if (canOpen) {
                                onSelectYard(searchInput.trim(), selectedTemplate)
                            }
                        },
                        enabled = canOpen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_open_yard_schema"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YardGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = DarkTextMuted
                        )
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNewAddress) "Создать и открыть схему" else "Открыть схему двора",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddressChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = YardGreen,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun YardListItemCard(
    yard: YardItem,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                BorderStroke(
                    width = if (isCurrent) 1.5.dp else 1.dp,
                    color = if (isCurrent) YardGreen else DarkBorder
                ),
                RoundedCornerShape(14.dp)
            )
            .testTag("yard_item_${yard.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) DarkSurfaceVariant.copy(alpha = 0.9f) else DarkSurfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCurrent) YardGreen.copy(alpha = 0.2f) else DarkBorder.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (yard.isDefault) Icons.Default.Domain else Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = if (isCurrent) YardGreen else DarkTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = yard.address,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (yard.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = YardGreen.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, YardGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Сервер",
                                fontSize = 9.sp,
                                color = YardGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${yard.city} • ${if (yard.spotsCount > 0) "${yard.spotsCount} мест" else "Схема настраивается"}",
                        fontSize = 11.sp,
                        color = DarkTextMuted
                    )
                    if (yard.freeSpotsCount > 0) {
                        Text(
                            text = " (${yard.freeSpotsCount} свободно)",
                            fontSize = 11.sp,
                            color = YardGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(YardGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            } else if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить двор",
                        tint = DarkTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TemplateOptionItem(
    template: YardTemplateType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) YardCyan.copy(alpha = 0.15f) else DarkSurface,
        border = BorderStroke(
            1.dp,
            if (isSelected) YardCyan else DarkBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isSelected) YardCyan else DarkTextMuted, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(YardCyan)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = template.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) YardCyan else DarkTextPrimary
                )
                Text(
                    text = template.description,
                    fontSize = 11.sp,
                    color = DarkTextSecondary
                )
            }
        }
    }
}
