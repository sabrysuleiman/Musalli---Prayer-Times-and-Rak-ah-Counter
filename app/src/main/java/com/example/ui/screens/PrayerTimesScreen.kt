package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OfflinePrayerCalculator
import com.example.data.PrayerSoundManager
import com.example.data.PrayerStatsManager
import com.example.model.PrayerLogStatus
import com.example.model.PrayerTiming
import com.example.model.PrayerType
import com.example.ui.components.CityPickerDialog
import com.example.ui.components.DndActivationDialog
import com.example.ui.components.SoundPickerDialog
import com.example.viewmodel.NextPrayerCountdown
import com.example.viewmodel.PrayerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    prayerViewModel: PrayerViewModel,
    onNavigateToCounter: (PrayerType) -> Unit,
    onNavigateToStats: () -> Unit = {},
    onRequestLocationPermission: () -> Unit
) {
    val schedule by prayerViewModel.schedule.collectAsState()
    val timingsList by prayerViewModel.timingsList.collectAsState()
    val nextPrayer by prayerViewModel.nextPrayer.collectAsState()
    val countdown by prayerViewModel.countdown.collectAsState()
    val dndStatus by prayerViewModel.dndStatus.collectAsState()
    val previewSoundId by prayerViewModel.previewSoundId.collectAsState()

    val context = LocalContext.current
    val statsManager = remember { PrayerStatsManager(context) }

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

    fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    // MANDATORY REQUIREMENT: Default always opens to today!
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val todayCalendar = remember { Calendar.getInstance() }

    val dateFormatKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val selectedDateKey = remember(selectedCalendar) { dateFormatKey.format(selectedCalendar.time) }
    val todayKey = remember { dateFormatKey.format(todayCalendar.time) }
    val isToday = selectedDateKey == todayKey

    val dayNameArabicFormat = remember { SimpleDateFormat("EEEE", Locale("ar")) }
    val dayNameArabic = remember(selectedCalendar) { dayNameArabicFormat.format(selectedCalendar.time) }

    val fullDateArabicFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale("ar")) }
    val fullDateArabic = remember(selectedCalendar) { fullDateArabicFormat.format(selectedCalendar.time) }

    // Day prayer logs state for selected day
    var dayLog by remember(selectedDateKey) {
        mutableStateOf(statsManager.getDayLog(selectedDateKey, selectedCalendar.time))
    }

    // Computed timings for the selected day
    val displayTimings = remember(selectedCalendar, isToday, timingsList) {
        if (isToday) {
            timingsList
        } else {
            val offlineMethod = when (prayerViewModel.preferences.calculationMethodIndex) {
                0 -> OfflinePrayerCalculator.CalculationMethod.UMM_AL_QURA
                1 -> OfflinePrayerCalculator.CalculationMethod.EGYPTIAN
                2 -> OfflinePrayerCalculator.CalculationMethod.MWL
                3 -> OfflinePrayerCalculator.CalculationMethod.ISNA
                else -> OfflinePrayerCalculator.CalculationMethod.KARACHI
            }
            val pastSchedule = OfflinePrayerCalculator.calculate(
                latitude = prayerViewModel.preferences.selectedLatitude,
                longitude = prayerViewModel.preferences.selectedLongitude,
                calendar = selectedCalendar,
                method = offlineMethod,
                cityName = prayerViewModel.preferences.selectedCityName
            )
            listOf(
                PrayerTiming(PrayerType.FAJR, pastSchedule.fajr, 0L, isPassed = true),
                PrayerTiming(PrayerType.SUNRISE, pastSchedule.sunrise, 0L, isPassed = true),
                PrayerTiming(PrayerType.DHUHR, pastSchedule.dhuhr, 0L, isPassed = true),
                PrayerTiming(PrayerType.ASR, pastSchedule.asr, 0L, isPassed = true),
                PrayerTiming(PrayerType.MAGHRIB, pastSchedule.maghrib, 0L, isPassed = true),
                PrayerTiming(PrayerType.ISHA, pastSchedule.isha, 0L, isPassed = true)
            )
        }
    }

    var showCityPicker by remember { mutableStateOf(false) }
    var showDndDialog by remember { mutableStateOf(false) }
    var selectedPrayerForSound by remember { mutableStateOf<PrayerTiming?>(null) }

    if (showCityPicker) {
        CityPickerDialog(
            currentCityName = schedule?.locationName ?: "مكة المكرمة",
            onCitySelected = { city ->
                prayerViewModel.selectCity(city)
            },
            onUseGps = {
                prayerViewModel.repository.preferences.isGpsEnabled = true
                onRequestLocationPermission()
            },
            onDismiss = { showCityPicker = false }
        )
    }

    if (showDndDialog) {
        DndActivationDialog(
            nextPrayer = nextPrayer,
            allTimings = timingsList,
            isFajrBypassEnabled = dndStatus.bypassFajr,
            onActivate = { untilMillis, bypassFajr ->
                prayerViewModel.preferences.dndBypassFajr = bypassFajr
                prayerViewModel.activateDndUntil(untilMillis)
            },
            onDismiss = { showDndDialog = false }
        )
    }

    selectedPrayerForSound?.let { timing ->
        SoundPickerDialog(
            prayerTitle = timing.type.arabicName,
            currentSoundId = prayerViewModel.getPrayerSound(timing.type.id),
            playingPreviewSoundId = previewSoundId,
            onPlayPreview = { soundId -> prayerViewModel.playSoundPreview(soundId) },
            onStopPreview = { prayerViewModel.stopSoundPreview() },
            onSaveSound = { soundId, applyToAll ->
                if (applyToAll) {
                    prayerViewModel.setAllPrayersSound(soundId)
                } else {
                    prayerViewModel.setPrayerSound(timing.type.id, soundId)
                }
            },
            onDismiss = { selectedPrayerForSound = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مواقيت الصلاة",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        schedule?.let {
                            Text(
                                text = "${it.hijriDate} • ${it.locationName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // DND Quick Action Button
                    IconButton(
                        onClick = {
                            if (dndStatus.isActive) {
                                prayerViewModel.deactivateDnd()
                            } else {
                                showDndDialog = true
                            }
                        },
                        modifier = Modifier.testTag("dnd_top_action_btn")
                    ) {
                        Icon(
                            if (dndStatus.isActive) Icons.Default.DoNotDisturbOn else Icons.Default.DoNotDisturbOff,
                            contentDescription = "وضع عدم الإزعاج",
                            tint = if (dndStatus.isActive) Color(0xFFF3C766) else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showCityPicker = true },
                        modifier = Modifier.testTag("change_location_button")
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "تغيير الموقع", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { onNavigateToStats() },
                        modifier = Modifier.testTag("stats_top_action_btn")
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = "الإحصائيات والالتزام", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { onRequestLocationPermission() },
                        modifier = Modifier.testTag("refresh_timings_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // DND Active Banner if active, or DND trigger card
                if (dndStatus.isActive) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E2F28)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_dnd_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF3C766),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.DoNotDisturbOn,
                                            contentDescription = null,
                                            tint = Color(0xFF1E2F28),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "وضع عدم الإزعاج نشط (${dndStatus.remainingFormatted})",
                                        color = Color(0xFFF3C766),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (dndStatus.bypassFajr) "مكتوم • يُستثنى أذان الفجر (تنبيه حرج)" else "كافة التنبيهات مكتومة مؤقتاً",
                                        color = Color(0xCCFFFFFF),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { prayerViewModel.deactivateDnd() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("cancel_dnd_btn")
                            ) {
                                Text("إلغاء", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    // Small quick toggle for DND
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDndDialog = true }
                            .testTag("activate_dnd_quick_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DoNotDisturbOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تفعيل وضع عدم الإزعاج (DND)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "مخصص بوقت",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 1. Interactive Day Navigation & Prayer Tracker Bar
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day_navigator_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Day Button (الرجوع للأيام السابقة)
                            IconButton(
                                onClick = {
                                    val newCal = (selectedCalendar.clone() as Calendar).apply {
                                        add(Calendar.DAY_OF_YEAR, -1)
                                    }
                                    selectedCalendar = newCal
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("prev_day_btn")
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "اليوم السابق",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Center Date Info
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isToday) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "اليوم",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "$dayNameArabic، $fullDateArabic",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (!isToday) {
                                    TextButton(
                                        onClick = {
                                            selectedCalendar = Calendar.getInstance()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(26.dp)
                                            .testTag("return_to_today_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.Today,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "العودة إلى اليوم",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Next Day Button (Up to today)
                            val canGoNext = selectedCalendar.before(todayCalendar)
                            IconButton(
                                onClick = {
                                    if (canGoNext) {
                                        val newCal = (selectedCalendar.clone() as Calendar).apply {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                        }
                                        selectedCalendar = newCal
                                    }
                                },
                                enabled = canGoNext,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("next_day_btn")
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "اليوم التالي",
                                    tint = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }

                        // Summary of completed prayers for the selected day
                        val completedCount = dayLog.totalOnTime
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "صلوات هذا اليوم: أُديت $completedCount من 5",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (completedCount == 5) Color(0xFF0F5A44) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (completedCount == 5) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0F5A44)
                                ) {
                                    Text(
                                        text = "مكتملة ✓",
                                        color = Color(0xFFF3C766),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Next Prayer Countdown Card (shown when viewing today)
            if (isToday) {
                item {
                    NextPrayerCard(
                        countdown = countdown,
                        nextPrayer = nextPrayer,
                        onStartCounterForNext = {
                            nextPrayer?.type?.let { onNavigateToCounter(it) }
                        }
                    )
                }
            }

            // Header for daily prayers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صلوات $dayNameArabic وتتبع الأداء",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fullDateArabic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timings Cards for the selected day
            items(displayTimings) { timing ->
                val soundId = prayerViewModel.getPrayerSound(timing.type.id)
                val soundItem = PrayerSoundManager.getSoundItem(soundId)

                val isCompleted = when (timing.type) {
                    PrayerType.FAJR -> dayLog.fajr == PrayerLogStatus.ON_TIME || dayLog.fajr == PrayerLogStatus.LATE
                    PrayerType.DHUHR -> dayLog.dhuhr == PrayerLogStatus.ON_TIME || dayLog.dhuhr == PrayerLogStatus.LATE
                    PrayerType.ASR -> dayLog.asr == PrayerLogStatus.ON_TIME || dayLog.asr == PrayerLogStatus.LATE
                    PrayerType.MAGHRIB -> dayLog.maghrib == PrayerLogStatus.ON_TIME || dayLog.maghrib == PrayerLogStatus.LATE
                    PrayerType.ISHA -> dayLog.isha == PrayerLogStatus.ON_TIME || dayLog.isha == PrayerLogStatus.LATE
                    else -> false
                }

                // "بدلا من ذلك اجعلها فقط على الصلاة التي أتى وقتها زر ابدأ العداد"
                val showCounterButton = isToday && (timing.isCurrent || (timing.isPassed && !isCompleted)) && timing.type != PrayerType.SUNRISE

                PrayerItemCard(
                    timing = timing,
                    soundItemTitle = soundItem.nameArabic,
                    isCompleted = isCompleted,
                    showCounterButton = showCounterButton,
                    onToggleCompleted = {
                        vibrateShort()
                        dayLog = statsManager.togglePrayerCompletion(selectedDateKey, timing.type)
                    },
                    onToggleNotification = { enabled ->
                        prayerViewModel.toggleNotification(timing.type.id, enabled)
                    },
                    onOpenSoundPicker = {
                        if (timing.type != PrayerType.SUNRISE) {
                            selectedPrayerForSound = timing
                        }
                    },
                    onOpenCounter = {
                        if (timing.type != PrayerType.SUNRISE) {
                            onNavigateToCounter(timing.type)
                        }
                    }
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToStats() }
                        .testTag("home_prayer_stats_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F5A44),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = Color(0xFFF3C766),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "إحصائيات الصلاة والالتزام",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "شاهد تقارير التزامك الأسبوعية والشهرية بالرسوم البيانية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun NextPrayerCard(
    countdown: NextPrayerCountdown,
    nextPrayer: PrayerTiming?,
    onStartCounterForNext: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().testTag("next_prayer_card")
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F5A44),
                            Color(0xFF093326)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33FFFFFF)
                        ) {
                            Text(
                                text = "الصلاة القادمة",
                                color = Color(0xFFF3C766),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = countdown.prayerTitleArabic.ifBlank { "..." },
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = countdown.timeFormatted.ifBlank { "--:--" },
                            color = Color(0xFFF3C766),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        nextPrayer?.type?.defaultRakahs?.let { rakahs ->
                            if (rakahs > 0) {
                                Text(
                                    text = "$rakahs ركعات فريضة",
                                    color = Color(0xCCFFFFFF),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Countdown Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "متبقي على الأذان:",
                        color = Color(0xEEFFFFFF),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val timeFormatted = String.format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d",
                        countdown.hoursRemaining,
                        countdown.minutesRemaining,
                        countdown.secondsRemaining
                    )
                    Text(
                        text = timeFormatted,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { countdown.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFF3C766),
                    trackColor = Color(0x33FFFFFF),
                )
            }
        }
    }
}

@Composable
fun PrayerItemCard(
    timing: PrayerTiming,
    soundItemTitle: String,
    isCompleted: Boolean,
    showCounterButton: Boolean,
    onToggleCompleted: () -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onOpenSoundPicker: () -> Unit,
    onOpenCounter: () -> Unit
) {
    val isCurrent = timing.isCurrent
    val isNext = timing.isNext
    val isPassed = timing.isPassed

    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        isCompleted -> Color(0xFF0F5A44).copy(alpha = 0.08f)
        isNext -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    var notificationActive by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_card_${timing.type.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    // Status icon / prayer icon
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isCompleted -> Color(0xFF0F5A44)
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isNext -> Color(0xFFF3C766)
                            isPassed -> MaterialTheme.colorScheme.outlineVariant
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val icon = when (timing.type) {
                                PrayerType.FAJR -> Icons.Default.WbTwilight
                                PrayerType.SUNRISE -> Icons.Default.WbSunny
                                PrayerType.DHUHR -> Icons.Default.Brightness7
                                PrayerType.ASR -> Icons.Default.Brightness5
                                PrayerType.MAGHRIB -> Icons.Default.NightsStay
                                PrayerType.ISHA -> Icons.Default.Bedtime
                                else -> Icons.Default.AccessTime
                            }
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isCompleted || isCurrent || isNext) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = timing.type.arabicName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "أتى وقتها",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (isCompleted && timing.type != PrayerType.SUNRISE) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0F5A44).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "✓ تم الأداء",
                                        color = Color(0xFF0F5A44),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (timing.type != PrayerType.SUNRISE) {
                            // Sound Title Pill
                            Text(
                                text = soundItemTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onOpenSoundPicker() }
                            )
                        } else {
                            Text(
                                text = "شروق الشمس",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timing.timeFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    if (timing.type != PrayerType.SUNRISE) {
                        Spacer(modifier = Modifier.width(6.dp))

                        // Sound Picker Shortcut Button
                        IconButton(
                            onClick = onOpenSoundPicker,
                            modifier = Modifier.size(36.dp).testTag("sound_picker_btn_${timing.type.id}")
                        ) {
                            Icon(
                                Icons.Outlined.MusicNote,
                                contentDescription = "تغيير الصوت",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Notification toggle button
                        IconButton(
                            onClick = {
                                notificationActive = !notificationActive
                                onToggleNotification(notificationActive)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (notificationActive) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                                contentDescription = "التنبيه",
                                tint = if (notificationActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ACTION BUTTONS: "تمت الصلاة" and "ابدأ العداد" (Only for non-sunrise)
            if (timing.type != PrayerType.SUNRISE) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "تمت الصلاة" Tracking Button
                    if (isCompleted) {
                        Button(
                            onClick = onToggleCompleted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F5A44),
                                contentColor = Color(0xFFF3C766)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("prayer_completed_btn_${timing.type.id}")
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تمت الصلاة ✓",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onToggleCompleted,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("prayer_completed_btn_${timing.type.id}")
                        ) {
                            Icon(
                                Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تمت الصلاة",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // "ابدأ العداد" Button: ONLY displayed for the prayer whose time has arrived!
                    if (showCounterButton) {
                        Button(
                            onClick = onOpenCounter,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF3C766),
                                contentColor = Color(0xFF1E2F28)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("start_counter_btn_${timing.type.id}")
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF1E2F28)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ابدأ العداد",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E2F28)
                            )
                        }
                    }
                }
            }
        }
    }
}
