package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParkingBlocker
import com.example.data.model.ParkingSpot
import com.example.ui.LeaveTarget
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardAmber
import com.example.ui.theme.YardCyan
import com.example.ui.theme.YardGreen
import com.example.ui.theme.YardRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailSheet(
    spot: ParkingSpot,
    blocker: ParkingBlocker?,
    isAdmin: Boolean,
    initialCar: String,
    initialPhone: String,
    initialApt: String,
    onDismiss: () -> Unit,
    onPark: (car: String, apt: String, phone: String, leaveTime: String) -> Unit,
    onOpenLeave: (LeaveTarget) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var carInput by remember { mutableStateOf(initialCar) }
    var phoneInput by remember { mutableStateOf(initialPhone) }
    var aptInput by remember { mutableStateOf(initialApt) }
    var leaveTimeInput by remember { mutableStateOf("08:30 утра") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("spot_detail_sheet")
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
                            .background(
                                if (spot.isFree) YardGreen.copy(alpha = 0.2f) else YardRed.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Место №${spot.id}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (spot.isFree) YardGreen else YardRed
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (spot.isFree) "Свободно" else "Занято",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (spot.isFree) YardGreen else DarkTextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = DarkTextSecondary)
                }
            }

            // Blocker Warning Banner if blocked by row 2
            if (blocker != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF331414)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(YardAmber)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = YardAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Выезд перекрыт машиной во 2-м ряду!",
                                fontWeight = FontWeight.Bold,
                                color = YardAmber,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Авто: ${blocker.carNumber} (до ${blocker.leaveTime ?: "утра"})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (blocker.phone.isNotBlank()) {
                                Button(
                                    onClick = { dialPhone(context, blocker.phone) },
                                    colors = ButtonDefaults.buttonColors(containerColor = YardGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Позвонить", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Button(
                                    onClick = {
                                        sendSms(
                                            context,
                                            blocker.phone,
                                            "Здравствуйте! Вы перекрыли мою машину на месте №${spot.id} во дворе. Пожалуйста, выпустите!"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = YardCyan),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (spot.isFree) {
                // Free spot: Enter car details to occupy
                Text(
                    text = "Занять это парковочное место:",
                    fontSize = 14.sp,
                    color = DarkTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = carInput,
                    onValueChange = { carInput = it.uppercase() },
                    label = { Text("Госномер авто (напр. А123АА)") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = YardGreen) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_car_number"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YardGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = aptInput,
                        onValueChange = { aptInput = it },
                        label = { Text("Квартира") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = DarkTextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_apt_number"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YardGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Телефон") },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = DarkTextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("input_phone_number"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YardGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = leaveTimeInput,
                    onValueChange = { leaveTimeInput = it },
                    label = { Text("Уеду в... (напр. до 19:00 или до утра)") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = YardAmber) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_row1_leave_time"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YardAmber,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (carInput.isNotBlank()) {
                            onPark(carInput, aptInput, phoneInput, leaveTimeInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YardGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_occupy_spot"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Занять парковочное место", fontWeight = FontWeight.Bold, color = Color.Black)
                }

            } else {
                // Occupied spot details
                val occupant = spot.occupiedBy!!

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Car License plate badge
                        LicensePlateBadge(occupant.carNumber)

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Квартира", fontSize = 12.sp, color = DarkTextSecondary)
                                Text(
                                    text = occupant.apt?.ifBlank { "—" } ?: "—",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary
                                )
                            }
                            Column {
                                Text("Телефон", fontSize = 12.sp, color = DarkTextSecondary)
                                Text(
                                    text = occupant.phone?.ifBlank { "Не указан" } ?: "Не указан",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = YardAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Припаркован: ${formatRelativeTime(occupant.parkedAt)}",
                                fontSize = 13.sp,
                                color = YardAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!occupant.leaveTime.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = YardGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Планирует уехать: ${occupant.leaveTime}",
                                    fontSize = 13.sp,
                                    color = YardGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons: Call / SMS / Free
                if (!occupant.phone.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { dialPhone(context, occupant.phone) },
                            colors = ButtonDefaults.buttonColors(containerColor = YardGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Позвонить", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                sendSms(
                                    context,
                                    occupant.phone,
                                    "Здравствуйте! Пишу по поводу парковки на месте №${spot.id} (авто ${occupant.carNumber})."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = YardCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Написать SMS", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = { onOpenLeave(LeaveTarget.SpotTarget(spot)) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = YardRed
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(YardRed)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("button_leave_spot"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = YardRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAdmin) "★ Освободить место (Админ)" else "Освободить место",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LicensePlateBadge(plate: String) {
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = plate.ifBlank { "НЕТ НОМЕРА" },
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(20.dp)
                    .background(Color.Black)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RUS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
        }
    }
}

fun formatRelativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "Не указано"
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val cleanIso = if (iso.contains(".")) iso.substringBefore(".") else iso.replace("Z", "")
        val date = format.parse(cleanIso) ?: Date()
        val localFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val diffMin = (System.currentTimeMillis() - date.time) / 60000
        val timeStr = localFormat.format(date)
        if (diffMin < 60) {
            "$timeStr ($diffMin мин назад)"
        } else {
            val hours = diffMin / 60
            "$timeStr (~$hours ч назад)"
        }
    } catch (e: Exception) {
        iso.take(16).replace("T", " ")
    }
}

fun dialPhone(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    } catch (_: Exception) {}
}

fun sendSms(context: Context, phone: String, message: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
