package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AzkarCategory
import com.example.model.AzkarRepository
import com.example.model.DhikrItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen() {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(AzkarCategory.MORNING) }

    // Map of Dhikr ID to current completed count
    val dhikrCounts = remember { mutableStateMapOf<String, Int>() }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrateTap(isCompleted: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isCompleted) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 150), -1))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    val currentItems = remember(selectedCategory) {
        AzkarRepository.items.filter { it.category == selectedCategory }
    }

    val totalItems = currentItems.size
    val completedItems = currentItems.count { item ->
        (dhikrCounts[item.id] ?: 0) >= item.targetCount
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "الأذكار اليومية",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ألا بذكر الله تطمئن القلوب",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            currentItems.forEach { item ->
                                dhikrCounts[item.id] = 0
                            }
                            vibrateTap(false)
                        },
                        modifier = Modifier.testTag("reset_category_azkar_btn")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "إعادة ضبط أذكار هذا القسم",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AzkarCategory.entries) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat.titleArabic,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("azkar_tab_${cat.id}")
                    )
                }
            }

            // Category Progress Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "التقدم في ${selectedCategory.titleArabic}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$completedItems من $totalItems أذكار مكتملة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Dhikr Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(currentItems, key = { it.id }) { dhikr ->
                    val count = dhikrCounts[dhikr.id] ?: 0
                    val isCompleted = count >= dhikr.targetCount

                    DhikrCard(
                        dhikr = dhikr,
                        currentCount = count,
                        isCompleted = isCompleted,
                        onIncrement = {
                            if (count < dhikr.targetCount) {
                                val newCount = count + 1
                                dhikrCounts[dhikr.id] = newCount
                                vibrateTap(newCount >= dhikr.targetCount)
                            }
                        },
                        onReset = {
                            dhikrCounts[dhikr.id] = 0
                            vibrateTap(false)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun DhikrCard(
    dhikr: DhikrItem,
    currentCount: Int,
    isCompleted: Boolean,
    onIncrement: () -> Unit,
    onReset: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isCompleted) Color(0xFF133226) else MaterialTheme.colorScheme.surface,
        label = "card_color"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 1.dp else 2.dp),
        border = if (isCompleted) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E8B6D))) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onIncrement() }
            .testTag("dhikr_card_${dhikr.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Dhikr Arabic Text
            Text(
                text = dhikr.textArabic,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 28.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Virtue Note
            if (dhikr.virtueArabic.isNotBlank()) {
                Text(
                    text = dhikr.virtueArabic,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted) Color(0xFFF3C766) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Bottom Progress & Counter Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Counter Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCompleted) Color(0xFF0F5A44) else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onIncrement() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFFF3C766),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تم بحمد الله (${dhikr.targetCount})",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$currentCount / ${dhikr.targetCount}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Reset button for individual dhikr
                if (currentCount > 0) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "إعادة العد",
                            tint = if (isCompleted) Color(0xAAFFFFFF) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Progress Line
            if (dhikr.targetCount > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (currentCount.toFloat() / dhikr.targetCount).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isCompleted) Color(0xFFF3C766) else MaterialTheme.colorScheme.primary,
                    trackColor = Color(0x22FFFFFF)
                )
            }
        }
    }
}
