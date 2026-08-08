package fumi.day.literalmemo.data.repository

import android.os.FileObserver
import fumi.day.literalmemo.di.PileDir
import fumi.day.literalmemo.domain.model.Memo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepositoryImpl @Inject constructor(
    @param:PileDir private val pileDir: File
) : MemoRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
     * Refreshes the pile after a write this app made, so an in-app edit or delete never depends
     * on an inotify event arriving.
     */
    private val localChanges = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Emits once per batch of changes to the pile directory. A sync writes many files
     * back to back, so events are debounced instead of triggering a re-read each time.
     *
     * Shared, so the whole app only ever holds one [FileObserver] on the pile: Android keys
     * inotify watches by path, so a second observer takes over the watch and then tears it down
     * when it stops, leaving every earlier observer deaf.
     */
    private val pileChanges: Flow<Unit> = callbackFlow {
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
    }
        .buffer(1, BufferOverflow.DROP_OLDEST)
        .shareIn(scope, SharingStarted.WhileSubscribed())

    @OptIn(FlowPreview::class)
    override fun observeAll(): Flow<List<Memo>> = merge(pileChanges, localChanges)
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
        localChanges.tryEmit(Unit)
        Unit
    }

    override suspend fun delete(fileName: String) = withContext(Dispatchers.IO) {
        File(pileDir, fileName).delete()
        localChanges.tryEmit(Unit)
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
