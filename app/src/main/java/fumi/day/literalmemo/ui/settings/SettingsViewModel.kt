package fumi.day.literalmemo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalmemo.data.github.GitHubRepository
import fumi.day.literalmemo.data.github.GitHubSyncManager
import fumi.day.literalmemo.data.github.SyncResult
import fumi.day.literalmemo.data.prefs.AppFont
import fumi.day.literalmemo.data.prefs.UserPreferences
import fumi.day.literalmemo.data.prefs.UserPrefs
import fumi.day.literalmemo.data.repository.MemoRepository
import fumi.day.literalmemo.domain.model.Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val memoRepository: MemoRepository,
    private val syncManager: GitHubSyncManager,
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    val userPrefs: StateFlow<UserPrefs> = userPreferences.userPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPrefs()
        )

    private val _trashedMemos = MutableStateFlow<List<Memo>>(emptyList())
    val trashedMemos: StateFlow<List<Memo>> = _trashedMemos.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    private val _isDeletingFromRemote = MutableStateFlow(false)
    val isDeletingFromRemote: StateFlow<Boolean> = _isDeletingFromRemote.asStateFlow()

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

    fun loadTrashedMemos() {
        viewModelScope.launch {
            _trashedMemos.value = memoRepository.observeTrash().first()
        }
    }

    fun restoreMemo(fileName: String) {
        viewModelScope.launch {
            memoRepository.restore(fileName)
            loadTrashedMemos()
            
            // Sync to restore on remote as well
            val prefs = userPrefs.value
            if (prefs.gitHubEnabled) {
                syncNow()
            }
        }
    }

    fun deletePermanently(fileName: String) {
        viewModelScope.launch {
            val prefs = userPrefs.value
            
            // Delete from local
            memoRepository.deletePermanently(fileName)
            
            // Delete from remote if GitHub is enabled
            if (prefs.gitHubEnabled && prefs.gitHubToken.isNotBlank() && prefs.gitHubRepo.isNotBlank()) {
                _isDeletingFromRemote.value = true
                try {
                    // fileName is like ".20240101-120000.md", remote path is "pile/.20240101-120000.md"
                    val remotePath = "pile/$fileName"
                    val trashedFiles = gitHubRepository.listTrashedFiles(prefs.gitHubToken, prefs.gitHubRepo)
                    trashedFiles.getOrNull()?.find { it.path == remotePath }?.let { file ->
                        gitHubRepository.deleteFile(
                            token = prefs.gitHubToken,
                            repo = prefs.gitHubRepo,
                            path = file.path,
                            sha = file.sha,
                            message = "Permanently delete $fileName"
                        )
                    }
                } finally {
                    _isDeletingFromRemote.value = false
                }
            }
            
            loadTrashedMemos()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val prefs = userPrefs.value
            val memosToDelete = _trashedMemos.value.toList()
            
            if (prefs.gitHubEnabled && prefs.gitHubToken.isNotBlank() && prefs.gitHubRepo.isNotBlank()) {
                _isDeletingFromRemote.value = true
                try {
                    // Get remote trashed files
                    val trashedFiles = gitHubRepository.listTrashedFiles(prefs.gitHubToken, prefs.gitHubRepo)
                        .getOrNull() ?: emptyList()
                    
                    for (memo in memosToDelete) {
                        // Delete from local
                        memoRepository.deletePermanently(memo.fileName)
                        
                        // Delete from remote
                        val remotePath = "pile/${memo.fileName}"
                        trashedFiles.find { it.path == remotePath }?.let { file ->
                            gitHubRepository.deleteFile(
                                token = prefs.gitHubToken,
                                repo = prefs.gitHubRepo,
                                path = file.path,
                                sha = file.sha,
                                message = "Permanently delete ${memo.fileName}"
                            )
                        }
                    }
                } finally {
                    _isDeletingFromRemote.value = false
                }
            } else {
                // Just delete locally
                for (memo in memosToDelete) {
                    memoRepository.deletePermanently(memo.fileName)
                }
            }
            
            loadTrashedMemos()
        }
    }
}
