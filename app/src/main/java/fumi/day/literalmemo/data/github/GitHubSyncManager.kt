package fumi.day.literalmemo.data.github

import fumi.day.literalmemo.data.git.GitForge
import fumi.day.literalmemo.data.git.GitForgeApi
import fumi.day.literalmemo.data.git.GiteaRepository
import fumi.day.literalmemo.data.git.PileSyncer
import fumi.day.literalmemo.data.git.SyncResult
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives [PileSyncer] from the user's forge preferences and exposes the sync state the UI observes.
 */
@Singleton
class GitHubSyncManager @Inject constructor(
    private val pileSyncer: PileSyncer,
    private val gitHubRepository: GitHubRepository,
    private val userPreferences: UserPreferences
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncErrors = MutableStateFlow<List<String>>(emptyList())

    /** Every error from the last sync attempt; empty once a sync succeeds. */
    val syncErrors: StateFlow<List<String>> = _syncErrors.asStateFlow()

    fun launchSync() {
        if (_isSyncing.value) return
        appScope.launch { syncAndAwait() }
    }

    /** Move to remote trash in the app-scoped coroutine so it survives navigation/ViewModel teardown. */
    fun launchMoveToRemoteTrash(fileName: String) {
        appScope.launch { moveToRemoteTrash(fileName) }
    }

    suspend fun syncAndAwait(): SyncResult? {
        if (_isSyncing.value) return null
        _isSyncing.value = true
        _syncErrors.value = emptyList()
        return try {
            val result = syncIfEnabled()
            if (result != null) {
                _syncErrors.value = result.errors
            }
            result
        } finally {
            _isSyncing.value = false
        }
    }

    private fun resolveApi(forge: GitForge, host: String): GitForgeApi =
        when (forge) {
            GitForge.GITHUB -> gitHubRepository
            GitForge.GITEA -> GiteaRepository(host)
        }

    fun clearLocalData() {
        pileSyncer.clearLocalData()
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
        val result = pileSyncer.sync(api, prefs.gitHubToken, prefs.gitHubRepo, prefs.lastSyncedAt, prefs.lastSyncedShas)
        if (result.errors.isEmpty()) {
            userPreferences.setLastSyncedAt(System.currentTimeMillis())
            userPreferences.setLastSyncedShas(result.remoteShas)
        }
        result
    }
}
