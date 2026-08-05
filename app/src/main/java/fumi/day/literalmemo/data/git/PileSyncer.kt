package fumi.day.literalmemo.data.git

import fumi.day.literalmemo.di.PileDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val errors: List<String> = emptyList(),
    val remoteShas: Map<String, String> = emptyMap()
)

/**
 * Reconciles the local pile directory with a remote `pile/` + `trash/` pair.
 *
 * Forge-agnostic and free of preferences or UI state, so the conflict rules can be exercised
 * against a fake [GitForgeApi] in unit tests.
 */
@Singleton
class PileSyncer @Inject constructor(
    @param:PileDir private val pileDir: File
) {

    suspend fun sync(
        api: GitForgeApi,
        token: String,
        repo: String,
        lastSyncedAt: Long?,
        lastSyncedShas: Map<String, String> = emptyMap()
    ): SyncResult = withContext(Dispatchers.IO) {
        var uploaded = 0
        var downloaded = 0
        val errors = mutableListOf<String>()
        val newRemoteShas = mutableMapOf<String, String>()

        try {
            val localPileFiles = pileDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".md") }
                ?.associateBy { it.name }
                ?: emptyMap()

            val remotePileResult = api.listPileFiles(token, repo)
            if (remotePileResult.isFailure) {
                errors.add("Failed to connect: ${describe(remotePileResult.exceptionOrNull())}")
                return@withContext SyncResult(errors = errors)
            }
            val remotePileFiles = remotePileResult.getOrThrow().associateBy { it.path.substringAfterLast("/") }

            val remoteTrashFiles = (api.listTrashFiles(token, repo).getOrNull() ?: emptyList())
                .associateBy { it.path.substringAfterLast("/") }

            val allFileNames = (localPileFiles.keys + remotePileFiles.keys + remoteTrashFiles.keys).toSet()

            for (fileName in allFileNames) {
                val inLocalPile = fileName in localPileFiles
                val inRemotePile = fileName in remotePileFiles
                val inRemoteTrash = fileName in remoteTrashFiles
                val knownSha = lastSyncedShas[fileName]

                try {
                    when {
                        inRemoteTrash && inLocalPile -> {
                            // Another device trashed it → delete locally
                            localPileFiles[fileName]!!.delete()
                        }

                        inLocalPile && !inRemotePile && !inRemoteTrash -> {
                            if (knownSha != null) {
                                // Was on remote before, now gone → delete locally
                                localPileFiles[fileName]!!.delete()
                            } else {
                                // New local file → upload
                                val content = localPileFiles[fileName]!!.readText(Charsets.UTF_8)
                                val result = api.putFile(token, repo, "pile/$fileName", content, message = "Add $fileName")
                                if (result.isSuccess) {
                                    uploaded++
                                    result.getOrNull()?.sha?.let { newRemoteShas[fileName] = it }
                                } else {
                                    errors.add("$fileName: upload failed: ${describe(result.exceptionOrNull())}")
                                }
                            }
                        }

                        !inLocalPile && inRemotePile -> {
                            if (knownSha != null) {
                                // Deleted locally → move to remote trash
                                val remoteFile = remotePileFiles[fileName]!!
                                val contentResult = api.getFile(token, repo, remoteFile.path)
                                val content = contentResult.getOrNull()?.content
                                if (content == null) {
                                    errors.add("$fileName: trash failed: ${describe(contentResult.exceptionOrNull())}")
                                } else {
                                    val trashResult = api.moveToTrash(token, repo, fileName, remoteFile.sha, content)
                                    if (trashResult.isFailure) {
                                        errors.add("$fileName: trash failed: ${describe(trashResult.exceptionOrNull())}")
                                    }
                                }
                            } else {
                                // New remote file → download
                                val remoteFile = remotePileFiles[fileName]!!
                                val contentResult = api.getFile(token, repo, remoteFile.path)
                                if (contentResult.isSuccess) {
                                    File(pileDir, fileName).writeText(contentResult.getOrThrow().content, Charsets.UTF_8)
                                    newRemoteShas[fileName] = remoteFile.sha
                                    downloaded++
                                } else {
                                    // SHA not tracked → retried on next sync
                                    errors.add("$fileName: download failed: ${describe(contentResult.exceptionOrNull())}")
                                }
                            }
                        }

                        inLocalPile && inRemotePile -> {
                            val localFile = localPileFiles[fileName]!!
                            val remoteFile = remotePileFiles[fileName]!!
                            val localContent = localFile.readText(Charsets.UTF_8)

                            val remoteContentResult = api.getFile(token, repo, remoteFile.path)
                            if (remoteContentResult.isSuccess) {
                                val remoteContent = remoteContentResult.getOrThrow().content
                                if (localContent != remoteContent) {
                                    val localChanged = localFile.lastModified() > (lastSyncedAt ?: 0L)
                                    val remoteChanged = knownSha != null && knownSha != remoteFile.sha

                                    when {
                                        localChanged && remoteChanged -> {
                                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                            val conflictName = fileName.removeSuffix(".md") + "_conflict_$timestamp.md"
                                            File(pileDir, conflictName).writeText(localContent, Charsets.UTF_8)
                                            localFile.writeText(remoteContent, Charsets.UTF_8)
                                            newRemoteShas[fileName] = remoteFile.sha
                                            downloaded++
                                        }
                                        localChanged -> {
                                            val result = api.putFile(token, repo, "pile/$fileName", localContent, sha = remoteFile.sha, message = "Update $fileName")
                                            if (result.isSuccess) {
                                                uploaded++
                                                newRemoteShas[fileName] = result.getOrNull()?.sha ?: remoteFile.sha
                                            } else {
                                                newRemoteShas[fileName] = remoteFile.sha
                                                errors.add("$fileName: upload failed: ${describe(result.exceptionOrNull())}")
                                            }
                                        }
                                        else -> {
                                            localFile.writeText(remoteContent, Charsets.UTF_8)
                                            newRemoteShas[fileName] = remoteFile.sha
                                            downloaded++
                                        }
                                    }
                                } else {
                                    newRemoteShas[fileName] = remoteFile.sha
                                }
                            } else {
                                knownSha?.let { newRemoteShas[fileName] = it }
                                errors.add("$fileName: download failed: ${describe(remoteContentResult.exceptionOrNull())}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    errors.add("$fileName: ${describe(e)}")
                }
            }
        } catch (e: Exception) {
            errors.add("Sync failed: ${describe(e)}")
        }

        SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            errors = errors,
            remoteShas = newRemoteShas
        )
    }

    fun clearLocalData() {
        pileDir.listFiles()?.forEach { it.delete() }
    }

    /** Keeps the cause visible in the UI without letting a long API response body take it over. */
    private fun describe(error: Throwable?): String {
        val message = error?.message?.trim()?.takeIf { it.isNotEmpty() }
            ?: error?.javaClass?.simpleName
            ?: return "unknown error"
        return if (message.length > MAX_ERROR_LENGTH) message.take(MAX_ERROR_LENGTH) + "…" else message
    }

    private companion object {
        const val MAX_ERROR_LENGTH = 160
    }
}
