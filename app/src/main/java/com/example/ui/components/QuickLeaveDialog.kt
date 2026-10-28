package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LeaveTarget
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardRed

@Composable
fun QuickLeaveDialog(
    target: LeaveTarget,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onConfirmSpot: (spotId: Int, digits: String) -> Unit,
    onConfirmBlocker: (blockerId: String, digits: String) -> Unit
) {
    var digits by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val title = when (target) {
        is LeaveTarget.SpotTarget -> "Освободить место №${target.spot.id}"
        is LeaveTarget.BlockerTarget -> "Освободить 2-й ряд (${target.blocker.carNumber})"
    }

    val carLabel = when (target) {
        is LeaveTarget.SpotTarget -> target.spot.occupiedBy?.carNumber ?: ""
        is LeaveTarget.BlockerTarget -> target.blocker.carNumber +
            (if (!target.blocker.apt.isNullOrBlank()) ", кв. ${target.blocker.apt}" else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = YardRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkTextPrimary
                )
            }
        },
        text = {
            Column {
                if (carLabel.isNotBlank()) {
                    Text(
                        text = "Автомобиль: $carLabel",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isAdmin) {
                    Text(
                        text = "Вы вошли как Администратор. Подтверждение телефона не требуется.",
                        color = Color(0xFF81C784),
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "Для защиты от случайного снятия введите последние 4 цифры номера телефона, указанного при парковке:",
                        color = DarkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = digits,
                        onValueChange = {
                            if (it.length <= 4) digits = it
                            errorText = null
                        },
                        placeholder = { Text("4 цифры (напр. 4252)", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_leave_digits"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            color = Color.White
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YardRed,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        )
                    )

                    if (errorText != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorText!!,
                            color = YardRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isAdmin && digits.trim().length != 4) {
                        errorText = "Введите ровно 4 цифры телефона!"
                        return@Button
                    }
                    when (target) {
                        is LeaveTarget.SpotTarget -> onConfirmSpot(target.spot.id, digits.trim())
                        is LeaveTarget.BlockerTarget -> onConfirmBlocker(target.blocker.id, digits.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = YardRed),
                modifier = Modifier.testTag("button_confirm_leave")
            ) {
                Text("Подтвердить", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена", color = DarkTextSecondary)
            }
        }
    )
}
