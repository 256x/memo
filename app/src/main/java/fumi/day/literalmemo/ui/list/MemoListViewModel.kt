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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoListViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    userPreferences: UserPreferences,
    private val syncManager: GitHubSyncManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val memos: StateFlow<List<Memo>> = _searchQuery
        .flatMapLatest { query ->
            memoRepository.observeAll().map { memos ->
                if (query.isBlank()) memos
                else memos.filter { it.content.contains(query, ignoreCase = true) }
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

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val syncError: StateFlow<String?> = syncManager.syncError

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
        syncManager.launchSync()
    }

    fun sync() {
        syncManager.launchSync()
    }
}
