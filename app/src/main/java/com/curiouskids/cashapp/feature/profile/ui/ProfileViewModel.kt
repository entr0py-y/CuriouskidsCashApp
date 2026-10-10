package com.curiouskids.cashapp.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.auth.data.AuthRepository
import com.curiouskids.cashapp.feature.profile.data.ProfileRepository
import com.curiouskids.cashapp.feature.profile.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

data class ProfileUiState(
    val profile: ProfileDto = ProfileDto(),
    val branches: List<BranchDto> = emptyList(),
    val familyMembers: List<FamilyMemberDto> = emptyList(),
    val checklist: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isMinor: Boolean = false,
    val exportDataJson: String? = null,
    val isSuccessMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Fetch Branches
            when (val bRes = repository.getBranches()) {
                is Result.Success -> _uiState.update { it.copy(branches = bRes.data) }
                else -> {}
            }

            // Fetch Profile
            when (val pRes = repository.getProfile()) {
                is Result.Success -> {
                    val p = pRes.data
                    val isMinorUser = p.date_of_birth?.let { calculateAge(it) < 18 } ?: false
                    _uiState.update { 
                        it.copy(
                            profile = p,
                            isMinor = isMinorUser,
                            checklist = computeChecklist(p, isMinorUser)
                        ) 
                    }
                }
                is Result.Error -> _uiState.update { it.copy(errorMessage = pRes.errorModel.message) }
                else -> {}
            }

            // Fetch Family
            when (val fRes = repository.getFamilyMembers()) {
                is Result.Success -> _uiState.update { it.copy(familyMembers = fRes.data) }
                else -> {}
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateProfile(updated: ProfileDto) {
        // Local Validation for Minors
        val age = updated.date_of_birth?.let { calculateAge(it) } ?: 100
        val isMinorUser = age < 18

        if (isMinorUser) {
            if (updated.guardian_name.isBlank() || updated.guardian_phone.isBlank() || updated.guardian_relationship.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Accounts for users under 18 require guardian name, phone, and relationship.") }
                return
            }
            if (!updated.parental_consent_given) {
                _uiState.update { it.copy(errorMessage = "Parental consent must be explicitly granted for minor accounts.") }
                return
            }
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val res = repository.updateProfile(updated)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            profile = res.data,
                            isMinor = isMinorUser,
                            checklist = computeChecklist(res.data, isMinorUser),
                            isSuccessMessage = "Profile updated successfully!"
                        ) 
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.errorModel.message) }
                else -> {}
            }
        }
    }

    fun addFamilyMember(name: String, dob: String) {
        if (name.isBlank() || dob.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name and date of birth are required for family members.") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = repository.addFamilyMember(FamilyMemberDto(name = name, date_of_birth = dob))) {
                is Result.Success -> {
                    loadData()
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.errorModel.message) }
                else -> {}
            }
        }
    }

    fun deleteFamilyMember(id: String) {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = repository.deleteFamilyMember(id)) {
                is Result.Success -> loadData()
                is Result.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.errorModel.message) }
                else -> {}
            }
        }
    }

    fun exportData() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = repository.getDataExport()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, exportDataJson = res.data.toString()) }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.errorModel.message) }
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch(dispatchers.main) {
            authRepository.logout()
        }
    }

    private fun calculateAge(dobIso: String): Int {
        return try {
            val dob = LocalDate.parse(dobIso)
            Period.between(dob, LocalDate.now()).years
        } catch (e: Exception) {
            20
        }
    }

    private fun computeChecklist(p: ProfileDto, isMinor: Boolean): List<String> {
        val list = mutableListOf<String>()
        if (p.full_name.isBlank()) list.add("Add full name")
        if (p.date_of_birth == null) list.add("Add date of birth")
        if (p.email.isBlank()) list.add("Add email address")
        if (p.city.isBlank()) list.add("Add city")
        if (p.preferred_branch == null) list.add("Select preferred branch")
        if (p.emergency_contact_name.isBlank() || p.emergency_contact_phone.isBlank()) list.add("Add emergency contact")
        if (isMinor) {
            if (p.guardian_name.isBlank()) list.add("Add guardian details")
        }
        return list
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null, isSuccessMessage = null, exportDataJson = null) }
    }
}
