package com.nadodiganesh.meetcost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nadodiganesh.meetcost.data.CurrencyRepository
import com.nadodiganesh.meetcost.data.MeetingHistoryRepository
import com.nadodiganesh.meetcost.data.MeetingRecord
import com.nadodiganesh.meetcost.data.RealCurrencyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class AppScreen {
    TRACKER, HISTORY
}

class MeetingViewModel(
    private val repository: CurrencyRepository = RealCurrencyRepository(),
    private var historyRepository: MeetingHistoryRepository? = null
) : ViewModel() {

    val availableCurrencies = listOf("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "BRL")

    var attendees = MutableStateFlow(5)
    var hourlyRate = MutableStateFlow(75.0)

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _isEnded = MutableStateFlow(false)
    val isEnded: StateFlow<Boolean> = _isEnded

    private val _currentCostUsd = MutableStateFlow(0.0)
    val currentCostUsd: StateFlow<Double> = _currentCostUsd

    private val _selectedCurrency = MutableStateFlow("USD")
    val selectedCurrency: StateFlow<String> = _selectedCurrency

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme

    private val _currentScreen = MutableStateFlow(AppScreen.TRACKER)
    val currentScreen: StateFlow<AppScreen> = _currentScreen

    private val _meetingHistory = MutableStateFlow<List<MeetingRecord>>(emptyList())
    val meetingHistory: StateFlow<List<MeetingRecord>> = _meetingHistory

    private val _isIntroDismissed = MutableStateFlow(false)
    val isIntroDismissed: StateFlow<Boolean> = _isIntroDismissed

    private var tickerJob: kotlinx.coroutines.Job? = null

    init {
        fetchRates()
        observeHistory()
    }

    fun attachHistoryRepository(repo: MeetingHistoryRepository) {
        historyRepository = repo
        observeHistory()
    }

    private fun observeHistory() {
        historyRepository?.let { repo ->
            viewModelScope.launch {
                repo.meetings.collect { list ->
                    _meetingHistory.value = list
                }
            }
            viewModelScope.launch {
                repo.isIntroDismissed.collect { dismissed ->
                    _isIntroDismissed.value = dismissed
                }
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive && _isTracking.value) {
                delay(1000L)
                if (_isTracking.value) {
                    _elapsedSeconds.value += 1
                    recalculateCost()
                }
            }
        }
    }

    private fun fetchRates() {
        viewModelScope.launch {
            repository.fetchRates("USD")?.let {
                _exchangeRates.value = it
            }
        }
    }

    private fun recalculateCost() {
        val totalHours = _elapsedSeconds.value / 3600.0
        
        val rateInUsd = if (_selectedCurrency.value == "USD") {
            hourlyRate.value
        } else {
            val exchangeRate = _exchangeRates.value[_selectedCurrency.value] ?: 1.0
            if (exchangeRate > 0) hourlyRate.value / exchangeRate else hourlyRate.value
        }

        _currentCostUsd.value = attendees.value * rateInUsd * totalHours
    }

    fun startOrResume() {
        _isTracking.value = true
        _isEnded.value = false
        startTicker()
    }

    fun pause() {
        _isTracking.value = false
        tickerJob?.cancel()
    }

    fun endMeeting() {
        _isTracking.value = false
        tickerJob?.cancel()
        _isEnded.value = true

        val duration = _elapsedSeconds.value
        val costUsd = _currentCostUsd.value
        val exchangeRate = _exchangeRates.value[_selectedCurrency.value] ?: 1.0
        val costSelected = costUsd * exchangeRate

        val record = MeetingRecord(
            endedAt = System.currentTimeMillis(),
            durationSec = duration,
            attendees = attendees.value,
            hourlyRate = hourlyRate.value,
            currency = _selectedCurrency.value,
            cost = costSelected,
            costUsd = costUsd
        )
        _meetingHistory.value = listOf(record) + _meetingHistory.value
        viewModelScope.launch {
            historyRepository?.addMeeting(record)
        }
    }

    fun reset() {
        _isTracking.value = false
        tickerJob?.cancel()
        _isEnded.value = false
        _elapsedSeconds.value = 0L
        _currentCostUsd.value = 0.0
    }

    fun setCurrency(code: String) {
        _selectedCurrency.value = code
    }

    fun setTheme(theme: AppTheme) {
        _appTheme.value = theme
    }

    fun cycleTheme() {
        _appTheme.value = when (_appTheme.value) {
            AppTheme.SYSTEM -> AppTheme.LIGHT
            AppTheme.LIGHT -> AppTheme.DARK
            AppTheme.DARK -> AppTheme.SYSTEM
        }
    }

    fun navigateToHistory() {
        _currentScreen.value = AppScreen.HISTORY
    }

    fun navigateToTracker() {
        _currentScreen.value = AppScreen.TRACKER
    }

    fun deleteMeeting(id: String) {
        _meetingHistory.value = _meetingHistory.value.filter { it.id != id }
        viewModelScope.launch {
            historyRepository?.deleteMeeting(id)
        }
    }

    fun clearAllHistory() {
        _meetingHistory.value = emptyList()
        viewModelScope.launch {
            historyRepository?.clearHistory()
        }
    }

    fun dismissIntro() {
        _isIntroDismissed.value = true
        viewModelScope.launch {
            historyRepository?.setIntroDismissed(true)
        }
    }
}
