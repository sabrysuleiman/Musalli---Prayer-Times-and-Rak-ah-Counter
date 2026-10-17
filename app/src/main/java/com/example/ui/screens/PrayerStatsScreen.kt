package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrayerStatsManager
import com.example.model.*

enum class StatsPeriod {
    WEEKLY,
    MONTHLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerStatsScreen() {
    val context = LocalContext.current
    val statsManager = remember { PrayerStatsManager(context) }

    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEKLY) }
    var weeklyStats by remember { mutableStateOf(statsManager.getWeeklyStats()) }
    var monthlyStats by remember { mutableStateOf(statsManager.getMonthlyStats()) }

    var selectedDayIndex by remember { mutableStateOf(weeklyStats.days.size - 1) } // Default to today (last item)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إحصائيات الصلوات والالتزام",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تتبع المحافظة على الصلاة في وقتها",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            // Period Selector Tabs (Weekly vs Monthly)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Tab(
                        selected = selectedPeriod == StatsPeriod.WEEKLY,
                        onClick = { selectedPeriod = StatsPeriod.WEEKLY },
                        text = {
                            Text(
                                "الإحصائيات الأسبوعية",
                                fontWeight = if (selectedPeriod == StatsPeriod.WEEKLY) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPeriod == StatsPeriod.WEEKLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    Tab(
                        selected = selectedPeriod == StatsPeriod.MONTHLY,
                        onClick = { selectedPeriod = StatsPeriod.MONTHLY },
                        text = {
                            Text(
                                "الإحصائيات الشهرية",
                                fontWeight = if (selectedPeriod == StatsPeriod.MONTHLY) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPeriod == StatsPeriod.MONTHLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedPeriod) {
                    StatsPeriod.WEEKLY -> {
                        WeeklyStatsView(
                            weeklyStats = weeklyStats,
                            selectedDayIndex = selectedDayIndex,
                            onSelectDay = { selectedDayIndex = it },
                            onTogglePrayer = { dayLog, pType ->
                                statsManager.togglePrayerStatus(dayLog.dateKey, pType)
                                weeklyStats = statsManager.getWeeklyStats()
                                monthlyStats = statsManager.getMonthlyStats()
                            }
                        )
                    }
                    StatsPeriod.MONTHLY -> {
                        MonthlyStatsView(monthlyStats = monthlyStats)
                    }
                }

                // Motivational Quran/Hadith Banner
                MotivationalCard()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun WeeklyStatsView(
    weeklyStats: WeeklyStats,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit,
    onTogglePrayer: (DayPrayerLog, PrayerType) -> Unit
) {
    // Hero Summary Card
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().testTag("weekly_summary_card")
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F5A44), Color(0xFF093326))
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x33FFFFFF)
                    ) {
                        Text(
                            text = "معدل الالتزام الأسبوعي",
                            color = Color(0xFFF3C766),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${weeklyStats.weeklyCommitmentPercent}% في وقتها",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${weeklyStats.totalOnTimePrayers} من أصل 35 صلاة في وقتها",
                        color = Color(0xCCFFFFFF),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Streak flame badge
                Surface(
                    shape = CircleShape,
                    color = Color(0x22E65100),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "${weeklyStats.streakDays} أيام",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Weekly Interactive Bar Chart
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("weekly_bar_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أداء الصلوات خلال آخر 7 أيام:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الهدف: ٥ صلوات يومياً",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Bar Chart
            WeeklyBarChartCanvas(
                days = weeklyStats.days,
                selectedDayIndex = selectedDayIndex,
                onSelectDay = onSelectDay
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "💡 اضغط على أي يوم في الرسم البياني لعرض صلواته وتعديلها",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Selected Day Detail Grid
    val selectedDay = weeklyStats.days.getOrNull(selectedDayIndex) ?: weeklyStats.days.last()
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("day_detail_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "صلوات يوم ${selectedDay.dayNameArabic}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedDay.dateKey,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedDay.isFullDayCompleted) Color(0xFF0F5A44) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${selectedDay.totalOnTime} من 5 صلوات",
                        color = if (selectedDay.isFullDayCompleted) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 Prayers Status Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrayerStatusRow("صلاة الفجر", selectedDay.fajr) {
                    onTogglePrayer(selectedDay, PrayerType.FAJR)
                }
                PrayerStatusRow("صلاة الظهر", selectedDay.dhuhr) {
                    onTogglePrayer(selectedDay, PrayerType.DHUHR)
                }
                PrayerStatusRow("صلاة العصر", selectedDay.asr) {
                    onTogglePrayer(selectedDay, PrayerType.ASR)
                }
                PrayerStatusRow("صلاة المغرب", selectedDay.maghrib) {
                    onTogglePrayer(selectedDay, PrayerType.MAGHRIB)
                }
                PrayerStatusRow("صلاة العشاء", selectedDay.isha) {
                    onTogglePrayer(selectedDay, PrayerType.ISHA)
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChartCanvas(
    days: List<DayPrayerLog>,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val barHeightProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        barHeightProgress.animateTo(1f, animationSpec = tween(700))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val totalWidth = maxWidth
        val barCount = days.size.coerceAtLeast(1)
        val barWidthDp = (totalWidth - 32.dp) / barCount

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = selectedDayIndex == index
                val onTimeCount = day.totalOnTime
                val barFraction = (onTimeCount / 5f) * barHeightProgress.value

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .width(barWidthDp)
                        .fillMaxHeight()
                        .clickable { onSelectDay(index) }
                        .padding(horizontal = 2.dp)
                ) {
                    // Count on top of bar
                    Text(
                        text = "$onTimeCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .height(100.dp * barFraction.coerceAtLeast(0.08f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                when {
                                    isSelected -> Color(0xFFD4AF37)
                                    onTimeCount == 5 -> Color(0xFF0F5A44)
                                    onTimeCount >= 3 -> Color(0xFF2E8B6D)
                                    else -> Color(0xFFE57373)
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Day Name
                    Text(
                        text = day.dayNameArabic.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerStatusRow(
    prayerTitle: String,
    status: PrayerLogStatus,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = prayerTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (status) {
                    PrayerLogStatus.ON_TIME -> Color(0xFF0F5A44)
                    PrayerLogStatus.LATE -> Color(0xFFFFA000)
                    PrayerLogStatus.MISSED -> Color(0xFFD32F2F)
                }
            ) {
                Text(
                    text = when (status) {
                        PrayerLogStatus.ON_TIME -> "أديت في وقتها ✓"
                        PrayerLogStatus.LATE -> "متأخرة / قضاء"
                        PrayerLogStatus.MISSED -> "لم تسجل ✕"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MonthlyStatsView(monthlyStats: MonthlyStats) {
    // Monthly Donut / Radial Hero Card
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().testTag("monthly_radial_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "تقرير ${monthlyStats.monthNameArabic}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Donut Chart
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 16.dp.toPx()
                    val radius = size.minDimension / 2 - strokeW / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Track circle
                    drawCircle(
                        color = Color(0x220F5A44),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeW)
                    )

                    // Filled arc
                    val sweep = 360f * (monthlyStats.commitmentPercent / 100f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF0F5A44), Color(0xFFF3C766), Color(0xFF0F5A44))
                        ),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${monthlyStats.commitmentPercent}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "نسبة الالتزام",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "أديت ${monthlyStats.onTimePrayers} صلاة في وقتها من أصل ${monthlyStats.totalPrayers} صلاة خلال الشهر",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    // Weekly Trends over the Month
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("monthly_trends_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "تطور الالتزام عبر أسابيع الشهر:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            monthlyStats.weeklyBreakdown.forEach { (weekTitle, percent) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = weekTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "$percent%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (percent >= 90) Color(0xFF0F5A44) else Color(0xFFF3C766),
                        trackColor = Color(0x33000000)
                    )
                }
            }
        }
    }

    // Per-Prayer Adherence Rates Card
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "معدل المحافظة على كل فريضة في وقتها:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            PrayerRateRow("صلاة الفجر 🌅 (تنبيه خاص)", monthlyStats.fajrRate, isFajr = true)
            PrayerRateRow("صلاة الظهر ☀️", monthlyStats.dhuhrRate)
            PrayerRateRow("صلاة العصر ⛅", monthlyStats.asrRate)
            PrayerRateRow("صلاة المغرب 🌇", monthlyStats.maghribRate)
            PrayerRateRow("صلاة العشاء 🌙", monthlyStats.ishaRate)
        }
    }
}

@Composable
fun PrayerRateRow(name: String, rate: Int, isFajr: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isFajr) FontWeight.Bold else FontWeight.Normal,
            color = if (isFajr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (rate >= 90) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "$rate% التزام",
                color = if (rate >= 90) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun MotivationalCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3C766).copy(alpha = 0.16f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFB8860B),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "«أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ: الصَّلَاةُ عَلَى وَقْتِهَا»",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "المواظبة على أداء الصلاة في أول وقتها بركة في العمر والرزق ونور لصاحبها يوم القيامة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
