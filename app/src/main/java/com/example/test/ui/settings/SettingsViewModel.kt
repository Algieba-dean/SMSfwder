package com.example.test.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.repository.EmailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val emailRepository: EmailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadEmailConfigStatus()
    }

    private fun loadEmailConfigStatus() {
        viewModelScope.launch {
            try {
                val config = emailRepository.getDefaultConfig()
                _uiState.value = _uiState.value.copy(
                    isEmailConfigured = config != null,
                    senderEmail = config?.senderEmail ?: "Not configured",
                    receiverEmail = config?.receiverEmail ?: "Not configured",
                    smtpHost = config?.smtpHost ?: "Not configured"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isEmailConfigured = false,
                    senderEmail = "Error loading",
                    receiverEmail = "Error loading",
                    smtpHost = "Error loading"
                )
            }
        }
    }

    fun refreshEmailConfig() {
        loadEmailConfigStatus()
    }
}

data class SettingsUiState(
    val isEmailConfigured: Boolean = false,
    val senderEmail: String = "Not configured",
    val receiverEmail: String = "Not configured",
    val smtpHost: String = "Not configured"
) 