package com.nadodiganesh.meetcost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nadodiganesh.meetcost.ui.MoneySandglass
import java.util.Locale

// Custom Turquoise & Orange Color Scheme
val TurquoisePrimary = Color(0xFF008080) // Teal
val OrangeSecondary = Color(0xFFFF8C00) // Dark Orange
val TurquoiseContainer = Color(0xFFE0F2F1)
val OrangeContainer = Color(0xFFFFF3E0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MeetingViewModel = viewModel()
            val themeMode by vm.appTheme.collectAsState()
            
            val darkTheme = when (themeMode) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            val colorScheme = if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF4DB6AC), // Lighter turquoise for dark
                    secondary = Color(0xFFFFB74D), // Lighter orange for dark
                    onPrimary = Color.Black,
                    onSecondary = Color.Black
                )
            } else {
                lightColorScheme(
                    primary = TurquoisePrimary,
                    secondary = OrangeSecondary,
                    primaryContainer = TurquoiseContainer,
                    secondaryContainer = OrangeContainer,
                    onPrimary = Color.White,
                    onSecondary = Color.White
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TrackerScreen(vm)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(vm: MeetingViewModel) {
    val costUsd by vm.currentCostUsd.collectAsState()
    val isTracking by vm.isTracking.collectAsState()
    val isEnded by vm.isEnded.collectAsState()
    val elapsedSeconds by vm.elapsedSeconds.collectAsState()
    val attendees by vm.attendees.collectAsState()
    val hourlyRate by vm.hourlyRate.collectAsState()
    val selectedCurrency by vm.selectedCurrency.collectAsState()
    val exchangeRates by vm.exchangeRates.collectAsState()

    var attendeesText by remember { mutableStateOf(attendees.toString()) }
    var rateText by remember { mutableStateOf(hourlyRate.toString()) }
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val isMeetingInProgress = elapsedSeconds > 0 && !isEnded
    val canEdit = !isMeetingInProgress && !isEnded

    LaunchedEffect(attendees, hourlyRate, isEnded) {
        if (!isMeetingInProgress) {
            attendeesText = attendees.toString()
            rateText = hourlyRate.toString()
        }
    }

    val exchangeRate = exchangeRates[selectedCurrency] ?: 1.0
    val rateInUsd = if (selectedCurrency == "USD") hourlyRate else (if (exchangeRate > 0) hourlyRate / exchangeRate else hourlyRate)
    val burnRate = (attendees * rateInUsd) / 3600.0

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered App Name with subtle theme switcher
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "MeeTCost",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // Minimalist theme switcher (replaces "SYSTEM" word)
            TextButton(
                onClick = { showThemeMenu = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("🎨", fontSize = 18.sp)
                DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                    AppTheme.values().forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.name) },
                            onClick = {
                                vm.setTheme(theme)
                                showThemeMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Money Sandglass Animation
        Box(
            modifier = Modifier.height(180.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MoneySandglass(
                isTracking = isTracking,
                burnRate = burnRate,
                modifier = Modifier.size(130.dp, 180.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isEnded) {
            Text(
                text = if (isTracking) "BURNING MONEY..." else "PAUSED",
                style = MaterialTheme.typography.labelLarge,
                color = if (isTracking) MaterialTheme.colorScheme.secondary else Color.Gray,
                fontWeight = FontWeight.Bold
            )
            
            val currentCostSelected = costUsd * exchangeRate
            Text(
                text = if (selectedCurrency == "USD") {
                    String.format(Locale.US, "$%.2f", currentCostSelected)
                } else {
                    String.format(Locale.US, "%.2f %s", currentCostSelected, selectedCurrency)
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isTracking) MaterialTheme.colorScheme.onBackground else Color.Gray
            )
            if (selectedCurrency != "USD") {
                Text(
                    text = String.format(Locale.US, "($%.2f USD)", costUsd),
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            DurationClock(elapsedSeconds)
        } else {
            SummaryView(costUsd, selectedCurrency, exchangeRates, attendees, elapsedSeconds)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!isEnded) {
            // Attendees Input (Range 2 to 25)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Attendees: $attendees",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (canEdit) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Slider(
                        value = attendees.toFloat().coerceIn(2f, 25f),
                        onValueChange = { 
                            vm.attendees.value = it.toInt()
                            attendeesText = it.toInt().toString()
                        },
                        valueRange = 2f..25f,
                        steps = 22,
                        modifier = Modifier.weight(1f),
                        enabled = canEdit
                    )
                    OutlinedTextField(
                        value = attendeesText,
                        onValueChange = {
                            val newVal = it.toIntOrNull() ?: 0
                            if (newVal < 100) {
                                attendeesText = it
                                vm.attendees.value = newVal
                            }
                        },
                        modifier = Modifier.width(70.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = canEdit,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rate Input with inline Currency Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = {
                            rateText = it
                            vm.hourlyRate.value = it.toDoubleOrNull() ?: 0.0
                        },
                        label = { Text("Hourly Rate") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = canEdit,
                        suffix = { Text(if (selectedCurrency == "USD") "$" else selectedCurrency) }
                    )
                    
                    Box {
                        OutlinedCard(
                            onClick = { if (canEdit) showCurrencyMenu = true },
                            enabled = canEdit,
                            modifier = Modifier.height(56.dp).padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedCurrency, fontWeight = FontWeight.Bold)
                                Text(" ▼", fontSize = 10.sp)
                            }
                        }
                        DropdownMenu(expanded = showCurrencyMenu, onDismissRequest = { showCurrencyMenu = false }) {
                            vm.availableCurrencies.forEach { curr ->
                                DropdownMenuItem(text = { Text(curr) }, onClick = {
                                    vm.setCurrency(curr)
                                    showCurrencyMenu = false
                                })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isTracking) {
                    Button(onClick = { vm.pause() }, modifier = Modifier.weight(1f)) { Text("Pause") }
                    Button(onClick = { vm.endMeeting() }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error), modifier = Modifier.weight(1f)) { Text("End") }
                } else {
                    Button(onClick = { vm.startOrResume() }, modifier = Modifier.weight(1f)) { Text(if (elapsedSeconds > 0) "Resume" else "Start") }
                    if (elapsedSeconds > 0) {
                        Button(onClick = { vm.reset() }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary), modifier = Modifier.weight(1f)) { Text("Reset") }
                    }
                }
            }
        } else {
            Button(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) { Text("Start New Meeting") }
        }
    }
}

@Composable
fun DurationClock(seconds: Long) {
    val hh = seconds / 3600
    val mm = (seconds % 3600) / 60
    val ss = seconds % 60
    val timeString = if (hh > 0) String.format(Locale.US, "%02d:%02d:%02d", hh, mm, ss) else String.format(Locale.US, "%02d:%02d", mm, ss)
    
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
        Text(text = timeString, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SummaryView(costUsd: Double, targetCurrency: String, rates: Map<String, Double>, attendees: Int, elapsedSeconds: Long) {
    val rate = rates[targetCurrency] ?: 1.0
    val costTarget = costUsd * rate
    val hh = elapsedSeconds / 3600
    val mm = (elapsedSeconds / 60) % 60
    val ss = elapsedSeconds % 60
    
    val durationText = if (hh > 0) "${hh}h ${mm}m ${ss}s" else "${mm}m ${ss}s"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TOTAL COST", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text = if (targetCurrency == "USD") {
                String.format(Locale.US, "$%.2f", costTarget)
            } else {
                String.format(Locale.US, "%.2f %s", costTarget, targetCurrency)
            },
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold
        )
        if (targetCurrency != "USD") {
            Text(String.format(Locale.US, "≈ $%.2f USD", costUsd), fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Duration: $durationText", style = MaterialTheme.typography.bodyLarge)
                Text("Participants: $attendees", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
