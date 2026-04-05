package fumi.day.literalmemo.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalmemo.domain.model.Memo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : MemoRepository {

    private val pileDir: File by lazy {
        File(context.filesDir, "pile").also { it.mkdirs() }
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
        val file = File(pileDir, memo.fileName)
        file.writeText(memo.content, Charsets.UTF_8)
    }

    override suspend fun delete(fileName: String) = withContext(Dispatchers.IO) {
        File(pileDir, fileName).delete()
        Unit
    }
}
