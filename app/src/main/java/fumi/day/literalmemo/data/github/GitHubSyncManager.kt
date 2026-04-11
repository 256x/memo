package fumi.day.literalmemo.data.github

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalmemo.data.git.GitForge
import fumi.day.literalmemo.data.git.GitForgeApi
import fumi.day.literalmemo.data.git.GiteaRepository
import fumi.day.literalmemo.data.prefs.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

@Singleton
class GitHubSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gitHubRepository: GitHubRepository,
    private val userPreferences: UserPreferences
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun launchSync() {
        if (_isSyncing.value) return
        appScope.launch { syncAndAwait() }
    }

    suspend fun syncAndAwait(): SyncResult? {
        if (_isSyncing.value) return null
        _isSyncing.value = true
        _syncError.value = null
        return try {
            val result = syncIfEnabled()
            if (result != null && result.errors.isNotEmpty()) {
                _syncError.value = result.errors.first()
            }
            result
        } finally {
            _isSyncing.value = false
        }
    }

    private val pileDir: File by lazy {
        File(context.filesDir, "pile").also { it.mkdirs() }
    }

    private fun resolveApi(forge: GitForge, host: String): GitForgeApi =
        when (forge) {
            GitForge.GITHUB -> gitHubRepository
            GitForge.GITEA -> GiteaRepository(host)
        }

    suspend fun moveToRemoteTrash(fileName: String) {
        val prefs = userPreferences.userPrefs.first()
        if (!prefs.gitHubEnabled || prefs.gitHubToken.isBlank() || prefs.gitHubRepo.isBlank()) return
        if (prefs.gitForge == GitForge.GITEA && prefs.gitHost.isBlank()) return

        val api = resolveApi(prefs.gitForge, prefs.gitHost)
        val remoteFiles = api.listPileFiles(prefs.gitHubToken, prefs.gitHubRepo).getOrNull() ?: return
        val remoteFile = remoteFiles.find { it.path.substringAfterLast("/") == fileName } ?: return
        val content = api.getFile(prefs.gitHubToken, prefs.gitHubRepo, remoteFile.path).getOrNull()?.content ?: return
        api.moveToTrash(prefs.gitHubToken, prefs.gitHubRepo, fileName, remoteFile.sha, content)
    }

    suspend fun syncIfEnabled(): SyncResult? = withContext(Dispatchers.IO) {
        val prefs = userPreferences.userPrefs.first()
        if (!prefs.gitHubEnabled || prefs.gitHubToken.isBlank() || prefs.gitHubRepo.isBlank()) {
            return@withContext null
        }
        if (prefs.gitForge == GitForge.GITEA && prefs.gitHost.isBlank()) {
            return@withContext null
        }

        val api = resolveApi(prefs.gitForge, prefs.gitHost)
        val result = sync(api, prefs.gitHubToken, prefs.gitHubRepo, prefs.lastSyncedAt, prefs.lastSyncedShas)
        if (result.errors.isEmpty()) {
            userPreferences.setLastSyncedAt(System.currentTimeMillis())
            userPreferences.setLastSyncedShas(result.remoteShas)
        }
        result
    }

    suspend fun sync(api: GitForgeApi, token: String, repo: String, lastSyncedAt: Long?, lastSyncedShas: Map<String, String> = emptyMap()): SyncResult = withContext(Dispatchers.IO) {
        var uploaded = 0
        var downloaded = 0
        val errors = mutableListOf<String>()
        var newRemoteShas: Map<String, String> = emptyMap()

        try {
            val localPileFiles = pileDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".md") }
                ?.associateBy { it.name }
                ?: emptyMap()

            val remotePileResult = api.listPileFiles(token, repo)
            if (remotePileResult.isFailure) {
                errors.add("Failed to connect")
                return@withContext SyncResult(errors = errors)
            }
            val remotePileFiles = remotePileResult.getOrThrow().associateBy { it.path.substringAfterLast("/") }
            newRemoteShas = remotePileFiles.mapValues { it.value.sha }

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
                                if (result.isSuccess) uploaded++
                            }
                        }

                        !inLocalPile && inRemotePile -> {
                            if (knownSha != null) {
                                // Deleted locally → move to remote trash
                                val remoteFile = remotePileFiles[fileName]!!
                                val content = api.getFile(token, repo, remoteFile.path).getOrNull()?.content
                                if (content != null) {
                                    api.moveToTrash(token, repo, fileName, remoteFile.sha, content)
                                }
                            } else {
                                // New remote file → download
                                val remoteFile = remotePileFiles[fileName]!!
                                val contentResult = api.getFile(token, repo, remoteFile.path)
                                if (contentResult.isSuccess) {
                                    File(pileDir, fileName).writeText(contentResult.getOrThrow().content, Charsets.UTF_8)
                                    downloaded++
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
                                            downloaded++
                                        }
                                        localChanged -> {
                                            val result = api.putFile(token, repo, "pile/$fileName", localContent, sha = remoteFile.sha, message = "Update $fileName")
                                            if (result.isSuccess) uploaded++
                                        }
                                        else -> {
                                            localFile.writeText(remoteContent, Charsets.UTF_8)
                                            downloaded++
                                        }
                                    }
                                }
                            }
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
            errors = errors,
            remoteShas = newRemoteShas
        )
    }
}
