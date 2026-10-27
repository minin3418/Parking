package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParkingLandmark
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardAmber
import com.example.ui.theme.YardCyan
import com.example.ui.theme.YardGreen

@Composable
fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onLogin: (pin: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = YardAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Вход для Администратора", fontWeight = FontWeight.Bold, color = DarkTextPrimary)
            }
        },
        text = {
            Column {
                Text(
                    "Введите PIN-код администратора дворовой территории:",
                    fontSize = 13.sp,
                    color = DarkTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN-код") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_admin_pin"),
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
                onClick = { if (pin.isNotBlank()) onLogin(pin) },
                colors = ButtonDefaults.buttonColors(containerColor = YardAmber),
                modifier = Modifier.testTag("button_login_admin")
            ) {
                Text("Войти", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun EditLandmarkDialog(
    landmark: ParkingLandmark,
    onDismiss: () -> Unit,
    onSave: (newText: String) -> Unit
) {
    var text by remember { mutableStateOf(landmark.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Изменить подпись на карте", fontWeight = FontWeight.Bold, color = DarkTextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст объекта") },
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
                onClick = { if (text.isNotBlank()) onSave(text) },
                colors = ButtonDefaults.buttonColors(containerColor = YardGreen)
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
fun AdminControlBar(
    isEditMode: Boolean,
    onToggleEdit: () -> Unit,
    onSaveMap: () -> Unit,
    onLogout: () -> Unit,
    onSyncGitHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_control_bar"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF261922)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE91E63))),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "★ Панель Администратора",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF4081),
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF80AB)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Выйти", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onToggleEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditMode) Color(0xFF555555) else YardAmber
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isEditMode) "Отмена правки" else "Редактор схемы двора",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (isEditMode) {
                    Button(
                        onClick = onSaveMap,
                        colors = ButtonDefaults.buttonColors(containerColor = YardGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onSyncGitHub,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = YardCyan)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Опубликовать схему", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Схемы, места и опрос хранятся в общей базе и видны всем жителям",
                color = DarkTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
