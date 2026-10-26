package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ParkingLandmark
import com.example.data.model.ParkingSpot
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardAmber
import com.example.ui.theme.YardCyan
import com.example.ui.theme.YardGreen
import com.example.ui.theme.YardRed

@Composable
fun SchemaEditorToolbar(
    showGrid: Boolean,
    hasUnsavedChanges: Boolean,
    spotsCount: Int,
    landmarksCount: Int,
    onToggleGrid: () -> Unit,
    onAddSpot: () -> Unit,
    onAddRow: () -> Unit,
    onAddLandmark: () -> Unit,
    onOpenElements: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("schema_editor_toolbar"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E232A)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(YardAmber)),
        shape = RoundedCornerShape(12.dp)
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
                            .size(8.dp)
                            .background(if (hasUnsavedChanges) YardAmber else YardGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🛠️ Редактор схемы двора",
                        fontWeight = FontWeight.Bold,
                        color = YardAmber,
                        fontSize = 13.sp
                    )
                    if (hasUnsavedChanges) {
                        Text(
                            text = " • Есть правки",
                            color = YardAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Отмена", fontSize = 11.sp, color = DarkTextSecondary)
                    }
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = YardGreen),
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сохранить", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action tool strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddSpot,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C394B)),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = YardGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Место", fontSize = 12.sp, color = DarkTextPrimary)
                }

                Button(
                    onClick = onAddRow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C394B)),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.ViewModule, contentDescription = null, tint = YardCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Ряд мест", fontSize = 12.sp, color = DarkTextPrimary)
                }

                Button(
                    onClick = onAddLandmark,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C394B)),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF80AB), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Объект", fontSize = 12.sp, color = DarkTextPrimary)
                }

                Button(
                    onClick = onOpenElements,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Все ($spotsCount/$landmarksCount)", fontSize = 12.sp, color = DarkTextSecondary)
                }

                OutlinedButton(
                    onClick = onToggleGrid,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showGrid) YardAmber else DarkBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (showGrid) YardAmber.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.GridOn,
                        contentDescription = null,
                        tint = if (showGrid) YardAmber else DarkTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (showGrid) "Сетка: Вкл" else "Сетка: Выкл",
                        fontSize = 11.sp,
                        color = if (showGrid) YardAmber else DarkTextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotInspectorCard(
    spot: ParkingSpot,
    onClose: () -> Unit,
    onNudge: (dx: Float, dy: Float) -> Unit,
    onSetAngle: (angle: Float) -> Unit,
    onDuplicate: () -> Unit,
    onChangeId: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nudgeStep by remember { mutableFloatStateOf(5f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spot_inspector_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF192028)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(YardCyan)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(YardCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${spot.id}",
                            fontWeight = FontWeight.Bold,
                            color = YardCyan,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Машиноместо №${spot.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "X: ${spot.x.toInt()}  Y: ${spot.y.toInt()}  •  Угол: ${(spot.angle ?: 0f).toInt()}°",
                            fontSize = 11.sp,
                            color = DarkTextSecondary
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = DarkTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Orientation Presets & Slider
            Text(
                text = "Ориентация машины на схеме:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val currentAngle = (spot.angle ?: 0f).toInt()
                val presets = listOf(
                    0 to "0° Верт.",
                    45 to "45° Ёлочкой",
                    90 to "90° Гориз.",
                    135 to "135°",
                    180 to "180°",
                    270 to "270°",
                    315 to "-45°"
                )
                presets.forEach { (ang, title) ->
                    val isSelected = currentAngle == ang || (ang == 315 && currentAngle == -45)
                    Surface(
                        onClick = { onSetAngle(ang.toFloat()) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) YardCyan else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) YardCyan else DarkBorder
                        ),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else DarkTextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSetAngle((((spot.angle ?: 0f) - 5f + 360f) % 360f)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("-5°", fontSize = 11.sp, color = YardCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = spot.angle ?: 0f,
                    onValueChange = { onSetAngle(it) },
                    valueRange = 0f..360f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = YardCyan,
                        activeTrackColor = YardCyan,
                        inactiveTrackColor = DarkBorder
                    )
                )
                IconButton(
                    onClick = { onSetAngle((((spot.angle ?: 0f) + 5f) % 360f)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("+5°", fontSize = 11.sp, color = YardCyan, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Precision Joystick / Nudge Pad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Точная подгонка координат:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextSecondary
                )

                // Step toggle
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1f to "1px", 5f to "5px", 20f to "20px").forEach { (stepVal, title) ->
                        val isSelected = nudgeStep == stepVal
                        Surface(
                            onClick = { nudgeStep = stepVal },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) YardAmber else DarkSurfaceVariant,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else DarkTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Directional cross buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NudgeButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Влево",
                    onClick = { onNudge(-nudgeStep, 0f) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NudgeButton(
                        icon = Icons.Default.ArrowUpward,
                        label = "Вверх",
                        onClick = { onNudge(0f, -nudgeStep) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    NudgeButton(
                        icon = Icons.Default.ArrowDownward,
                        label = "Вниз",
                        onClick = { onNudge(0f, nudgeStep) }
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                NudgeButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    label = "Вправо",
                    onClick = { onNudge(nudgeStep, 0f) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDuplicate,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C394B)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = YardGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Копия (+1)", fontSize = 12.sp, color = DarkTextPrimary)
                }

                Button(
                    onClick = onChangeId,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = YardAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Номер №", fontSize = 12.sp, color = DarkTextPrimary)
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E1E1E)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = YardRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Удалить", fontSize = 12.sp, color = YardRed)
                }
            }
        }
    }
}

@Composable
fun LandmarkInspectorCard(
    landmark: ParkingLandmark,
    onClose: () -> Unit,
    onNudge: (dx: Float, dy: Float) -> Unit,
    onUpdateText: (newText: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nudgeStep by remember { mutableFloatStateOf(5f) }
    var currentText by remember(landmark.id) { mutableStateOf(landmark.text) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("landmark_inspector_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF231C24)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF4081))),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF80AB))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Объект на карте",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "X: ${landmark.x.toInt()}  Y: ${landmark.y.toInt()}",
                            fontSize = 11.sp,
                            color = DarkTextSecondary
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = DarkTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text field
            OutlinedTextField(
                value = currentText,
                onValueChange = {
                    currentText = it
                    onUpdateText(it)
                },
                label = { Text("Название или надпись") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF4081),
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = DarkTextPrimary,
                    unfocusedTextColor = DarkTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick preset chips
            Text(
                text = "Быстрый выбор типа объекта:",
                fontSize = 11.sp,
                color = DarkTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))

            val presets = listOf(
                "🚧 Шлагбаум",
                "🚪 Подъезд 1",
                "🚪 Подъезд 2",
                "🚪 Подъезд 3",
                "🚪 Подъезд 4",
                "🗑️ Мусорная площадка",
                "⚡ Электрощит / ТП",
                "🛑 Въезд во двор",
                "🛣️ Выезд со двора",
                "🌳 Зеленая зона",
                "📹 Видеонаблюдение",
                "🏪 Магазин",
                "🛝 Детская площадка"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    Surface(
                        onClick = {
                            currentText = preset
                            onUpdateText(preset)
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = preset,
                            fontSize = 11.sp,
                            color = DarkTextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Precision Move Joystick
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Перемещение объекта:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1f to "1px", 5f to "5px", 20f to "20px").forEach { (stepVal, title) ->
                        val isSelected = nudgeStep == stepVal
                        Surface(
                            onClick = { nudgeStep = stepVal },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFFFF4081) else DarkSurfaceVariant,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else DarkTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NudgeButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Влево",
                    onClick = { onNudge(-nudgeStep, 0f) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NudgeButton(
                        icon = Icons.Default.ArrowUpward,
                        label = "Вверх",
                        onClick = { onNudge(0f, -nudgeStep) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    NudgeButton(
                        icon = Icons.Default.ArrowDownward,
                        label = "Вниз",
                        onClick = { onNudge(0f, nudgeStep) }
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                NudgeButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    label = "Вправо",
                    onClick = { onNudge(nudgeStep, 0f) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E1E1E)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = YardRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Удалить объект с карты", fontSize = 12.sp, color = YardRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NudgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = DarkTextPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun AddSpotDialog(
    suggestedId: Int,
    onDismiss: () -> Unit,
    onConfirm: (id: Int, x: Float, y: Float, angle: Float) -> Unit
) {
    var idText by remember { mutableStateOf(suggestedId.toString()) }
    var xText by remember { mutableStateOf("480") }
    var yText by remember { mutableStateOf("200") }
    var angle by remember { mutableFloatStateOf(0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = YardGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить машиноместо", fontWeight = FontWeight.Bold, color = DarkTextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = idText,
                    onValueChange = { idText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Номер места (ID)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YardGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Ориентация:", fontSize = 12.sp, color = DarkTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0f to "0° Верт.", 45f to "45° Ёлочкой", 90f to "90° Гориз.").forEach { (angVal, label) ->
                        val isSelected = angle == angVal
                        Surface(
                            onClick = { angle = angVal },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) YardGreen else DarkSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else DarkTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = xText,
                        onValueChange = { xText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("X (0..960)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YardGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = yText,
                        onValueChange = { yText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Y (0..440)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YardGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = idText.toIntOrNull() ?: suggestedId
                    val x = xText.toFloatOrNull() ?: 480f
                    val y = yText.toFloatOrNull() ?: 200f
                    onConfirm(id, x, y, angle)
                },
                colors = ButtonDefaults.buttonColors(containerColor = YardGreen)
            ) {
                Text("Добавить", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена", color = DarkTextSecondary)
            }
        }
    )
}

@Composable
fun AddRowOfSpotsDialog(
    suggestedStartId: Int,
    onDismiss: () -> Unit,
    onConfirm: (count: Int, startId: Int, startX: Float, startY: Float, stepX: Float, stepY: Float, angle: Float) -> Unit
) {
    var count by remember { mutableIntStateOf(5) }
    var startIdText by remember { mutableStateOf(suggestedStartId.toString()) }
    var startXText by remember { mutableStateOf("100") }
    var startYText by remember { mutableStateOf("100") }
    var presetMode by remember { mutableIntStateOf(0) } // 0: Horiz vertical cars, 1: 45° herringbone, 2: 90° parallel, 3: Vert row

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ViewModule, contentDescription = null, tint = YardCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Создать ряд машиномест", fontWeight = FontWeight.Bold, color = DarkTextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Количество машин в ряду: $count",
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary,
                    fontSize = 14.sp
                )
                Slider(
                    value = count.toFloat(),
                    onValueChange = { count = it.toInt() },
                    valueRange = 2f..15f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = YardCyan,
                        activeTrackColor = YardCyan,
                        inactiveTrackColor = DarkBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = startIdText,
                    onValueChange = { startIdText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Начальный номер (ID)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YardCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Тип и ориентация ряда:", fontSize = 12.sp, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))

                val rowPresets = listOf(
                    "Горизонтальный ряд (0° перпендикулярно)" to 0,
                    "Парковка «Ёлочкой» (45° наискосок)" to 1,
                    "Параллельная парковка (90° вдоль)" to 2,
                    "Вертикальный ряд мест" to 3
                )

                rowPresets.forEach { (title, idx) ->
                    val isSelected = presetMode == idx
                    Surface(
                        onClick = { presetMode = idx },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) YardCyan.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) YardCyan else DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSelected) "● " else "○ ",
                                color = if (isSelected) YardCyan else DarkTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                color = if (isSelected) YardCyan else DarkTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startXText,
                        onValueChange = { startXText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Старт X") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YardCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = startYText,
                        onValueChange = { startYText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Старт Y") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YardCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startId = startIdText.toIntOrNull() ?: suggestedStartId
                    val startX = startXText.toFloatOrNull() ?: 100f
                    val startY = startYText.toFloatOrNull() ?: 100f

                    val (stepX, stepY, angle) = when (presetMode) {
                        0 -> Triple(42f, 0f, 0f)       // Horizontal row of vertical cars
                        1 -> Triple(45f, 0f, 45f)      // Herringbone angled
                        2 -> Triple(72f, 0f, 90f)      // Parallel parking
                        3 -> Triple(0f, 42f, 90f)      // Vertical column
                        else -> Triple(42f, 0f, 0f)
                    }

                    onConfirm(count, startId, startX, startY, stepX, stepY, angle)
                },
                colors = ButtonDefaults.buttonColors(containerColor = YardCyan)
            ) {
                Text("Создать ряд", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена", color = DarkTextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLandmarkDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, x: Float, y: Float) -> Unit
) {
    var text by remember { mutableStateOf("🚧 Шлагбаум") }
    var xText by remember { mutableStateOf("480") }
    var yText by remember { mutableStateOf("200") }

    val presets = listOf(
        "🚧 Шлагбаум",
        "🚪 Подъезд 1",
        "🚪 Подъезд 2",
        "🚪 Подъезд 3",
        "🚪 Подъезд 4",
        "🗑️ Мусорная площадка",
        "⚡ Электрощит / ТП",
        "🛑 Въезд во двор",
        "🛣️ Выезд со двора",
        "🌳 Зеленая зона",
        "📹 Камера наблюдения",
        "🏪 Магазин",
        "🛝 Детская площадка",
        "🅿️ Гостевая стоянка"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF80AB))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить объект двора", fontWeight = FontWeight.Bold, color = DarkTextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Название или надпись") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF4081),
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Готовые типы объектов двора:", fontSize = 12.sp, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = text == preset
                        Surface(
                            onClick = { text = preset },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFFF4081) else DarkSurfaceVariant,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else DarkTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = xText,
                        onValueChange = { xText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Координата X") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF4081),
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = yText,
                        onValueChange = { yText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Координата Y") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF4081),
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        val x = xText.toFloatOrNull() ?: 480f
                        val y = yText.toFloatOrNull() ?: 200f
                        onConfirm(text, x, y)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
            ) {
                Text("Добавить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена", color = DarkTextSecondary)
            }
        }
    )
}

@Composable
fun ChangeSpotIdDialog(
    currentId: Int,
    onDismiss: () -> Unit,
    onConfirm: (newId: Int) -> Unit
) {
    var idText by remember { mutableStateOf(currentId.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Изменить номер места №$currentId", fontWeight = FontWeight.Bold, color = DarkTextPrimary)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = idText,
                    onValueChange = { idText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Новый номер места") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YardAmber,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newId = idText.toIntOrNull()
                    if (newId != null && newId > 0) {
                        onConfirm(newId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = YardAmber)
            ) {
                Text("Применить", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена", color = DarkTextSecondary)
            }
        }
    )
}

@Composable
fun SchemaElementsDialog(
    spots: List<ParkingSpot>,
    landmarks: List<ParkingLandmark>,
    selectedSpotId: Int?,
    selectedLandmarkId: String?,
    onSelectSpot: (Int) -> Unit,
    onSelectLandmark: (String) -> Unit,
    onDeleteSpot: (Int) -> Unit,
    onDeleteLandmark: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Элементы схемы двора",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = DarkTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = DarkTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurfaceVariant,
                    contentColor = DarkTextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = if (selectedTab == 0) YardCyan else Color(0xFFFF80AB)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Места (${spots.size})", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Объекты (${landmarks.size})", fontSize = 13.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    val sortedSpots = remember(spots) { spots.sortedBy { it.id } }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sortedSpots, key = { it.id }) { spot ->
                            val isSelected = spot.id == selectedSpotId
                            Surface(
                                onClick = {
                                    onSelectSpot(spot.id)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) YardCyan.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) YardCyan else DarkBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "№${spot.id}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isSelected) YardCyan else DarkTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Координаты: (${spot.x.toInt()}, ${spot.y.toInt()})",
                                                fontSize = 11.sp,
                                                color = DarkTextSecondary
                                            )
                                            Text(
                                                text = "Угол: ${(spot.angle ?: 0f).toInt()}° • ${if (spot.occupiedBy == null) "Свободно" else "Занято"}",
                                                fontSize = 11.sp,
                                                color = if (spot.occupiedBy == null) YardGreen else YardAmber
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteSpot(spot.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = YardRed.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(landmarks, key = { it.id }) { lm ->
                            val isSelected = lm.id == selectedLandmarkId
                            Surface(
                                onClick = {
                                    onSelectLandmark(lm.id)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFF4081).copy(alpha = 0.2f) else DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF4081) else DarkBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF80AB), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = lm.text.ifBlank { "Объект" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = DarkTextPrimary
                                            )
                                            Text(
                                                text = "X: ${lm.x.toInt()}, Y: ${lm.y.toInt()}",
                                                fontSize = 11.sp,
                                                color = DarkTextSecondary
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteLandmark(lm.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = YardRed.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
