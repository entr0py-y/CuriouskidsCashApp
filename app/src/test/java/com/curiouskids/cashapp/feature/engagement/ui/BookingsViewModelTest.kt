package com.curiouskids.cashapp.feature.engagement.ui

import app.cash.turbine.test
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.ErrorModel
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.engagement.data.EngagementRepository
import com.curiouskids.cashapp.feature.engagement.domain.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingsViewModelTest {

    private val repository = mockk<EngagementRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val dispatchers = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    private lateinit var classUnderTest: BookingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        classUnderTest = BookingsViewModel(repository, dispatchers)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test sold out slot shows error message`() = runTest(testDispatcher) {
        val soldOutSlot = TimeSlotDto(
            id = "slot-1",
            date = "2026-10-12",
            start_time = "14:00",
            end_time = "15:00",
            capacity = 50,
            available_seats = 0 // SOLD OUT!
        )
        val product = ProductDto(id = "prod-1", name = "Ticket", type = "TICKET", price_paise = 50000)

        classUnderTest.selectSlot(soldOutSlot)
        classUnderTest.selectProduct(product)
        classUnderTest.createAndPayBooking("delhi-cp")

        classUnderTest.uiState.test {
            val state = awaitItem()
            assertEquals("Selected time slot is sold out. Please select another slot.", state.errorMessage)
        }
        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test missing waiver error sets isWaiverRequired flag`() = runTest(testDispatcher) {
        val slot = TimeSlotDto(id = "slot-1", date = "2026-10-12", start_time = "14:00", end_time = "15:00", capacity = 50, available_seats = 10)
        val product = ProductDto(id = "prod-1", name = "Ticket", type = "TICKET", price_paise = 50000)
        val mockBooking = BookingDto(id = "b-1", total_paise = 50000, status = "RESERVED", expires_at = "", created_at = "")

        coEvery { repository.createBooking(any()) } returns Result.Success(mockBooking)
        coEvery { repository.payBooking("b-1") } returns Result.Error(ErrorModel("WAIVER_REQUIRED", "WAIVER_REQUIRED: Signed waiver missing"))

        classUnderTest.selectSlot(slot)
        classUnderTest.selectProduct(product)
        classUnderTest.createAndPayBooking("delhi-cp")

        classUnderTest.uiState.test {
            var state = awaitItem()
            while (!state.isWaiverRequired) {
                state = awaitItem()
            }
            assertTrue(state.isWaiverRequired)
        }
        coroutineContext.job.cancelChildren()
    }
}
