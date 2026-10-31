package com.nadodiganesh.meetcost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nadodiganesh.meetcost.data.RealMeetingHistoryRepository
import com.nadodiganesh.meetcost.ui.HistoryScreen
import com.nadodiganesh.meetcost.ui.IntroTipsBanner
import com.nadodiganesh.meetcost.ui.MoneySandglass
import java.text.NumberFormat
import java.util.*

// Brand Color Palette matching Lovable Design
val TealPrimary = Color(0xFF0E7C7B)
val TealDark = Color(0xFF095C5B)
val CoralAccent = Color(0xFFFF7844)
val CoralAccentDark = Color(0xFFE65100)
val CoralSoft = Color(0xFFFFE8DE)
val MintSoft = Color(0xFFE2F4F2)

val HeroGradientLight = Brush.linearGradient(listOf(TealPrimary, TealDark))
val HeroGradientDark = Brush.linearGradient(listOf(Color(0xFF1B3836), Color(0xFF112423)))
val BurnGradient = Brush.linearGradient(listOf(CoralAccent, CoralAccentDark))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val vm: MeetingViewModel = viewModel()

            // Attach RealMeetingHistoryRepository using application context
            LaunchedEffect(Unit) {
                vm.attachHistoryRepository(RealMeetingHistoryRepository(context.applicationContext))
            }

            val themeMode by vm.appTheme.collectAsState()
            val darkTheme = when (themeMode) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            val colorScheme = if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF3EB4A8),
                    onPrimary = Color(0xFF112423),
                    primaryContainer = Color(0xFF1B3836),
                    onPrimaryContainer = Color(0xFFE2F4F2),
                    secondary = CoralAccent,
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFF3D251D),
                    onSecondaryContainer = Color(0xFFFFE8DE),
                    background = Color(0xFF121B1B),
                    onBackground = Color(0xFFF4F8F7),
                    surface = Color(0xFF1B2727),
                    onSurface = Color(0xFFF4F8F7),
                    surfaceVariant = Color(0xFF243434),
                    onSurfaceVariant = Color(0xFFB0C0BF),
                    outline = Color(0xFF354646)
                )
            } else {
                lightColorScheme(
                    primary = TealPrimary,
                    onPrimary = Color.White,
                    primaryContainer = MintSoft,
                    onPrimaryContainer = TealDark,
                    secondary = CoralAccent,
                    onSecondary = Color.White,
                    secondaryContainer = CoralSoft,
                    onSecondaryContainer = CoralAccentDark,
                    background = Color(0xFFF4F8F7),
                    onBackground = Color(0xFF1A2827),
                    surface = Color.White,
                    onSurface = Color(0xFF1A2827),
                    surfaceVariant = Color(0xFFE8EEED),
                    onSurfaceVariant = Color(0xFF5E7372),
                    outline = Color(0xFFE2EBEA)
                )
            }

            val heroGradient = if (darkTheme) HeroGradientDark else HeroGradientLight

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentScreen by vm.currentScreen.collectAsState()
                    val historyList by vm.meetingHistory.collectAsState()

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == AppScreen.HISTORY) {
                                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                            }
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.TRACKER -> TrackerScreen(
                                vm = vm,
                                heroGradient = heroGradient,
                                darkTheme = darkTheme
                            )
                            AppScreen.HISTORY -> HistoryScreen(
                                meetings = historyList,
                                onBack = { vm.navigateToTracker() },
                                onDeleteMeeting = { vm.deleteMeeting(it) },
                                onClearAll = { vm.clearAllHistory() },
                                heroGradient = heroGradient,
                                accentColor = CoralAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerScreen(
    vm: MeetingViewModel,
    heroGradient: Brush,
    darkTheme: Boolean
) {
    val costUsd by vm.currentCostUsd.collectAsState()
    val isTracking by vm.isTracking.collectAsState()
    val isEnded by vm.isEnded.collectAsState()
    val elapsedSeconds by vm.elapsedSeconds.collectAsState()
    val attendees by vm.attendees.collectAsState()
    val hourlyRate by vm.hourlyRate.collectAsState()
    val selectedCurrency by vm.selectedCurrency.collectAsState()
    val exchangeRates by vm.exchangeRates.collectAsState()
    val themeMode by vm.appTheme.collectAsState()
    val isIntroDismissed by vm.isIntroDismissed.collectAsState()

    var attendeesText by remember { mutableStateOf(attendees.toString()) }
    var rateText by remember { mutableStateOf(if (hourlyRate % 1.0 == 0.0) hourlyRate.toInt().toString() else hourlyRate.toString()) }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    val isMeetingInProgress = elapsedSeconds > 0 && !isEnded
    val canEdit = !isMeetingInProgress && !isEnded

    LaunchedEffect(attendees, hourlyRate, isEnded) {
        if (!isMeetingInProgress) {
            attendeesText = attendees.toString()
            rateText = if (hourlyRate % 1.0 == 0.0) hourlyRate.toInt().toString() else hourlyRate.toString()
        }
    }

    val exchangeRate = exchangeRates[selectedCurrency] ?: 1.0
    val currentCostSelected = costUsd * exchangeRate
    val totalRateSelected = attendees * hourlyRate
    val burnRatePerSec = totalRateSelected / 3600.0

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Section with curved bottom edge (40.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(heroGradient)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar (History Icon, Brand Logo, Theme Switcher)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { vm.navigateToHistory() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Meeting history",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // MeeTCost Logo
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MeeT",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Cost",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CoralAccent
                        )
                    }

                    // Cycle Theme Button
                    IconButton(
                        onClick = { vm.cycleTheme() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        val themeIcon = when (themeMode) {
                            AppTheme.SYSTEM -> Icons.Default.Computer
                            AppTheme.LIGHT -> Icons.Default.LightMode
                            AppTheme.DARK -> Icons.Default.DarkMode
                        }
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "Cycle theme",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sandglass Animation
                MoneySandglass(
                    isTracking = isTracking,
                    elapsedSeconds = elapsedSeconds,
                    accentColor = CoralAccent,
                    modifier = Modifier.size(96.dp, 144.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status text
                val statusText = when {
                    isEnded -> "TOTAL COST"
                    isTracking -> "BURNING MONEY…"
                    elapsedSeconds > 0 -> "PAUSED"
                    else -> "READY TO START"
                }

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.5.sp,
                    color = if (isTracking) CoralAccent else Color.White.copy(alpha = 0.70f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Main Cost Ticker (font-mono text-5xl font-bold tabular-nums)
                Text(
                    text = formatCurrencyAmount(currentCostSelected, selectedCurrency),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Secondary Currency (≈ $XX.XX USD) when selectedCurrency != USD
                if (selectedCurrency != "USD") {
                    Text(
                        text = "≈ ${formatCurrencyAmount(costUsd, "USD")}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Duration Clock Badge (font-mono text-lg font-bold tabular-nums)
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatDurationClock(elapsedSeconds),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Body Controls & Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dismissible Intro Tips Banner
            if (!isTracking && !isEnded && elapsedSeconds == 0L && !isIntroDismissed) {
                IntroTipsBanner(
                    onDismiss = { vm.dismissIntro() },
                    accentColor = CoralAccent,
                    accentSoftColor = if (darkTheme) Color(0xFF3D251D) else CoralSoft
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isEnded) {
                // Meeting Ended Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryStatCard(
                        label = "Duration",
                        value = formatDurationClock(elapsedSeconds),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "People",
                        value = attendees.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "Per min",
                        value = formatCurrencyAmount(totalRateSelected / 60.0, selectedCurrency),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Start New Meeting Button
                ActionButton(
                    text = "Start new meeting",
                    icon = Icons.AutoMirrored.Filled.RotateLeft,
                    onClick = { vm.reset() },
                    variant = ButtonVariant.PRIMARY
                )
            } else {
                val inputCardAlpha = if (canEdit) 1f else 0.6f

                // Card 1: Attendees Input & Slider
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(inputCardAlpha),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Attendees",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Soft rounded number pill: w-16 rounded-xl bg-primary-soft py-1.5 font-mono text-xl font-bold
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (darkTheme) Color(0xFF1B3836) else MintSoft)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = attendeesText,
                                    onValueChange = { input ->
                                        if (canEdit) {
                                            val num = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                                            if (num <= 99) {
                                                attendeesText = if (num > 0) num.toString() else input
                                                vm.attendees.value = num
                                            }
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    enabled = canEdit,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Custom Lovable Styled Slider: 6dp pill track, 24dp thumb with 4dp card border & shadow
                        @OptIn(ExperimentalMaterial3Api::class)
                        Slider(
                            value = attendees.toFloat().coerceIn(2f, 25f),
                            onValueChange = {
                                if (canEdit) {
                                    val rounded = it.toInt()
                                    vm.attendees.value = rounded
                                    attendeesText = rounded.toString()
                                }
                            },
                            valueRange = 2f..25f,
                            enabled = canEdit,
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .shadow(elevation = 4.dp, shape = CircleShape)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .padding(4.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            },
                            track = { sliderState ->
                                val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                                val fraction = if (range > 0f) {
                                    ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
                                } else 0f

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("2", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("25", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 2: Hourly Rate & Currency Selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(inputCardAlpha),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Avg. hourly rate per person",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Rate Input Box: rounded-2xl border bg-background px-4 py-3 font-mono text-2xl font-bold
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = rateText,
                                    onValueChange = { input ->
                                        if (canEdit) {
                                            rateText = input
                                            vm.hourlyRate.value = input.toDoubleOrNull() ?: 0.0
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    enabled = canEdit,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                )
                            }

                            // Currency Dropdown Pill: rounded-2xl bg-accent-soft py-3 pl-4 pr-9 font-bold text-foreground
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (darkTheme) Color(0xFF3D251D) else CoralSoft)
                                        .clickable(enabled = canEdit) { showCurrencyMenu = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedCurrency,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select currency",
                                        tint = CoralAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showCurrencyMenu,
                                    onDismissRequest = { showCurrencyMenu = false }
                                ) {
                                    vm.availableCurrencies.forEach { curr ->
                                        DropdownMenuItem(
                                            text = { Text(curr, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                vm.setCurrency(curr)
                                                showCurrencyMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Burn Rate display: Burn rate: <span class="font-mono font-semibold text-accent">$0.10/sec</span>
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Burn rate: ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${formatCurrencyAmount(burnRatePerSec, selectedCurrency)}/sec",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoralAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action Buttons
                if (isTracking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            text = "Pause",
                            icon = Icons.Default.Pause,
                            onClick = { vm.pause() },
                            variant = ButtonVariant.SOFT,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            text = "End",
                            icon = Icons.Default.Stop,
                            onClick = { vm.endMeeting() },
                            variant = ButtonVariant.DANGER,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else if (elapsedSeconds > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            text = "Resume",
                            icon = Icons.Default.PlayArrow,
                            onClick = { vm.startOrResume() },
                            variant = ButtonVariant.BURN,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            text = "Reset",
                            icon = Icons.AutoMirrored.Filled.RotateLeft,
                            onClick = { vm.reset() },
                            variant = ButtonVariant.SOFT,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    ActionButton(
                        text = "Start meeting",
                        icon = Icons.Default.PlayArrow,
                        onClick = { vm.startOrResume() },
                        variant = ButtonVariant.BURN,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

enum class ButtonVariant {
    BURN, PRIMARY, SOFT, DANGER
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    variant: ButtonVariant,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)

    when (variant) {
        ButtonVariant.BURN -> {
            Box(
                modifier = modifier
                    .height(56.dp)
                    .shadow(elevation = 8.dp, shape = shape, spotColor = CoralAccent)
                    .clip(shape)
                    .background(BurnGradient)
                    .clickable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = modifier.height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        ButtonVariant.SOFT -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled),
                modifier = modifier.height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        ButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = modifier.height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

fun formatCurrencyAmount(amount: Double, currencyCode: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        format.currency = Currency.getInstance(currencyCode)
        format.format(amount)
    } catch (e: Exception) {
        String.format(Locale.US, "%.2f %s", amount, currencyCode)
    }
}

fun formatDurationClock(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
