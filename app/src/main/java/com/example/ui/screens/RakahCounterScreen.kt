package com.example.ui.screens

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrayerStatsManager
import com.example.model.PrayerLogStatus
import com.example.model.PrayerType
import com.example.model.RakahSessionState
import com.example.viewmodel.RakahViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RakahCounterScreen(
    rakahViewModel: RakahViewModel,
    initialPrayer: PrayerType? = null,
    onBack: () -> Unit = {}
) {
    BackHandler { onBack() }
    val state by rakahViewModel.state.collectAsState()
    val context = LocalContext.current

    var selectedPrayer by remember { mutableStateOf(initialPrayer ?: PrayerType.DHUHR) }
    var targetCount by remember { mutableStateOf(selectedPrayer.defaultRakahs.coerceAtLeast(1)) }
    var currentCount by remember { mutableStateOf(1) }
    var countHistory by remember { mutableStateOf(listOf<Int>()) }
    var showPrayerPicker by remember { mutableStateOf(false) }
    val statsManager = remember { PrayerStatsManager(context) }
    var isSavedToStats by remember { mutableStateOf(false) }

    // Haptic feedback
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrateOnce() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(60)
            }
        } catch (_: Exception) {}
    }

    // MANDATORY REQUIREMENT: Screen stays awake without sleep/idle timeout
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun onScreenTapped() {
        vibrateOnce()
        countHistory = countHistory + currentCount
        currentCount += 1
    }

    fun onUndo() {
        if (countHistory.isNotEmpty()) {
            currentCount = countHistory.last()
            countHistory = countHistory.dropLast(1)
            vibrateOnce()
        }
    }

    fun onReset() {
        countHistory = emptyList()
        currentCount = 1
        isSavedToStats = false
        vibrateOnce()
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF091C15),
                        Color(0xFF05120D)
                    )
                )
            )
            // TOUCH WORKING ANYWHERE IN SCREEN
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onScreenTapped()
            }
            .testTag("full_screen_touch_counter")
    ) {
        // TOP DISCREET BAR
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("counter_back_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // Selected Prayer / Mode Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x22F3C766),
                    modifier = Modifier.clickable { showPrayerPicker = !showPrayerPicker }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFFF3C766),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedPrayer.arabicName} ($targetCount ركعات)",
                            color = Color(0xFFF3C766),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFFF3C766),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Screen Active Always Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x220F5A44)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF5ED3A1),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "الشاشة نشطة",
                        color = Color(0xFF5ED3A1),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Dropdown prayer picker row if opened
        AnimatedVisibility(
            visible = showPrayerPicker,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF132A21),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "اختر الصلاة لضبط عدد ركعاتها:",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val prayers = listOf(
                        PrayerType.FAJR,
                        PrayerType.DHUHR,
                        PrayerType.ASR,
                        PrayerType.MAGHRIB,
                        PrayerType.ISHA,
                        PrayerType.JUMMAH,
                        PrayerType.WITR,
                        PrayerType.QIYAM,
                        PrayerType.CUSTOM
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(prayers) { p ->
                            val isSel = selectedPrayer == p
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Color(0xFF0F5A44) else Color(0x33FFFFFF),
                                modifier = Modifier.clickable {
                                    selectedPrayer = p
                                    targetCount = if (p == PrayerType.CUSTOM) 2 else p.defaultRakahs
                                    currentCount = 1
                                    countHistory = emptyList()
                                    showPrayerPicker = false
                                }
                            ) {
                                Text(
                                    text = p.arabicName,
                                    color = if (isSel) Color(0xFFF3C766) else Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // EMPTY SCREEN WITH LARGE NUMBER IN CENTER
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Label above large number
            Text(
                text = "الركعة",
                color = Color(0x99FFFFFF),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )

            // GIGANTIC NUMBER IN THE EXACT CENTER
            Text(
                text = "$currentCount",
                color = Color(0xFFF3C766),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 150.sp,
                    lineHeight = 150.sp
                ),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("center_large_counter_number")
            )

            // Target Indicator (e.g. من 4)
            if (targetCount > 0) {
                Text(
                    text = "من $targetCount",
                    color = Color(0x77FFFFFF),
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 22.sp
                )
            }

            // Tashahhud hint if in 2nd rakah of 4 or final
            if (currentCount == 2 && targetCount > 2) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33D4AF37)
                ) {
                    Text(
                        text = "الجلوس للتشهد الأول",
                        color = Color(0xFFF3C766),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            } else if (currentCount >= targetCount && targetCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F5A44)
                ) {
                    Text(
                        text = "التشهد الأخير والتسليم • تقبل الله",
                        color = Color(0xFF5ED3A1),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSavedToStats) Color(0x335ED3A1) else Color(0xFFD4AF37),
                    modifier = Modifier
                        .clickable {
                            if (!isSavedToStats) {
                                statsManager.setPrayerStatus(
                                    statsManager.getTodayKey(),
                                    selectedPrayer,
                                    PrayerLogStatus.ON_TIME
                                )
                                isSavedToStats = true
                                vibrateOnce()
                            }
                        }
                        .testTag("log_prayer_to_stats_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSavedToStats) Icons.Default.CheckCircle else Icons.Default.BarChart,
                            contentDescription = null,
                            tint = if (isSavedToStats) Color(0xFF5ED3A1) else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSavedToStats) "تم تسجيل ${selectedPrayer.arabicName} في الإحصائيات ✓" else "تسجيل ${selectedPrayer.arabicName} في الإحصائيات",
                            color = if (isSavedToStats) Color(0xFF5ED3A1) else Color.Black,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Gentle hint
            Text(
                text = "المس أي مكان في الشاشة لزيادة الركعة",
                color = Color(0x55FFFFFF),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // ==========================================
        // SUBTLE BOTTOM OVERLAY CONTROLS (Undo, Reset)
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo button
            OutlinedButton(
                onClick = { onUndo() },
                enabled = countHistory.isNotEmpty(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color(0x33FFFFFF)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("counter_undo_btn")
            ) {
                Icon(Icons.Default.Undo, contentDescription = "تراجع", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تراجع", style = MaterialTheme.typography.bodyMedium)
            }

            // Reset button
            OutlinedButton(
                onClick = { onReset() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF3C766)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("counter_reset_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "تصفير", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تصفير", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
