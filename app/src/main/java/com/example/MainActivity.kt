package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.PrayerType
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PrayerViewModel
import com.example.viewmodel.QuranViewModel
import com.example.viewmodel.RakahViewModel

enum class MainTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PRAYER_TIMES("المواقيت", Icons.Default.AccessTime),
    QURAN_WARD("الورد القرآني", Icons.Default.MenuBook),
    AZKAR("الأذكار", Icons.Default.AutoAwesome),
    MORE("المزيد", Icons.Default.MoreHoriz)
}

class MainActivity : ComponentActivity() {

    private val prayerViewModel: PrayerViewModel by viewModels()
    private val rakahViewModel: RakahViewModel by viewModels()
    private val quranViewModel: QuranViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContainer(
                    prayerViewModel = prayerViewModel,
                    rakahViewModel = rakahViewModel,
                    quranViewModel = quranViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContainer(
    prayerViewModel: PrayerViewModel,
    rakahViewModel: RakahViewModel,
    quranViewModel: QuranViewModel
) {
    var currentTab by remember { mutableStateOf(MainTab.PRAYER_TIMES) }
    var activeCounterPrayer by remember { mutableStateOf<PrayerType?>(null) }
    var moreSubTab by remember { mutableStateOf(0) } // 0: Stats, 1: Qibla, 2: Settings
    val rakahState by rakahViewModel.state.collectAsState()

    // Location Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            prayerViewModel.loadInitial(hasLocationPermission = true)
        } else {
            prayerViewModel.loadInitial(hasLocationPermission = false)
        }
    }

    // Notification Permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Back handling: if counter is open or not on prayer times, return accordingly
    BackHandler(enabled = activeCounterPrayer != null || currentTab != MainTab.PRAYER_TIMES) {
        if (activeCounterPrayer != null) {
            activeCounterPrayer = null
        } else {
            currentTab = MainTab.PRAYER_TIMES
        }
    }

    if (activeCounterPrayer != null) {
        RakahCounterScreen(
            rakahViewModel = rakahViewModel,
            initialPrayer = activeCounterPrayer,
            onBack = { activeCounterPrayer = null }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    MainTab.PRAYER_TIMES -> {
                        PrayerTimesScreen(
                            prayerViewModel = prayerViewModel,
                            onNavigateToCounter = { prayer ->
                                activeCounterPrayer = prayer
                                rakahViewModel.selectPrayer(prayer)
                            },
                            onNavigateToStats = {
                                currentTab = MainTab.MORE
                                moreSubTab = 0
                            },
                            onRequestLocationPermission = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )
                    }

                    MainTab.QURAN_WARD -> {
                        QuranWardScreen(quranViewModel = quranViewModel)
                    }

                    MainTab.AZKAR -> {
                        AzkarScreen()
                    }

                MainTab.MORE -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-tab selector for More (Stats, Qibla, Settings)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            TabRow(
                                selectedTabIndex = moreSubTab,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = moreSubTab == 0,
                                    onClick = { moreSubTab = 0 },
                                    text = { Text("الإحصائيات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.testTag("more_subtab_stats")
                                )
                                Tab(
                                    selected = moreSubTab == 1,
                                    onClick = { moreSubTab = 1 },
                                    text = { Text("بوصلة القبلة", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.testTag("more_subtab_qibla")
                                )
                                Tab(
                                    selected = moreSubTab == 2,
                                    onClick = { moreSubTab = 2 },
                                    text = { Text("الإعدادات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.testTag("more_subtab_settings")
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (moreSubTab) {
                                0 -> PrayerStatsScreen()
                                1 -> QiblaScreen(prayerViewModel = prayerViewModel)
                                else -> SettingsScreen(
                                    prayerViewModel = prayerViewModel,
                                    rakahViewModel = rakahViewModel,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
