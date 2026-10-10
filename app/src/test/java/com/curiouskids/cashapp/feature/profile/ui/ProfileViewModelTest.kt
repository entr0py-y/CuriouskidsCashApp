package com.curiouskids.cashapp.feature.profile.ui

import app.cash.turbine.test
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.auth.data.AuthRepository
import com.curiouskids.cashapp.feature.profile.data.ProfileRepository
import com.curiouskids.cashapp.feature.profile.domain.*
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val repository = mockk<ProfileRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val dispatchers = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    private lateinit var classUnderTest: ProfileViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getBranches() } returns Result.Success(emptyList())
        coEvery { repository.getFamilyMembers() } returns Result.Success(emptyList())
        coEvery { repository.getProfile() } returns Result.Success(ProfileDto(full_name = "Adult Test", completion_percentage = 80))

        classUnderTest = ProfileViewModel(repository, authRepository, dispatchers)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test profile loading sets state`() = runTest(testDispatcher) {
        classUnderTest.uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.profile.full_name.isBlank()) {
                state = awaitItem()
            }
            assertEquals("Adult Test", state.profile.full_name)
            assertEquals(80, state.profile.completion_percentage)
        }
        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test minor update without guardian requires guardian info`() = runTest(testDispatcher) {
        val minorProfile = ProfileDto(
            full_name = "Minor User",
            date_of_birth = "2015-01-01", // Minor
            guardian_name = "", // Missing guardian
            parental_consent_given = false
        )

        classUnderTest.updateProfile(minorProfile)

        classUnderTest.uiState.test {
            var state = awaitItem()
            while (state.errorMessage == null) {
                state = awaitItem()
            }
            assertEquals("Accounts for users under 18 require guardian name, phone, and relationship.", state.errorMessage)
        }
        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test data export returns json string`() = runTest(testDispatcher) {
        val mockDataExport = DataExportDto(
            account = mapOf("id" to "123"),
            profile = ProfileDto(full_name = "Export User"),
            family_members = emptyList(),
            consents = emptyList()
        )
        coEvery { repository.getDataExport() } returns Result.Success(mockDataExport)

        classUnderTest.exportData()

        classUnderTest.uiState.test {
            var state = awaitItem()
            while (state.exportDataJson == null) {
                state = awaitItem()
            }
            assertNotNull(state.exportDataJson)
            assertTrue(state.exportDataJson!!.contains("Export User"))
        }
        coroutineContext.job.cancelChildren()
    }
}
