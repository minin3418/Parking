package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.data.model.SurveyItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.YardCyan
import com.example.ui.theme.YardGreen

@Composable
fun SurveyView(
    surveys: List<SurveyItem>?,
    hasVoted: Boolean,
    onSubmitSurvey: (answer: String, comment: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf("Буду пользоваться") }
    var comment by remember { mutableStateOf("") }

    val options = listOf(
        "👍 Буду пользоваться" to "Буду пользоваться",
        "🤝 Если все будут, то да" to "Если все будут, то да",
        "👎 Не нужно / Сложно" to "Не нужно / Сложно"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(YardCyan.copy(alpha = 0.5f))),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = YardCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Опрос жильцов двора",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Помогите решить, внедряем ли мы общую систему контроля мест и второго ряда на постоянной основе:",
                        fontSize = 13.sp,
                        color = DarkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (hasVoted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(YardGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, YardGreen, RoundedCornerShape(8.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = YardGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Спасибо! Ваш голос уже учтён.",
                                    fontWeight = FontWeight.Bold,
                                    color = YardGreen
                                )
                            }
                        }
                    } else {
                        options.forEach { (label, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOption = value }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedOption == value,
                                    onClick = { selectedOption = value },
                                    colors = RadioButtonDefaults.colors(selectedColor = YardCyan)
                                )
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Пожелание или предложение (необязательно)") },
                            leadingIcon = { Icon(Icons.Default.Feedback, contentDescription = null, tint = DarkTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YardCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = DarkTextPrimary,
                                unfocusedTextColor = DarkTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onSubmitSurvey(selectedOption, comment.trim()) },
                            colors = ButtonDefaults.buttonColors(containerColor = YardCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("button_submit_survey"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Отправить голос", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Community responses
        val list = surveys ?: emptyList()
        item {
            Text(
                text = "Ответы соседей (${list.size}):",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (list.isEmpty()) {
            item {
                Text(
                    text = "Пока ответов нет. Будьте первыми!",
                    fontSize = 14.sp,
                    color = DarkTextSecondary
                )
            }
        } else {
            items(list) { survey ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = survey.answer,
                                fontWeight = FontWeight.Bold,
                                color = YardCyan,
                                fontSize = 14.sp
                            )
                            if (!survey.date.isNullOrBlank()) {
                                Text(
                                    text = survey.date,
                                    color = DarkTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (!survey.comment.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💬 \"${survey.comment}\"",
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color(0xFFE0E0E0),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
