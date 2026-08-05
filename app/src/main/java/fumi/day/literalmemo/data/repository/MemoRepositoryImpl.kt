package fumi.day.literalmemo.data.repository

import android.os.FileObserver
import fumi.day.literalmemo.di.PileDir
import fumi.day.literalmemo.domain.model.Memo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepositoryImpl @Inject constructor(
    @param:PileDir private val pileDir: File
) : MemoRepository {

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

    private fun readAll(): List<Memo> = pileDir.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".md") }
        ?.mapNotNull { it.toMemo() }
        ?.sortedByDescending { it.updatedAt }
        ?: emptyList()

    /**
     * Emits once per batch of changes to the pile directory. A sync writes many files
     * back to back, so events are debounced instead of triggering a re-read each time.
     */
    private fun pileChanges(): Flow<Unit> = callbackFlow {
        @Suppress("DEPRECATION") // FileObserver(File, Int) requires API 29, minSdk is 26
        val observer = object : FileObserver(pileDir.absolutePath, WATCH_MASK) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null && !path.endsWith(".md")) return
                trySend(Unit)
            }
        }
        observer.startWatching()
        // Covers writes that landed between the initial read and startWatching().
        trySend(Unit)
        awaitClose { observer.stopWatching() }
    }.buffer(1, BufferOverflow.DROP_OLDEST)

    @OptIn(FlowPreview::class)
    override fun observeAll(): Flow<List<Memo>> = pileChanges()
        .debounce(DEBOUNCE_MS)
        .map { readAll() }
        .onStart { emit(readAll()) }
        .flowOn(Dispatchers.IO)

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

    private companion object {
        const val WATCH_MASK = FileObserver.CREATE or
            FileObserver.DELETE or
            FileObserver.MOVED_TO or
            FileObserver.MOVED_FROM or
            FileObserver.CLOSE_WRITE
        const val DEBOUNCE_MS = 200L
    }
}
