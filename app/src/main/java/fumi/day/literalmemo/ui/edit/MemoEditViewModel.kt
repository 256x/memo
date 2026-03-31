package fumi.day.literalmemo.ui.edit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalmemo.data.github.GitHubSyncManager
import fumi.day.literalmemo.data.prefs.UserPreferences
import fumi.day.literalmemo.data.prefs.UserPrefs
import fumi.day.literalmemo.data.repository.MemoRepository
import fumi.day.literalmemo.domain.model.Memo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val userPreferences: UserPreferences,
    private val syncManager: GitHubSyncManager
) : ViewModel() {

    private val fileName: String? = savedStateHandle["fileName"]
    val isNewMemo: Boolean = fileName == null

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private var originalContent: String = ""

    private val _currentFileName = MutableStateFlow(fileName)
    val currentFileName: StateFlow<String?> = _currentFileName.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(!isNewMemo)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    val userPrefs: StateFlow<UserPrefs> = userPreferences.userPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPrefs()
        )

    private var initialContentFromShare: String? = null

    fun setInitialContent(content: String?) {
        if (content != null && _content.value.isEmpty()) {
            _content.value = content
            initialContentFromShare = content
            originalContent = ""
        }
    }

    init {
        if (fileName != null) {
            viewModelScope.launch {
                val memo = memoRepository.observeAll().first().find { it.fileName == fileName }
                memo?.let {
                    _content.value = it.content
                    originalContent = it.content
                }
            }
        }
    }

    fun updateContent(newContent: String) {
        _content.value = newContent
    }

    fun togglePreviewMode() {
        _isPreviewMode.value = !_isPreviewMode.value
    }

    fun save() {
        val content = _content.value

        if (content.isBlank()) return

        if (content == originalContent && !isNewMemo && initialContentFromShare == null) return

        val fileNameToSave = _currentFileName.value ?: generateFileName()

        val memo = Memo(
            fileName = fileNameToSave,
            content = content,
            updatedAt = System.currentTimeMillis()
        )

        runBlocking {
            memoRepository.save(memo)
        }
        _currentFileName.value = fileNameToSave
        originalContent = content
        initialContentFromShare = null
    }

    fun saveAndSync() {
        Log.d("LiteralMemo", "saveAndSync called")
        save()
        syncInBackground()
    }

    fun deleteMemo(onComplete: () -> Unit) {
        Log.d("LiteralMemo", "deleteMemo called")
        GlobalScope.launch(Dispatchers.IO) {
            _currentFileName.value?.let { fileName ->
                Log.d("LiteralMemo", "trashing: $fileName")
                memoRepository.trash(fileName)
            }
            Log.d("LiteralMemo", "starting sync after delete")
            try {
                val prefs = userPreferences.userPrefs.first()
                Log.d("LiteralMemo", "prefs: enabled=${prefs.gitHubEnabled}")
                if (prefs.gitHubEnabled && prefs.gitHubToken.isNotBlank() && prefs.gitHubRepo.isNotBlank()) {
                    Log.d("LiteralMemo", "calling sync after delete...")
                    val result = syncManager.sync(prefs.gitHubToken, prefs.gitHubRepo, prefs.lastSyncedAt)
                    Log.d("LiteralMemo", "sync after delete result: up=${result.uploaded} down=${result.downloaded} errors=${result.errors}")
                    if (result.errors.isEmpty()) {
                        userPreferences.setLastSyncedAt(System.currentTimeMillis())
                    }
                }
            } catch (e: Exception) {
                Log.e("LiteralMemo", "sync error after delete", e)
            }
        }
        onComplete()
    }

    private fun syncInBackground() {
        Log.d("LiteralMemo", "syncInBackground called")
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val prefs = userPreferences.userPrefs.first()
                Log.d("LiteralMemo", "prefs: enabled=${prefs.gitHubEnabled}, token=${prefs.gitHubToken.take(5)}..., repo=${prefs.gitHubRepo}")
                if (prefs.gitHubEnabled && prefs.gitHubToken.isNotBlank() && prefs.gitHubRepo.isNotBlank()) {
                    Log.d("LiteralMemo", "calling sync...")
                    val result = syncManager.sync(prefs.gitHubToken, prefs.gitHubRepo, prefs.lastSyncedAt)
                    Log.d("LiteralMemo", "sync result: up=${result.uploaded} down=${result.downloaded} errors=${result.errors}")
                    if (result.errors.isEmpty()) {
                        userPreferences.setLastSyncedAt(System.currentTimeMillis())
                    }
                }
            } catch (e: Exception) {
                Log.e("LiteralMemo", "sync error", e)
            }
        }
    }

    private fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return "${formatter.format(Date())}.md"
    }
}
