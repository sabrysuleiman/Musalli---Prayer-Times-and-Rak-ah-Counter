package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuranMetadata
import com.example.model.QuranWardProgress
import com.example.model.SurahInfo
import com.example.viewmodel.QuranViewModel

enum class QuranSubView {
    WARD_DASHBOARD,
    READER,
    SURAHS_INDEX
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranWardScreen(quranViewModel: QuranViewModel) {
    val progress by quranViewModel.progress.collectAsState()
    val pageContent by quranViewModel.currentPageContent.collectAsState()
    val fontSize by quranViewModel.readerFontSize.collectAsState()

    var currentView by remember { mutableStateOf(QuranSubView.WARD_DASHBOARD) }
    var showGoalDialog by remember { mutableStateOf(false) }

    if (showGoalDialog) {
        GoalSettingsDialog(
            currentGoal = progress.dailyGoalPages,
            onSaveGoal = { quranViewModel.setDailyGoal(it) },
            onDismiss = { showGoalDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentView) {
                                QuranSubView.WARD_DASHBOARD -> "الورد القرآني اليومي"
                                QuranSubView.READER -> "قارئ الورد (صفحة ${pageContent.pageNumber})"
                                QuranSubView.SURAHS_INDEX -> "فهرس سور القرآن الكريم"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الجزء ${progress.currentJuz} • سورة ${pageContent.surah.nameArabic}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (currentView == QuranSubView.READER) {
                        IconButton(onClick = { quranViewModel.adjustFontSize(-2f) }) {
                            Text("A-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { quranViewModel.adjustFontSize(2f) }) {
                            Text("A+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { quranViewModel.saveBookmark() },
                            modifier = Modifier.testTag("save_bookmark_icon_btn")
                        ) {
                            Icon(Icons.Filled.Bookmark, contentDescription = "حفظ الفاصلة", tint = Color(0xFFF3C766))
                        }
                    } else {
                        IconButton(
                            onClick = { showGoalDialog = true },
                            modifier = Modifier.testTag("change_ward_goal_btn")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "تعديل الورد", tint = MaterialTheme.colorScheme.primary)
                        }
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
            // Sub-navigation tab selector
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TabPill(
                        title = "الورد والتقدم",
                        icon = Icons.Default.TrendingUp,
                        isSelected = currentView == QuranSubView.WARD_DASHBOARD,
                        onClick = { currentView = QuranSubView.WARD_DASHBOARD },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        title = "المصحف والقراءة",
                        icon = Icons.Default.MenuBook,
                        isSelected = currentView == QuranSubView.READER,
                        onClick = { currentView = QuranSubView.READER },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        title = "فهرس السور",
                        icon = Icons.Default.FormatListNumberedRtl,
                        isSelected = currentView == QuranSubView.SURAHS_INDEX,
                        onClick = { currentView = QuranSubView.SURAHS_INDEX },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (currentView) {
                QuranSubView.WARD_DASHBOARD -> {
                    WardDashboardView(
                        progress = progress,
                        onOpenReader = { currentView = QuranSubView.READER },
                        onOpenBookmark = {
                            quranViewModel.goToBookmark()
                            currentView = QuranSubView.READER
                        },
                        onMarkCompleted = { quranViewModel.markTodayWardCompleted() },
                        onChangeGoal = { showGoalDialog = true }
                    )
                }
                QuranSubView.READER -> {
                    QuranReaderView(
                        pageContent = pageContent,
                        fontSize = fontSize,
                        isBookmarked = progress.bookmarkPage == pageContent.pageNumber,
                        onPreviousPage = { quranViewModel.previousPage() },
                        onNextPage = { quranViewModel.nextPage() },
                        onSaveBookmark = { quranViewModel.saveBookmark() },
                        onMarkTodayCompleted = { quranViewModel.markTodayWardCompleted() }
                    )
                }
                QuranSubView.SURAHS_INDEX -> {
                    SurahsIndexView(
                        onSelectSurah = { surah ->
                            quranViewModel.setPage(surah.startPage)
                            currentView = QuranSubView.READER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.TabPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
            .clickable { onClick() }
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun WardDashboardView(
    progress: QuranWardProgress,
    onOpenReader: () -> Unit,
    onOpenBookmark: () -> Unit,
    onMarkCompleted: () -> Unit,
    onChangeGoal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Khatmah Progress Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().testTag("khatmah_progress_hero_card")
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
                                    text = "تقدم ختم القرآن الكريم",
                                    color = Color(0xFFF3C766),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "الصفحة ${progress.currentPage} من ${progress.totalPages}",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "الجزء ${progress.currentJuz} • سورة ${progress.bookmarkSurah}",
                                color = Color(0xCCFFFFFF),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Big Progress percentage badge
                        Surface(
                            shape = CircleShape,
                            color = Color(0x22F3C766),
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${progress.progressPercent}%",
                                        color = Color(0xFFF3C766),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "مكتمل",
                                        color = Color(0xAAFFFFFF),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFFF3C766),
                        trackColor = Color(0x33FFFFFF)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "متبقي: ${progress.remainingPages} صفحة",
                            color = Color(0xEEFFFFFF),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "متبقي حوالي ${progress.estimatedDaysToKhatmah} يوماً للختمة",
                            color = Color(0xFFF3C766),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Today's Ward Target Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ورد اليوم المستهدف",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${progress.dailyGoalPages} صفحات يومياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    TextButton(onClick = onChangeGoal) {
                        Text("تعديل الخطة", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (progress.isTodayGoalCompleted) "أحسنت! أتممت قراءة ورد اليوم بنجاح ✨" else "قرأت ${progress.pagesReadToday} من ${progress.dailyGoalPages} صفحات اليوم",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (progress.isTodayGoalCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F5A44)
                        ) {
                            Text(
                                text = "مكتمل ✓",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val todayFraction = (progress.pagesReadToday.toFloat() / progress.dailyGoalPages.coerceAtLeast(1)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { todayFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0x330F5A44)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenReader,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("start_reading_ward_btn")
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("قراءة الورد الآن")
                    }

                    if (!progress.isTodayGoalCompleted) {
                        OutlinedButton(
                            onClick = onMarkCompleted,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("mark_ward_done_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تمت القراءة")
                        }
                    }
                }
            }
        }

        // Bookmark and Streak row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bookmark Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenBookmark() }
                    .testTag("bookmark_shortcut_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الفاصلة المحفوظة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "صفحة ${progress.bookmarkPage}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "سورة ${progress.bookmarkSurah}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Streak Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("المواظبة اليومية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${progress.streakDays} أيام",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = "ختمات مكتملة: ${progress.completedKhatmahs}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuranReaderView(
    pageContent: com.example.data.PageContent,
    fontSize: Float,
    isBookmarked: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSaveBookmark: () -> Unit,
    onMarkTodayCompleted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Quran Page Frame
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0x330F5A44))),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("quran_page_frame")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Header with Surah name and Juz
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سورة ${pageContent.surah.nameArabic} (${pageContent.surah.revelationType})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "الجزء ${pageContent.juzNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bismillah Banner (unless Surah 9 At-Tawbah)
                if (pageContent.surah.number != 9) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (fontSize + 2).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Ayahs Content
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pageContent.ayahs.forEach { ayah ->
                        Text(
                            text = "${ayah.text} ﴿${ayah.number}﴾",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.8).sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Page Number bottom indicator
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${pageContent.pageNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reader Bottom Navigation Controls
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next Page (in RTL, next page is to the left)
                FilledTonalButton(
                    onClick = onNextPage,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("reader_next_page_btn")
                ) {
                    Text("التالية")
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }

                // Bookmark Button
                IconButton(
                    onClick = onSaveBookmark,
                    modifier = Modifier.testTag("reader_save_bookmark_btn")
                ) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "حفظ الفاصلة",
                        tint = if (isBookmarked) Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary
                    )
                }

                // Previous Page
                FilledTonalButton(
                    onClick = onPreviousPage,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("reader_prev_page_btn")
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                    Text("السابقة")
                }
            }
        }
    }
}

@Composable
fun SurahsIndexView(
    onSelectSurah: (SurahInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            QuranMetadata.surahs
        } else {
            QuranMetadata.surahs.filter {
                it.nameArabic.contains(searchQuery) || "${it.number}".contains(searchQuery)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث عن سورة بالاسم أو الرقم...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("surah_search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSurah(surah) }
                        .testTag("surah_item_${surah.number}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${surah.number}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "سورة ${surah.nameArabic}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${surah.revelationType} • ${surah.ayahCount} آيات • الجزء ${surah.startJuz}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "صفحة ${surah.startPage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalSettingsDialog(
    currentGoal: Int,
    onSaveGoal: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Pair("صفحتان يومياً (ختمة في 10 أشهر)", 2),
        Pair("٤ صفحات يومياً (ختمة في 5 أشهر)", 4),
        Pair("١٠ صفحات يومياً (ختمة في شهرين)", 10),
        Pair("٢٠ صفحة / جزء يومياً (ختمة شهرية)", 20)
    )

    var selectedGoal by remember { mutableStateOf(currentGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تحديد خطة الورد القرآني", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "اختر كمية القراءة اليومية المناسبة لك:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                options.forEach { (label, pages) ->
                    val isSel = selectedGoal == pages
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedGoal = pages }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = isSel,
                                onClick = { selectedGoal = pages }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveGoal(selectedGoal)
                onDismiss()
            }) {
                Text("حفظ الخطة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
