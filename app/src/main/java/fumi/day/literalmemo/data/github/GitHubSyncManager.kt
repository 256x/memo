package fumi.day.literalmemo.data.github

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalmemo.data.prefs.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val trashed: Int = 0,
    val errors: List<String> = emptyList()
)

@Singleton
class GitHubSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gitHubRepository: GitHubRepository,
    private val userPreferences: UserPreferences
) {
    private val pileDir: File by lazy {
        File(context.filesDir, "pile").also { it.mkdirs() }
    }

    private val trashDir: File by lazy {
        File(context.filesDir, "trash").also { it.mkdirs() }
    }

    suspend fun syncIfEnabled(): SyncResult? = withContext(Dispatchers.IO) {
        val prefs = userPreferences.userPrefs.first()
        if (!prefs.gitHubEnabled || prefs.gitHubToken.isBlank() || prefs.gitHubRepo.isBlank()) {
            return@withContext null
        }
        val result = sync(prefs.gitHubToken, prefs.gitHubRepo, prefs.lastSyncedAt)
        if (result.errors.isEmpty()) {
            userPreferences.setLastSyncedAt(System.currentTimeMillis())
        }
        result
    }

    suspend fun sync(token: String, repo: String, lastSyncedAt: Long?): SyncResult = withContext(Dispatchers.IO) {
        var uploaded = 0
        var downloaded = 0
        var trashed = 0
        val errors = mutableListOf<String>()

        try {
            val localPileFiles = pileDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".md") }
                ?.associateBy { it.name }
                ?: emptyMap()

            val localTrashFiles = trashDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".md") }
                ?.associateBy { it.name }
                ?: emptyMap()

            val remotePileResult = gitHubRepository.listPileFiles(token, repo)
            if (remotePileResult.isFailure) {
                errors.add("Failed to connect")
                return@withContext SyncResult(errors = errors)
            }
            val remotePileFiles = remotePileResult.getOrThrow().associateBy { it.path.substringAfterLast("/") }

            val remoteTrashResult = gitHubRepository.listTrashFiles(token, repo)
            val remoteTrashFiles = (remoteTrashResult.getOrNull() ?: emptyList()).associateBy { it.path.substringAfterLast("/") }

            val allFileNames = (localPileFiles.keys + localTrashFiles.keys + remotePileFiles.keys + remoteTrashFiles.keys).toSet()

            for (fileName in allFileNames) {
                val inLocalPile = fileName in localPileFiles
                val inLocalTrash = fileName in localTrashFiles
                val inRemotePile = fileName in remotePileFiles
                val inRemoteTrash = fileName in remoteTrashFiles

                try {
                    when {
                        inRemoteTrash && inLocalPile -> {
                            val localFile = localPileFiles[fileName]!!
                            val trashedFile = File(trashDir, fileName)
                            localFile.renameTo(trashedFile)
                            trashed++
                        }

                        inLocalPile && !inRemotePile && !inRemoteTrash -> {
                            val localFile = localPileFiles[fileName]!!
                            val content = localFile.readText(Charsets.UTF_8)
                            val result = gitHubRepository.putFile(
                                token, repo, "pile/$fileName", content,
                                message = "Add $fileName"
                            )
                            if (result.isSuccess) uploaded++
                        }

                        inLocalTrash && !inRemoteTrash -> {
                            val localFile = localTrashFiles[fileName]!!
                            val content = localFile.readText(Charsets.UTF_8)
                            if (inRemotePile) {
                                val remoteFile = remotePileFiles[fileName]!!
                                val moveResult = gitHubRepository.moveToTrash(token, repo, fileName, remoteFile.sha, content)
                                if (moveResult.isSuccess) uploaded++
                            } else {
                                val result = gitHubRepository.putFile(
                                    token, repo, "trash/$fileName", content,
                                    message = "Trash: $fileName"
                                )
                                if (result.isSuccess) uploaded++
                            }
                        }

                        inRemotePile && !inLocalPile && !inLocalTrash -> {
                            val remoteFile = remotePileFiles[fileName]!!
                            val contentResult = gitHubRepository.getFile(token, repo, remoteFile.path)
                            if (contentResult.isSuccess) {
                                val content = contentResult.getOrThrow().content
                                File(pileDir, fileName).writeText(content, Charsets.UTF_8)
                                downloaded++
                            }
                        }

                        inRemoteTrash && !inLocalPile && !inLocalTrash -> {
                            val remoteFile = remoteTrashFiles[fileName]!!
                            val contentResult = gitHubRepository.getFile(token, repo, remoteFile.path)
                            if (contentResult.isSuccess) {
                                val content = contentResult.getOrThrow().content
                                File(trashDir, fileName).writeText(content, Charsets.UTF_8)
                                downloaded++
                            }
                        }

                        inLocalPile && inRemotePile -> {
                            val localFile = localPileFiles[fileName]!!
                            val remoteFile = remotePileFiles[fileName]!!
                            val localContent = localFile.readText(Charsets.UTF_8)

                            val remoteContentResult = gitHubRepository.getFile(token, repo, remoteFile.path)
                            if (remoteContentResult.isSuccess) {
                                val remoteContent = remoteContentResult.getOrThrow().content
                                if (localContent != remoteContent) {
                                    val localModified = localFile.lastModified()
                                    val syncTime = lastSyncedAt ?: 0L
                                    if (localModified > syncTime) {
                                        val result = gitHubRepository.putFile(
                                            token, repo, "pile/$fileName", localContent,
                                            sha = remoteFile.sha,
                                            message = "Update $fileName"
                                        )
                                        if (result.isSuccess) uploaded++
                                    } else {
                                        localFile.writeText(remoteContent, Charsets.UTF_8)
                                        downloaded++
                                    }
                                }
                            }
                        }

                        inLocalTrash && inRemoteTrash -> {
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            errors.add("Sync failed")
        }

        SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            trashed = trashed,
            errors = errors
        )
    }
}
