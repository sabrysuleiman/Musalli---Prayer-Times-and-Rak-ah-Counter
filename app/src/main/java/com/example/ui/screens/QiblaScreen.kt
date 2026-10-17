package com.example.ui.screens

import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompassSensorManager
import com.example.data.QiblaUtil
import com.example.viewmodel.PrayerViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(prayerViewModel: PrayerViewModel) {
    val context = LocalContext.current
    val schedule by prayerViewModel.schedule.collectAsState()

    val lat = schedule?.latitude ?: 21.4225
    val lng = schedule?.longitude ?: 39.8262
    val cityName = schedule?.locationName ?: "مكة المكرمة"

    val qiblaBearing = remember(lat, lng) {
        QiblaUtil.calculateQiblaBearing(lat, lng).toFloat()
    }

    val distanceKm = remember(lat, lng) {
        QiblaUtil.calculateDistanceToKaabaKm(lat, lng)
    }

    val sensorManager = remember { CompassSensorManager(context) }
    val orientationState by sensorManager.orientation.collectAsState()

    DisposableEffect(Unit) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    // Relative angle between current heading and Kaaba
    val deviceAzimuth = orientationState.azimuthDegrees
    val relativeQiblaAngle = (qiblaBearing - deviceAzimuth + 360f) % 360f
    val isFacingQibla = abs((relativeQiblaAngle + 180f) % 360f - 180f) <= 4f

    // Vibrate gently once when entering Qibla alignment
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    var wasFacing by remember { mutableStateOf(false) }
    LaunchedEffect(isFacingQibla) {
        if (isFacingQibla && !wasFacing) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            } catch (_: Exception) {}
        }
        wasFacing = isFacingQibla
    }

    val animatedDialRotation by animateFloatAsState(
        targetValue = -deviceAzimuth,
        animationSpec = spring(stiffness = 300f),
        label = "dial_rotation"
    )

    val animatedNeedleRotation by animateFloatAsState(
        targetValue = relativeQiblaAngle,
        animationSpec = spring(stiffness = 300f),
        label = "needle_rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اتجاه القِبلة الشريفة",
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Location and Distance Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qibla_info_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = cityName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المسافة إلى الكعبة: $distanceKm كم • الزاوية: ${qiblaBearing.roundToInt()}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Compass Container
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .testTag("qibla_compass_container"),
                contentAlignment = Alignment.Center
            ) {
                // Glowing outer halo when aligned with Qibla
                if (isFacingQibla) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0x550F5A44), Color(0x22F3C766), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )
                }

                // Rotating Compass Dial
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedDialRotation),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2 - 16.dp.toPx()

                        // Outer ring
                        drawCircle(
                            color = if (isFacingQibla) Color(0xFF0F5A44) else Color(0x330F5A44),
                            radius = radius,
                            center = center,
                            style = Stroke(width = if (isFacingQibla) 5.dp.toPx() else 3.dp.toPx())
                        )

                        // 360 degree ticks
                        for (deg in 0 until 360 step 15) {
                            val angleRad = Math.toRadians(deg.toDouble())
                            val isMajor = deg % 90 == 0
                            val isSemi = deg % 30 == 0
                            val tickLen = if (isMajor) 16.dp.toPx() else if (isSemi) 10.dp.toPx() else 6.dp.toPx()

                            val startX = center.x + (radius - tickLen) * kotlin.math.sin(angleRad).toFloat()
                            val startY = center.y - (radius - tickLen) * kotlin.math.cos(angleRad).toFloat()
                            val endX = center.x + radius * kotlin.math.sin(angleRad).toFloat()
                            val endY = center.y - radius * kotlin.math.cos(angleRad).toFloat()

                            val color = when {
                                deg == 0 -> Color(0xFFD32F2F) // North = Red
                                isMajor -> Color(0xFF0F5A44)
                                else -> Color(0x440F5A44)
                            }

                            drawLine(
                                color = color,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = if (isMajor) 3.5.dp.toPx() else 1.5.dp.toPx()
                            )
                        }
                    }

                    // Cardinal Directions (N, E, S, W in Arabic)
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "شمال",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 22.dp)
                        )
                        Text(
                            text = "جنوب",
                            color = Color(0xFF0F5A44),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 22.dp)
                        )
                        Text(
                            text = "شرق",
                            color = Color(0xFF0F5A44),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 22.dp)
                        )
                        Text(
                            text = "غرب",
                            color = Color(0xFF0F5A44),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 22.dp)
                        )
                    }
                }

                // Kaaba Direction Pointer (Relative needle that points directly to Kaaba)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedNeedleRotation),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val needleLen = size.minDimension / 2 - 32.dp.toPx()

                        // Golden Kaaba Needle Arrow
                        val needlePath = Path().apply {
                            moveTo(center.x, center.y - needleLen)
                            lineTo(center.x + 14.dp.toPx(), center.y)
                            lineTo(center.x - 14.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(
                            needlePath,
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFF3C766), Color(0xFFD4AF37))
                            )
                        )

                        // Back needle
                        val backNeedle = Path().apply {
                            moveTo(center.x, center.y + needleLen * 0.45f)
                            lineTo(center.x + 9.dp.toPx(), center.y)
                            lineTo(center.x - 9.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(backNeedle, color = Color(0x330F5A44))

                        // Center hub
                        drawCircle(color = Color(0xFF0F5A44), radius = 12.dp.toPx(), center = center)
                        drawCircle(color = Color(0xFFF3C766), radius = 6.dp.toPx(), center = center)
                    }

                    // Kaaba Icon badge at needle tip
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 18.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isFacingQibla) Color(0xFF0F5A44) else Color(0xFFD4AF37),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Mosque,
                                    contentDescription = "الكعبة المشرفة",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Direction Feedback & Guidance Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFacingQibla) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qibla_alignment_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isFacingQibla) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أنت الآن باتجاه القِبلة الشريفة مباشرة! ✨",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val turnDir = if (relativeQiblaAngle > 180f) "يساراً" else "يميناً"
                        Text(
                            text = "أدر هاتفك $turnDir حتى يتطابق السهم الذهبي مع الأعلى",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الاتجاه الحالي للمستشعر: ${deviceAzimuth.roundToInt()}° • زاوية مكة: ${qiblaBearing.roundToInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (orientationState.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || orientationState.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 للمعايرة: حرّك هاتفك على شكل رقم 8 في الهواء لتحسين دقة البوصلة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD4AF37),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
