package com.nadodiganesh.meetcost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nadodiganesh.meetcost.data.CurrencyRepository
import com.nadodiganesh.meetcost.data.RealCurrencyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

class MeetingViewModel : ViewModel() {
    private val repository: CurrencyRepository = RealCurrencyRepository()

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

    private var tickerJob: kotlinx.coroutines.Job? = null

    init {
        fetchRates()
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                if (_isTracking.value) {
                    delay(1000L)
                    _elapsedSeconds.value += 1
                    recalculateCost()
                } else {
                    delay(200L)
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
    }

    fun pause() {
        _isTracking.value = false
    }

    fun endMeeting() {
        _isTracking.value = false
        _isEnded.value = true
    }

    fun reset() {
        _isTracking.value = false
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
}
