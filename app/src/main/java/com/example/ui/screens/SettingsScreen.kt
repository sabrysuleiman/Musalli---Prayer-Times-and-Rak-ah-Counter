package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.OfflinePrayerCalculator
import com.example.data.PrayerSoundManager
import com.example.model.PrayerType
import com.example.receiver.PrayerNotificationHelper
import com.example.ui.components.DndActivationDialog
import com.example.ui.components.SoundPickerDialog
import com.example.viewmodel.PrayerViewModel
import com.example.viewmodel.RakahViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prayerViewModel: PrayerViewModel,
    rakahViewModel: RakahViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val prefs = prayerViewModel.repository.preferences

    val dndStatus by prayerViewModel.dndStatus.collectAsState()
    val previewSoundId by prayerViewModel.previewSoundId.collectAsState()
    val nextPrayer by prayerViewModel.nextPrayer.collectAsState()
    val timingsList by prayerViewModel.timingsList.collectAsState()

    var notifyFajr by remember { mutableStateOf(prefs.notifyFajr) }
    var notifyDhuhr by remember { mutableStateOf(prefs.notifyDhuhr) }
    var notifyAsr by remember { mutableStateOf(prefs.notifyAsr) }
    var notifyMaghrib by remember { mutableStateOf(prefs.notifyMaghrib) }
    var notifyIsha by remember { mutableStateOf(prefs.notifyIsha) }

    var hapticFeedback by remember { mutableStateOf(prefs.counterHapticEnabled) }
    var selectedMethodIndex by remember { mutableStateOf(prefs.calculationMethodIndex) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showDndDialog by remember { mutableStateOf(false) }

    var selectedPrayerForSound by remember { mutableStateOf<String?>(null) } // "fajr", "dhuhr", etc.
    var selectedPrayerTitleForSound by remember { mutableStateOf("") }

    val methods = listOf(
        OfflinePrayerCalculator.CalculationMethod.UMM_AL_QURA,
        OfflinePrayerCalculator.CalculationMethod.EGYPTIAN,
        OfflinePrayerCalculator.CalculationMethod.MWL,
        OfflinePrayerCalculator.CalculationMethod.ISNA,
        OfflinePrayerCalculator.CalculationMethod.KARACHI
    )

    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = { Text("طريقة حساب مواقيت الصلاة", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    methods.forEachIndexed { index, method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMethodIndex = index
                                    prayerViewModel.updateCalculationMethod(index)
                                    showMethodDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethodIndex == index,
                                onClick = {
                                    selectedMethodIndex = index
                                    prayerViewModel.updateCalculationMethod(index)
                                    showMethodDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = method.arabicTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedMethodIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text("إغلاق")
                }
            }
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

    selectedPrayerForSound?.let { prayerId ->
        SoundPickerDialog(
            prayerTitle = selectedPrayerTitleForSound,
            currentSoundId = prayerViewModel.getPrayerSound(prayerId),
            playingPreviewSoundId = previewSoundId,
            onPlayPreview = { soundId -> prayerViewModel.playSoundPreview(soundId) },
            onStopPreview = { prayerViewModel.stopSoundPreview() },
            onSaveSound = { soundId, applyToAll ->
                if (applyToAll) {
                    prayerViewModel.setAllPrayersSound(soundId)
                } else {
                    prayerViewModel.setPrayerSound(prayerId, soundId)
                }
            },
            onDismiss = { selectedPrayerForSound = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات والتنبيهات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Stats Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth().testTag("stats_card")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "إجمالي الصلوات المكتملة عبر العداد",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${prefs.completedPrayersCount} صلاة",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // ==========================================
            // DO NOT DISTURB (DND) SECTION
            // ==========================================
            Text(
                text = "وضع عدم الإزعاج (Do Not Disturb)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (dndStatus.isActive) Color(0xFF1E2F28) else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().testTag("dnd_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (dndStatus.isActive) Icons.Default.DoNotDisturbOn else Icons.Default.DoNotDisturbOff,
                                contentDescription = null,
                                tint = if (dndStatus.isActive) Color(0xFFF3C766) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (dndStatus.isActive) "وضع عدم الإزعاج: نشط الآن" else "وضع عدم الإزعاج: متوقف",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dndStatus.isActive) Color(0xFFF3C766) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (dndStatus.isActive) "متبقي: ${dndStatus.remainingFormatted}" else "كتم التنبيهات مع استثناء صلاة الفجر",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (dndStatus.isActive) Color(0xCCFFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (dndStatus.isActive) {
                            Button(
                                onClick = { prayerViewModel.deactivateDnd() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E3A3A)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("deactivate_dnd_settings_btn")
                            ) {
                                Text("إلغاء الآن")
                            }
                        } else {
                            Button(
                                onClick = { showDndDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("activate_dnd_settings_btn")
                            ) {
                                Text("تفعيل بمؤقت")
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (dndStatus.isActive) Color(0x33FFFFFF) else MaterialTheme.colorScheme.outlineVariant
                    )

                    // Bypass Fajr Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تجاوز صلاة الفجر لوضع عدم الإزعاج",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dndStatus.isActive) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "يضمن إيقاظك لصلاة الفجر حتى لو كان وضع عدم الإزعاج مفعلاً",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (dndStatus.isActive) Color(0xAAFFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dndStatus.bypassFajr,
                            onCheckedChange = {
                                prayerViewModel.preferences.dndBypassFajr = it
                                prayerViewModel.setDndBypass("fajr", it)
                            },
                            modifier = Modifier.testTag("settings_bypass_fajr_switch")
                        )
                    }
                }
            }

            // ==========================================
            // SOUND CUSTOMIZATION SECTION (At least 5 sounds)
            // ==========================================
            Text(
                text = "تخصيص أصوات ونغمات التنبيه (أكثر من ٥ أصوات)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("prayer_sounds_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val prayers = listOf(
                        Triple("fajr", "صلاة الفجر", "fajr_special"),
                        Triple("dhuhr", "صلاة الظهر", "adhan_makkah"),
                        Triple("asr", "صلاة العصر", "adhan_madinah"),
                        Triple("maghrib", "صلاة المغرب", "adhan_aqsa"),
                        Triple("isha", "صلاة العشاء", "adhan_makkah")
                    )

                    prayers.forEachIndexed { index, (id, title, _) ->
                        val currentSoundId = prayerViewModel.getPrayerSound(id)
                        val soundItem = PrayerSoundManager.getSoundItem(currentSoundId)
                        val isPlaying = previewSoundId == currentSoundId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPrayerForSound = id
                                    selectedPrayerTitleForSound = title
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = soundItem.nameArabic,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Mini preview button
                                if (currentSoundId != "silent_vibrate") {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                prayerViewModel.stopSoundPreview()
                                            } else {
                                                prayerViewModel.playSoundPreview(currentSoundId)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "استماع",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        selectedPrayerForSound = id
                                        selectedPrayerTitleForSound = title
                                    }
                                ) {
                                    Text("تغيير")
                                }
                            }
                        }

                        if (index < prayers.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // Calculation Method Setting
            Text(
                text = "طريقة الحساب الفلكي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMethodDialog = true }
                    .testTag("calculation_method_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الجهة المعتمدة للحساب",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = methods.getOrNull(selectedMethodIndex)?.arabicTitle ?: "أم القرى",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
            }

            // Prayer Notifications Toggles
            Text(
                text = "تفعيل / إلغاء تنبيهات الصلوات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PrayerNotificationSwitch("صلاة الفجر", notifyFajr) {
                        notifyFajr = it
                        prayerViewModel.toggleNotification("fajr", it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PrayerNotificationSwitch("صلاة الظهر", notifyDhuhr) {
                        notifyDhuhr = it
                        prayerViewModel.toggleNotification("dhuhr", it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PrayerNotificationSwitch("صلاة العصر", notifyAsr) {
                        notifyAsr = it
                        prayerViewModel.toggleNotification("asr", it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PrayerNotificationSwitch("صلاة المغرب", notifyMaghrib) {
                        notifyMaghrib = it
                        prayerViewModel.toggleNotification("maghrib", it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PrayerNotificationSwitch("صلاة العشاء", notifyIsha) {
                        notifyIsha = it
                        prayerViewModel.toggleNotification("isha", it)
                    }
                }
            }

            // Test Notification Button
            OutlinedButton(
                onClick = {
                    onRequestNotificationPermission()
                    val soundId = prefs.getPrayerSound("asr")
                    PrayerSoundManager.playPreview(context, soundId)
                    PrayerNotificationHelper.showPrayerNotification(
                        context = context,
                        prayerId = "asr",
                        prayerNameArabic = "العصر",
                        timeString = "تجربة إشعار الأذان",
                        bypassedDnd = false,
                        wasSilencedByDnd = false
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("test_notification_btn")
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تجربة إشعار وتنبيه الأذان بالصوت المخصص")
            }

            // Counter Preferences
            Text(
                text = "إعدادات عداد الركعات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الاهتزاز اللمسي عند كل لمسة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "إشعار اهتزازي خفيف يؤكد تسجيل الركعة أو السجدة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hapticFeedback,
                            onCheckedChange = {
                                hapticFeedback = it
                                prefs.counterHapticEnabled = it
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PrayerNotificationSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
