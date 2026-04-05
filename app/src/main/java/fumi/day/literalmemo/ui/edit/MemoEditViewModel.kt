package fumi.day.literalmemo.ui.edit

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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    userPreferences: UserPreferences,
    private val syncManager: GitHubSyncManager
) : ViewModel() {

    private val fileName: String? = savedStateHandle["fileName"]
    private val isNewMemo: Boolean = fileName == null

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

    fun setInitialContent(content: String?) {
        if (content != null && _content.value.isEmpty()) {
            _content.value = content
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
        if (content == originalContent && !isNewMemo) return

        val fileNameToSave = _currentFileName.value ?: generateFileName()
        val memo = Memo(
            fileName = fileNameToSave,
            content = content,
            updatedAt = System.currentTimeMillis()
        )

        _currentFileName.value = fileNameToSave
        originalContent = content

        viewModelScope.launch(Dispatchers.IO) {
            memoRepository.save(memo)
        }
    }

    fun saveAndSync(onComplete: () -> Unit) {
        val content = _content.value
        if (content.isBlank()) {
            onComplete()
            return
        }
        if (content == originalContent && !isNewMemo) {
            onComplete()
            return
        }

        val fileNameToSave = _currentFileName.value ?: generateFileName()
        val memo = Memo(
            fileName = fileNameToSave,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
        _currentFileName.value = fileNameToSave
        originalContent = content

        viewModelScope.launch(Dispatchers.IO) {
            memoRepository.save(memo)
            syncManager.launchSync()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deleteMemo(onComplete: () -> Unit) {
        val fileNameToDelete = _currentFileName.value
        viewModelScope.launch(Dispatchers.IO) {
            fileNameToDelete?.let { memoRepository.trash(it) }
            syncManager.launchSync()
            withContext(Dispatchers.Main) {
                onComplete()
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
