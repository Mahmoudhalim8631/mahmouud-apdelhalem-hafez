package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppConfig
import com.example.data.PrayerSettings
import com.example.util.PrayerTimeCalculator
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Professional color palette representing twilight & Islamic design aesthetics
val TwilightDarkBlue = Color(0xFF0D1B2A)
val TwilightDeepSlate = Color(0xFF1B263B)
val IslamicEmerald = Color(0xFF10B981)
val IslamicEmeraldLight = Color(0xFF34D399)
val SolarGold = Color(0xFFF59E0B)
val MutedText = Color(0xFF94A3B8)
val LightSurface = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun PrayerDashboard(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Observe DB state
    val prayerList by viewModel.prayerSettingsList.collectAsState()
    val configState by viewModel.appConfig.collectAsState()
    val todayPrayers by viewModel.todayPrayers.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    // Local notification permission status
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val reqPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Refresh count-down state every second
    var tickTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            tickTimeMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    // Interactive custom state
    var showLocationSelector by remember { mutableStateOf(false) }
    var expandedPrayerId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TwilightDarkBlue, TwilightDeepSlate, LightSurface)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // APP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Prayer Alerts",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp
                        )
                    )
                    configState?.let { config ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { showLocationSelector = !showLocationSelector }
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = SolarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${config.cityName} (${config.latitude}, ${config.longitude})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = SolarGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = if (showLocationSelector) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle location",
                                tint = SolarGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Alert Test / Resync Button
                IconButton(
                    onClick = { viewModel.resyncAlarms() },
                    modifier = Modifier
                        .background(LightSurface, RoundedCornerShape(12.dp))
                        .testTag("resync_alarms_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resync alarms",
                        tint = IslamicEmerald
                    )
                }
            }

            // PERMISSION NOTIFICATION PROP
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { reqPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        .testTag("permission_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alert",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications Disabled",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Alarms cannot sound without notification permission. Tap to permit.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFECACA))
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Fix permission",
                            tint = Color.White
                        )
                    }
                }
            }

            // CALCULATE NEXT ALARM COUNTDOWN
            val nextAlarmDetails = remember(prayerList, todayPrayers, tickTimeMs) {
                findNextUpcomingAlarm(prayerList, todayPrayers, tickTimeMs)
            }

            // COUNTDOWN HERO CARD
            HeroCountdownCard(nextAlarmDetails)

            Spacer(modifier = Modifier.height(16.dp))

            // REGION SELECTOR PANEL
            AnimatedVisibility(
                visible = showLocationSelector,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LocationConfigPanel(
                    config = configState ?: AppConfig(),
                    onUpdateConfig = { newConfig ->
                        viewModel.updateAppConfig(newConfig)
                        showLocationSelector = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TITLE OF TIMELINE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDate = remember(currentDate) {
                    val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                    sdf.format(currentDate.time)
                }
                Text(
                    text = "Timeline ($formattedDate)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Fast Day offset adjustments
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.updateDayOfYearOffset(-1) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MutedText)
                    ) {
                        Text("< Prev")
                    }
                    TextButton(
                        onClick = { viewModel.updateDayOfYearOffset(0) },
                        colors = ButtonDefaults.textButtonColors(contentColor = SolarGold)
                    ) {
                        Text("Today")
                    }
                    TextButton(
                        onClick = { viewModel.updateDayOfYearOffset(1) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MutedText)
                    ) {
                        Text("Next >")
                    }
                }
            }

            // PRAYER TIMELINE ITEMS
            Spacer(modifier = Modifier.height(8.dp))
            if (prayerList.isEmpty() || todayPrayers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = IslamicEmerald)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    prayerList.forEach { prayerSetting ->
                        val cal = todayPrayers[prayerSetting.prayerId]
                        val isExpanded = expandedPrayerId == prayerSetting.prayerId

                        PrayerItemCard(
                            settings = prayerSetting,
                            prayerCalName = prayerSetting.prayerId,
                            timeCalendar = cal,
                            isExpanded = isExpanded,
                            onToggleAlert = { enabled ->
                                viewModel.updatePrayerSettings(prayerSetting.copy(isEnabled = enabled))
                            },
                            onExpandClick = {
                                expandedPrayerId = if (isExpanded) null else prayerSetting.prayerId
                            },
                            onSaveCustomSettings = { min, isEnabled, customMsg ->
                                viewModel.updatePrayerSettings(
                                    prayerSetting.copy(
                                        minutesBefore = min,
                                        isEnabled = isEnabled,
                                        customMessage = customMsg
                                    )
                                )
                                expandedPrayerId = null
                            },
                            onTestTrigger = {
                                viewModel.triggerTestReminder(prayerSetting.displayName)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CALCULAR DETAILS & FOOTER INFO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Calculations Info",
                            tint = SolarGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculation Mechanics",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Calculations run completely offline. Sun declination and the Equation of Time are generated mathematically to achieve high precision. Sunrise and Sunset are calculated exactly using atmospheric solar refraction adjustments (0.833°).",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText, lineHeight = 16.sp)
                    )
                }
            }
        }
    }
}

// Model representing next upcoming alarm
data class NextAlarmInfo(
    val prayerId: String,
    val prayerName: String,
    val minutesBefore: Int,
    val alertTimeCal: Calendar,
    val hoursRemaining: Long,
    val minutesRemaining: Long,
    val secondsRemaining: Long
)

// Main utility engine to find next alarm
fun findNextUpcomingAlarm(
    settings: List<PrayerSettings>,
    prayers: Map<String, Calendar>,
    nowMs: Long
): NextAlarmInfo? {
    if (settings.isEmpty() || prayers.isEmpty()) return null

    var closestAlertMs = Long.MAX_VALUE
    var selectedSetting: PrayerSettings? = null
    var selectedAlertCal: Calendar? = null

    settings.forEach { setting ->
        if (!setting.isEnabled) return@forEach
        
        val pCal = prayers[setting.prayerId] ?: return@forEach

        // Construct today's alert time
        val alertCalToday = pCal.clone() as Calendar
        alertCalToday.add(Calendar.MINUTE, -setting.minutesBefore)
        var alertMs = alertCalToday.timeInMillis

        // If it was already in the past, calculate for tomorrow!
        if (alertMs <= nowMs) {
            val alertCalTomorrow = pCal.clone() as Calendar
            alertCalTomorrow.add(Calendar.DAY_OF_YEAR, 1)
            alertCalTomorrow.add(Calendar.MINUTE, -setting.minutesBefore)
            alertMs = alertCalTomorrow.timeInMillis
        }

        if (alertMs < closestAlertMs) {
            closestAlertMs = alertMs
            selectedSetting = setting
            selectedAlertCal = if (alertMs == todayAlertCalInMillis(pCal, setting.minutesBefore)) alertCalToday else {
                val c = pCal.clone() as Calendar
                c.add(Calendar.DAY_OF_YEAR, 1)
                c.add(Calendar.MINUTE, -setting.minutesBefore)
                c
            }
        }
    }

    val setting = selectedSetting ?: return null
    val alertCal = selectedAlertCal ?: return null

    val diffMs = closestAlertMs - nowMs
    if (diffMs <= 0) return null

    val totalSeconds = diffMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return NextAlarmInfo(
        prayerId = setting.prayerId,
        prayerName = setting.displayName,
        minutesBefore = setting.minutesBefore,
        alertTimeCal = alertCal,
        hoursRemaining = hours,
        minutesRemaining = minutes,
        secondsRemaining = seconds
    )
}

private fun todayAlertCalInMillis(prayerCal: Calendar, minutesBefore: Int): Long {
    val c = prayerCal.clone() as Calendar
    c.add(Calendar.MINUTE, -minutesBefore)
    return c.timeInMillis
}

@SuppressLint("DefaultLocale")
@Composable
fun HeroCountdownCard(info: NextAlarmInfo?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_countdown_card"),
        colors = CardDefaults.cardColors(containerColor = TwilightDeepSlate),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (info == null) {
                Text(
                    text = "No Alarms Scheduled Today",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = SolarGold,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Activate alarms below to begin monitoring",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText),
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "NEXT REMINDER ALERT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MutedText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val formattedRemaining = if (info.hoursRemaining > 0) {
                    String.format("%02d : %02d : %02d", info.hoursRemaining, info.minutesRemaining, info.secondsRemaining)
                } else {
                    String.format("%02d : %02d", info.minutesRemaining, info.secondsRemaining)
                }

                Text(
                    text = formattedRemaining,
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = SolarGold,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.testTag("countdown_timer_text")
                )

                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedAlertTime = sdf.format(info.alertTimeCal.time)

                Text(
                    text = "Approaching ${info.prayerName} in ${info.minutesRemaining + info.hoursRemaining * 60} min",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Text(
                    text = "Scheduled alert triggers at $formattedAlertTime (${info.minutesBefore}m before)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = IslamicEmeraldLight,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocationConfigPanel(
    config: AppConfig,
    onUpdateConfig: (AppConfig) -> Unit
) {
    var latStr by remember(config) { mutableStateOf(config.latitude.toString()) }
    var lonStr by remember(config) { mutableStateOf(config.longitude.toString()) }
    var selectedCityName by remember(config) { mutableStateOf(config.cityName) }
    var selectedMethodIndex by remember(config) { mutableStateOf(config.calcMethodIndex) }
    var selectedAsrMethod by remember(config) { mutableStateOf(config.asrMethod) }

    var expandedPreset by remember { mutableStateOf(false) }
    var expandedMethod by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("location_config_panel"),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardStroke(1.dp, IslamicEmerald.copy(0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Preset & Target Calculation Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )

            // Preset City Selector
            Column {
                Text(
                    text = "Choose Major Preset City",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expandedPreset,
                    onExpandedChange = { expandedPreset = !expandedPreset }
                ) {
                    TextField(
                        value = selectedCityName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPreset) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("city_dropdown"),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = TwilightDarkBlue,
                            unfocusedContainerColor = TwilightDarkBlue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPreset,
                        onDismissRequest = { expandedPreset = false },
                        modifier = Modifier.background(LightSurface)
                    ) {
                        PrayerTimeCalculator.PRESET_CITIES.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.name, color = Color.White) },
                                onClick = {
                                    selectedCityName = city.name
                                    latStr = city.latitude.toString()
                                    lonStr = city.longitude.toString()
                                    selectedMethodIndex = city.defaultMethod
                                    expandedPreset = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Custom Coordinates...", color = SolarGold) },
                            onClick = {
                                selectedCityName = "Custom"
                                expandedPreset = false
                            }
                        )
                    }
                }
            }

            // Lat / Long TextFields (Only visible/editable if "Custom" is selected, or let users adjust)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Latitude",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    TextField(
                        value = latStr,
                        onValueChange = {
                            latStr = it
                            selectedCityName = "Custom"
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lat_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = TwilightDarkBlue,
                            unfocusedContainerColor = TwilightDarkBlue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Longitude",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    TextField(
                        value = lonStr,
                        onValueChange = {
                            lonStr = it
                            selectedCityName = "Custom"
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lon_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = TwilightDarkBlue,
                            unfocusedContainerColor = TwilightDarkBlue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            // Calculation Method Dropdown
            Column {
                Text(
                    text = "Calculation Method",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expandedMethod,
                    onExpandedChange = { expandedMethod = !expandedMethod }
                ) {
                    val methodObj = PrayerTimeCalculator.CALCULATION_METHODS[selectedMethodIndex]
                    TextField(
                        value = methodObj.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMethod) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = TwilightDarkBlue,
                            unfocusedContainerColor = TwilightDarkBlue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMethod,
                        onDismissRequest = { expandedMethod = false },
                        modifier = Modifier.background(LightSurface)
                    ) {
                        PrayerTimeCalculator.CALCULATION_METHODS.forEachIndexed { index, m ->
                            DropdownMenuItem(
                                text = { Text(m.name, color = Color.White) },
                                onClick = {
                                    selectedMethodIndex = index
                                    expandedMethod = false
                                }
                            )
                        }
                    }
                }
            }

            // Asr Shadow Selector
            Column {
                Text(
                    text = "Asr Juristic Standard",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAsrMethod = 1 },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedAsrMethod == 1) IslamicEmerald.copy(0.2f) else TwilightDarkBlue
                        ),
                        border = CardStroke(1.dp, if (selectedAsrMethod == 1) IslamicEmerald else Color.Transparent)
                    ) {
                        Text(
                            text = "Standard\n(Shafi'i/Maliki/Hanbali)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (selectedAsrMethod == 1) IslamicEmeraldLight else Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAsrMethod = 2 },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedAsrMethod == 2) IslamicEmerald.copy(0.2f) else TwilightDarkBlue
                        ),
                        border = CardStroke(1.dp, if (selectedAsrMethod == 2) IslamicEmerald else Color.Transparent)
                    ) {
                        Text(
                            text = "Hanafi\n(Double Shadow)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (selectedAsrMethod == 2) IslamicEmeraldLight else Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
            }

            // Save Config Action
            Button(
                onClick = {
                    val finalLat = latStr.toDoubleOrNull() ?: config.latitude
                    val finalLon = lonStr.toDoubleOrNull() ?: config.longitude
                    onUpdateConfig(
                        AppConfig(
                            id = 1,
                            latitude = finalLat,
                            longitude = finalLon,
                            cityName = selectedCityName,
                            calcMethodIndex = selectedMethodIndex,
                            asrMethod = selectedAsrMethod
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("save_config_button"),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicEmerald)
            ) {
                Text("Save Coordinates & Method Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CardStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun PrayerItemCard(
    settings: PrayerSettings,
    prayerCalName: String,
    timeCalendar: Calendar?,
    isExpanded: Boolean,
    onToggleAlert: (Boolean) -> Unit,
    onExpandClick: () -> Unit,
    onSaveCustomSettings: (Int, Boolean, String) -> Unit,
    onTestTrigger: () -> Unit
) {
    var minInput by remember(settings) { mutableStateOf(settings.minutesBefore) }
    var msgInput by remember(settings) { mutableStateOf(settings.customMessage) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_card_${prayerCalName}"),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Summary Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info block
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val designIcon = when (prayerCalName) {
                        "fajr" -> Icons.Default.Brightness3 // Moon / Twilight
                        "sunrise" -> Icons.Default.WbTwilight // Sunrise
                        "dhuhr" -> Icons.Default.WbSunny // Mid-day Sun
                        "asr" -> Icons.Default.CloudQueue // Afternoon transition
                        "maghrib" -> Icons.Default.WbTwilight // Twilight
                        "isha" -> Icons.Default.Brightness2 // Night stars
                        else -> Icons.Default.Notifications
                    }

                    Icon(
                        imageVector = designIcon,
                        contentDescription = settings.displayName,
                        tint = if (settings.isEnabled) IslamicEmeraldLight else MutedText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = settings.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        if (settings.isEnabled) {
                            Text(
                                text = "Reminder: ${settings.minutesBefore}m before",
                                style = MaterialTheme.typography.bodySmall.copy(color = SolarGold)
                            )
                        } else {
                            Text(
                                text = "Reminders Off",
                                style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Concrete calculated prayer time
                    if (timeCalendar != null) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        Text(
                            text = sdf.format(timeCalendar.time),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }

                    // Simple instant toggle Switch
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { onToggleAlert(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = IslamicEmerald,
                            checkedTrackColor = IslamicEmerald.copy(0.3f),
                            uncheckedThumbColor = MutedText,
                            uncheckedTrackColor = TwilightDarkBlue
                        ),
                        modifier = Modifier.testTag("switch_${prayerCalName}")
                    )
                }
            }

            // Expanded Settings Block
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TwilightDarkBlue.copy(0.4f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Divider(color = MutedText.copy(0.15f))

                    // Warning offset minutes slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reminder Offset Time",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "$minInput minutes before",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = SolarGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        Slider(
                            value = minInput.toFloat(),
                            onValueChange = { minInput = it.roundToInt() },
                            valueRange = 0f..120f,
                            steps = 24,
                            colors = SliderDefaults.colors(
                                thumbColor = SolarGold,
                                activeTrackColor = SolarGold,
                                inactiveTrackColor = MutedText.copy(0.3f)
                            )
                        )

                        // Default tags reminders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SuggestionChip(
                                onClick = { minInput = 10 },
                                label = { Text("10m (Default)", fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = if (minInput == 10) SolarGold else Color.White
                                )
                            )
                            SuggestionChip(
                                onClick = { minInput = 30 },
                                label = { Text("30m", fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = if (minInput == 30) SolarGold else Color.White
                                )
                            )
                            SuggestionChip(
                                onClick = { minInput = 60 },
                                label = { Text("60m (Fajr default)", fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = if (minInput == 60) SolarGold else Color.White
                                )
                            )
                        }
                    }

                    // Custom Notification details message
                    Column {
                        Text(
                            text = "Custom Action Message",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedText, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        TextField(
                            value = msgInput,
                            onValueChange = { msgInput = it },
                            placeholder = { Text("E.g., Prayer is soon, prepare yourself!") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("message_input_${prayerCalName}"),
                            maxLines = 2,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = TwilightDarkBlue,
                                unfocusedContainerColor = TwilightDarkBlue,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Text(
                            text = "Add %d in message to auto-interpolate remaining minutes.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedText, fontSize = 11.sp)
                        )
                    }

                    // Save / Test Action Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestTrigger() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_sound_button_${prayerCalName}"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SolarGold),
                            border = CardStroke(1.dp, SolarGold)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Test sound", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Sound")
                        }

                        Button(
                            onClick = { onSaveCustomSettings(minInput, settings.isEnabled, msgInput) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_settings_button_${prayerCalName}"),
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicEmerald)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Apply changes", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Settings")
                        }
                    }
                }
            }
        }
    }
}
