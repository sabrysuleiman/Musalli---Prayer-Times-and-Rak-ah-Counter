package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.PrayerTiming
import com.example.model.PrayerType

data class DndDurationOption(
    val label: String,
    val durationMinutes: Int,
    val isUntilNext: Boolean = false,
    val isUntilFajr: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndActivationDialog(
    nextPrayer: PrayerTiming?,
    allTimings: List<PrayerTiming>,
    isFajrBypassEnabled: Boolean,
    onActivate: (untilMillis: Long, bypassFajr: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val durationOptions = remember {
        listOf(
            DndDurationOption("٣٠ دقيقة", 30),
            DndDurationOption("ساعة واحدة", 60),
            DndDurationOption("ساعتان", 120),
            DndDurationOption("٤ ساعات", 240),
            DndDurationOption("٨ ساعات (نوم)", 480),
            DndDurationOption("حتى الصلاة القادمة", 0, isUntilNext = true),
            DndDurationOption("حتى صلاة الفجر", 0, isUntilFajr = true)
        )
    }

    var selectedOption by remember { mutableStateOf(durationOptions[1]) } // Default 1 hour
    var bypassFajr by remember { mutableStateOf(isFajrBypassEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dnd_activation_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DoNotDisturbOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "وضع عدم الإزعاج (DND)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "كتم كافة التنبيهات والأصوات خلال فترة محددة للراحة أو الاجتماع، مع استثناء التنبيهات الحرجة (مثل صلاة الفجر) لتتجاوز الكتم وتوقظك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "اختر مدة وضع عدم الإزعاج:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Duration Options Grid / Column
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationOptions.forEach { opt ->
                        val isSelected = selectedOption == opt
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = opt }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedOption = opt },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fajr Bypass Critical Setting
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3C766).copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = Color(0xFF9E7806),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "تجاوز تنبيه صلاة الفجر (مُوصى به)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "يسمح لصوت أذان الفجر بإيقاظك حتى أثناء وضع عدم الإزعاج",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = bypassFajr,
                            onCheckedChange = { bypassFajr = it },
                            modifier = Modifier.testTag("bypass_fajr_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val targetMillis: Long = when {
                            selectedOption.isUntilNext -> {
                                nextPrayer?.timestampMillis ?: (now + 60 * 60 * 1000L)
                            }
                            selectedOption.isUntilFajr -> {
                                val fajrTiming = allTimings.firstOrNull { it.type == PrayerType.FAJR }
                                if (fajrTiming != null && fajrTiming.timestampMillis > now) {
                                    fajrTiming.timestampMillis
                                } else {
                                    (fajrTiming?.timestampMillis ?: now) + 24 * 3600 * 1000L
                                }
                            }
                            else -> {
                                now + (selectedOption.durationMinutes * 60 * 1000L)
                            }
                        }
                        onActivate(targetMillis, bypassFajr)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_activate_dnd_btn")
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تفعيل وضع عدم الإزعاج الآن",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
