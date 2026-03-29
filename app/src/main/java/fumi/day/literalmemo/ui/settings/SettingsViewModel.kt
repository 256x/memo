package fumi.day.literalmemo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalmemo.data.github.GitHubSyncManager
import fumi.day.literalmemo.data.github.SyncResult
import fumi.day.literalmemo.data.prefs.AppFont
import fumi.day.literalmemo.data.prefs.UserPreferences
import fumi.day.literalmemo.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val syncManager: GitHubSyncManager
) : ViewModel() {

    val userPrefs: StateFlow<UserPrefs> = userPreferences.userPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPrefs()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    fun setFont(font: AppFont) {
        viewModelScope.launch {
            userPreferences.setFont(font)
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch {
            userPreferences.setFontSize(size)
        }
    }

    fun setBackgroundColor(hex: String) {
        viewModelScope.launch {
            userPreferences.setBackgroundColor(hex)
        }
    }

    fun setTextColor(hex: String) {
        viewModelScope.launch {
            userPreferences.setTextColor(hex)
        }
    }

    fun setAccentColor(hex: String) {
        viewModelScope.launch {
            userPreferences.setAccentColor(hex)
        }
    }

    fun setFabOnLeft(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setFabOnLeft(enabled)
        }
    }

    fun saveGitHubConfig(token: String, repo: String) {
        viewModelScope.launch {
            userPreferences.setGitHubConfig(
                enabled = token.isNotBlank() && repo.isNotBlank(),
                token = token,
                repo = repo
            )
        }
    }

    fun disconnectGitHub() {
        viewModelScope.launch {
            userPreferences.clearGitHubConfig()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncResult.value = null
            try {
                val prefs = userPrefs.value
                if (prefs.gitHubEnabled && prefs.gitHubToken.isNotBlank() && prefs.gitHubRepo.isNotBlank()) {
                    val result = syncManager.sync(prefs.gitHubToken, prefs.gitHubRepo)
                    _syncResult.value = result
                    if (result.errors.isEmpty()) {
                        userPreferences.setLastSyncedAt(System.currentTimeMillis())
                    }
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
