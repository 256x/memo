package fumi.day.literalmemo.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    userPreferences: UserPreferences
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
        
        // 空の場合は保存しない
        if (content.isBlank()) return
        
        // 内容が変更されていない場合は保存しない
        if (content == originalContent && !isNewMemo) return

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
    }

    fun deleteMemo(onComplete: () -> Unit) {
        viewModelScope.launch {
            _currentFileName.value?.let { fileName ->
                memoRepository.trash(fileName)
            }
            onComplete()
        }
    }

    private fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "${formatter.format(Date())}.md"
    }
}
