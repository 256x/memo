package fumi.day.literalmemo.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalmemo.domain.model.Memo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : MemoRepository {

    private val pileDir: File by lazy {
        File(context.filesDir, "pile").also { it.mkdirs() }
    }

    private val trashDir: File by lazy {
        File(context.filesDir, "trash").also { it.mkdirs() }
    }

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun generateFileName(): String {
        return "${dateFormat.format(Date())}.md"
    }

    private fun File.toMemo(): Memo? {
        if (!exists() || !isFile || !name.endsWith(".md")) return null
        return try {
            Memo(
                fileName = name,
                content = readText(Charsets.UTF_8),
                updatedAt = lastModified()
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun observeAll(): Flow<List<Memo>> = flow {
        while (true) {
            val memos = pileDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".md") }
                ?.mapNotNull { it.toMemo() }
                ?.sortedByDescending { it.updatedAt }
                ?: emptyList()
            emit(memos)
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getByFileName(fileName: String): Memo? = withContext(Dispatchers.IO) {
        File(pileDir, fileName).toMemo()
    }

    override suspend fun save(memo: Memo) = withContext(Dispatchers.IO) {
        val fileName = memo.fileName.ifEmpty { generateFileName() }
        val file = File(pileDir, fileName)
        file.writeText(memo.content, Charsets.UTF_8)
    }

    override suspend fun trash(fileName: String) = withContext(Dispatchers.IO) {
        val file = File(pileDir, fileName)
        if (file.exists()) {
            val trashedFile = File(trashDir, fileName)
            file.renameTo(trashedFile)
        }
    }

    override fun search(query: String): Flow<List<Memo>> {
        return observeAll().map { memos ->
            if (query.isBlank()) {
                memos
            } else {
                memos.filter { memo ->
                    memo.content.contains(query, ignoreCase = true)
                }
            }
        }
    }
}
