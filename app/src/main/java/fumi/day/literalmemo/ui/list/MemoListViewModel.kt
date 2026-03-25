package fumi.day.literalmemo.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalmemo.data.github.GitHubSyncManager
import fumi.day.literalmemo.data.prefs.UserPreferences
import fumi.day.literalmemo.data.prefs.UserPrefs
import fumi.day.literalmemo.data.repository.MemoRepository
import fumi.day.literalmemo.domain.model.Memo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoListViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val userPreferences: UserPreferences,
    private val syncManager: GitHubSyncManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val memos: StateFlow<List<Memo>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                memoRepository.observeAll()
            } else {
                memoRepository.search(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userPrefs: StateFlow<UserPrefs> = userPreferences.userPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPrefs()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()


    fun executeSearch(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun trashMemo(fileName: String) {
        viewModelScope.launch {
            memoRepository.trash(fileName)
        }
    }

    fun sync() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val prefs = userPrefs.value
                if (prefs.gitHubEnabled && prefs.gitHubToken.isNotBlank() && prefs.gitHubRepo.isNotBlank()) {
                    val result = syncManager.sync(prefs.gitHubToken, prefs.gitHubRepo)
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
