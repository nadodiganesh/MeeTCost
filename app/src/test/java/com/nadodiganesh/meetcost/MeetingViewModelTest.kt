package com.nadodiganesh.meetcost

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingViewModelTest {

    private lateinit var viewModel: MeetingViewModel
    private lateinit var repository: FakeCurrencyRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCurrencyRepository(mapOf("EUR" to 0.85))
        viewModel = MeetingViewModel(repository)
    }

    @After
    fun tearDown() {
        viewModel.pause()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        assertEquals(5, viewModel.attendees.value)
        assertEquals(75.0, viewModel.hourlyRate.value, 0.0)
        assertEquals(0.0, viewModel.currentCostUsd.value, 0.0)
        assertEquals(false, viewModel.isTracking.value)
        assertEquals(false, viewModel.isEnded.value)
    }

    @Test
    fun `start meeting updates tracking state`() = runTest {
        viewModel.startOrResume()
        assertEquals(true, viewModel.isTracking.value)
        assertEquals(false, viewModel.isEnded.value)
        viewModel.pause()
    }

    @Test
    fun `reset meeting clears cost and elapsed time`() = runTest {
        viewModel.startOrResume()
        viewModel.reset()
        assertEquals(0.0, viewModel.currentCostUsd.value, 0.0)
        assertEquals(0L, viewModel.elapsedSeconds.value)
        assertEquals(false, viewModel.isTracking.value)
    }

    @Test
    fun `set currency updates selected currency`() = runTest {
        viewModel.setCurrency("EUR")
        viewModel.selectedCurrency.test {
            assertEquals("EUR", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cycle theme cycles between system, light, and dark`() = runTest {
        assertEquals(AppTheme.SYSTEM, viewModel.appTheme.value)
        viewModel.cycleTheme()
        assertEquals(AppTheme.LIGHT, viewModel.appTheme.value)
        viewModel.cycleTheme()
        assertEquals(AppTheme.DARK, viewModel.appTheme.value)
        viewModel.cycleTheme()
        assertEquals(AppTheme.SYSTEM, viewModel.appTheme.value)
    }

    @Test
    fun `navigation between tracker and history`() = runTest {
        assertEquals(AppScreen.TRACKER, viewModel.currentScreen.value)
        viewModel.navigateToHistory()
        assertEquals(AppScreen.HISTORY, viewModel.currentScreen.value)
        viewModel.navigateToTracker()
        assertEquals(AppScreen.TRACKER, viewModel.currentScreen.value)
    }

    @Test
    fun `end meeting saves record to history and updates state`() = runTest {
        viewModel.startOrResume()
        viewModel.endMeeting()
        assertEquals(false, viewModel.isTracking.value)
        assertEquals(true, viewModel.isEnded.value)
        assertEquals(1, viewModel.meetingHistory.value.size)
        assertEquals(5, viewModel.meetingHistory.value[0].attendees)
    }
}
