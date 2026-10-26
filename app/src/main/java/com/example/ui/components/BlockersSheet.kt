package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParkingBlocker
import com.example.ui.LeaveTarget
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardAmber
import com.example.ui.theme.YardAmberContainer
import com.example.ui.theme.YardCyan
import com.example.ui.theme.YardGreen
import com.example.ui.theme.YardRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Row2SelectionBottomBar(
    selectedSpotIds: Set<Int>,
    initialCar: String,
    initialPhone: String,
    onCancel: () -> Unit,
    onSubmit: (car: String, phone: String, time: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var car by remember { mutableStateOf(initialCar) }
    var phone by remember { mutableStateOf(initialPhone) }
    var time by remember { mutableStateOf("08:30 утра") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("row2_selection_bottom_bar"),
        color = DarkSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 16.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, YardAmber)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = YardAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Встать во 2-й ряд (Подпереть)",
                        fontWeight = FontWeight.Bold,
                        color = YardAmber,
                        fontSize = 15.sp
                    )
                }

                IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Отмена", tint = DarkTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selected spots pills
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Перекрыты на карте: ",
                    fontSize = 13.sp,
                    color = DarkTextSecondary
                )
                if (selectedSpotIds.isEmpty()) {
                    Text(
                        text = "Нажмите на машинки выше",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = YardAmber
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        selectedSpotIds.sorted().forEach { id ->
                            Box(
                                modifier = Modifier
                                    .background(YardAmber.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(1.dp, YardAmber, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("№$id", color = YardAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = car,
                    onValueChange = { car = it.uppercase() },
                    label = { Text("Госномер (А123АА)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YardAmber,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YardAmber,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Уеду в... (напр. 08:30 утра)") },
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = YardAmber) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YardAmber,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = DarkTextPrimary,
                    unfocusedTextColor = DarkTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onSubmit(car, phone, time) },
                enabled = selectedSpotIds.isNotEmpty() && car.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = YardAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_submit_row2"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Подтвердить парковку во 2-м ряду",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun BlockersListView(
    blockers: List<ParkingBlocker>,
    onOpenLeave: (LeaveTarget) -> Unit,
    yardAddress: String = "Карбышева 22 / 22А",
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    if (blockers.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = YardGreen,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Второй ряд свободен!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ни одна машина во дворе сейчас не перекрывает выезд другим.",
                    fontSize = 14.sp,
                    color = DarkTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Машины во 2-м ряду (подпёрли выезд): ${blockers.size}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = YardAmber
                )
            }

            items(blockers, key = { it.id }) { blocker ->
                BlockerCard(
                    blocker = blocker,
                    onCall = { dialPhone(context, blocker.phone) },
                    onSms = {
                        sendSms(
                            context,
                            blocker.phone,
                            "Здравствуйте! Вы перекрыли выезд во дворе $yardAddress. Пожалуйста, выпустите мою машину."
                        )
                    },

                    onFree = { onOpenLeave(LeaveTarget.BlockerTarget(blocker)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockerCard(
    blocker: ParkingBlocker,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onFree: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blocker_card_${blocker.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(YardAmber.copy(alpha = 0.5f))),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = YardAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = blocker.carNumber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                if (!blocker.leaveTime.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(YardAmberContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "до ${blocker.leaveTime}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = YardAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Blocked spots
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Подпёр места: ",
                    fontSize = 13.sp,
                    color = DarkTextSecondary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    blocker.blockedSpots.forEach { id ->
                        Box(
                            modifier = Modifier
                                .background(YardRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("№$id", color = YardRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (blocker.phone.isNotBlank()) {
                    Button(
                        onClick = onCall,
                        colors = ButtonDefaults.buttonColors(containerColor = YardGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Набрать", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onSms,
                        colors = ButtonDefaults.buttonColors(containerColor = YardCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SMS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onFree,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Освободить", fontSize = 12.sp)
                }
            }
        }
    }
}
