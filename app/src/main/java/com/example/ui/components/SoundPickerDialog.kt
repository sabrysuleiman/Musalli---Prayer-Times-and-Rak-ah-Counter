package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PrayerSoundItem
import com.example.data.PrayerSoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerDialog(
    prayerTitle: String,
    currentSoundId: String,
    playingPreviewSoundId: String?,
    onPlayPreview: (String) -> Unit,
    onStopPreview: () -> Unit,
    onSaveSound: (selectedSoundId: String, applyToAll: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSoundId by remember { mutableStateOf(currentSoundId) }
    var applyToAllPrayers by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            onStopPreview()
        }
    }

    AlertDialog(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        modifier = Modifier.testTag("sound_picker_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "نغمة وتنبيه: $prayerTitle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اختر الصوت واستمع إليه بالضغط على زر التشغيل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        onStopPreview()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sound List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PrayerSoundManager.availableSounds) { sound ->
                        val isSelected = selectedSoundId == sound.id
                        val isPlaying = playingPreviewSoundId == sound.id

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSoundId = sound.id
                                }
                                .testTag("sound_card_${sound.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play / Stop Preview Button
                                if (sound.id != "silent_vibrate") {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                onStopPreview()
                                            } else {
                                                onPlayPreview(sound.id)
                                            }
                                        },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            shadowElevation = 2.dp,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = if (isPlaying) "إيقاف" else "استماع",
                                                    tint = if (isPlaying) Color.White else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Vibration,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = sound.nameArabic,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (sound.isCriticalRecommended) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF3C766).copy(alpha = 0.3f)
                                            ) {
                                                Text(
                                                    text = "مميز",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF8A6703),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = sound.descArabic,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedSoundId = sound.id },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Apply to all prayers toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyToAllPrayers = !applyToAllPrayers }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyToAllPrayers,
                        onCheckedChange = { applyToAllPrayers = it },
                        modifier = Modifier.testTag("apply_to_all_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تطبيق هذا الصوت على جميع الصلوات الخمس",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Button(
                    onClick = {
                        onStopPreview()
                        onSaveSound(selectedSoundId, applyToAllPrayers)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_sound_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ التخصيص", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
