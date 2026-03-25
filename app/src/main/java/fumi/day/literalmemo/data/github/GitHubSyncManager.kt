package fumi.day.literalmemo.data.github

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalmemo.data.repository.MemoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val deleted: Int = 0,
    val errors: List<String> = emptyList()
)

@Singleton
class GitHubSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gitHubRepository: GitHubRepository,
    private val memoRepository: MemoRepository
) {

    private val pileDir: File by lazy {
        File(context.filesDir, "pile").also { it.mkdirs() }
    }

    suspend fun sync(token: String, repo: String): SyncResult = withContext(Dispatchers.IO) {
        var uploaded = 0
        var downloaded = 0
        var deleted = 0
        val errors = mutableListOf<String>()

        try {
            val localMemos = memoRepository.observeAll().first()
            val localFiles = localMemos.associateBy { it.fileName }

            val localTrashedMemos = memoRepository.observeTrash().first()
            val localTrashedFiles = localTrashedMemos.associateBy { it.fileName }

            val remoteFilesResult = gitHubRepository.listFiles(token, repo)
            if (remoteFilesResult.isFailure) {
                errors.add("Failed to list remote files: ${remoteFilesResult.exceptionOrNull()?.message}")
                return@withContext SyncResult(errors = errors)
            }
            val remoteFiles = remoteFilesResult.getOrThrow()
            val remoteFileMap = remoteFiles.associateBy { it.path.substringAfterLast("/") }

            val remoteTrashedResult = gitHubRepository.listTrashedFiles(token, repo)
            val remoteTrashedFiles = remoteTrashedResult.getOrNull() ?: emptyList()
            val remoteTrashedMap = remoteTrashedFiles.associateBy { it.path.substringAfterLast("/") }

            // Upload local memos
            for ((fileName, memo) in localFiles) {
                val remoteFile = remoteFileMap[fileName]
                val trashedFileName = ".$fileName"
                val remoteTrashedFile = remoteTrashedMap[trashedFileName]

                // If file exists in remote trash but is now active locally (restored),
                // delete from remote trash first
                if (remoteTrashedFile != null) {
                    val deleteResult = gitHubRepository.deleteFile(
                        token = token,
                        repo = repo,
                        path = remoteTrashedFile.path,
                        sha = remoteTrashedFile.sha,
                        message = "Remove from trash: $trashedFileName (restored)"
                    )
                    if (deleteResult.isSuccess) {
                        deleted++
                    } else {
                        errors.add("Failed to remove $trashedFileName from remote trash: ${deleteResult.exceptionOrNull()?.message}")
                    }
                }

                if (remoteFile == null) {
                    val result = gitHubRepository.putFile(
                        token = token,
                        repo = repo,
                        path = "pile/$fileName",
                        content = memo.content,
                        message = "Add $fileName"
                    )
                    if (result.isSuccess) {
                        uploaded++
                    } else {
                        errors.add("Failed to upload $fileName: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    val remoteContent = gitHubRepository.getFile(token, repo, remoteFile.path)
                        .getOrNull()?.content

                    if (remoteContent != null && remoteContent != memo.content) {
                        val result = gitHubRepository.putFile(
                            token = token,
                            repo = repo,
                            path = "pile/$fileName",
                            content = memo.content,
                            sha = remoteFile.sha,
                            message = "Update $fileName"
                        )
                        if (result.isSuccess) {
                            uploaded++
                        } else {
                            errors.add("Failed to update $fileName: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            }

            // Download remote memos (not in local and not in local trash)
            for ((fileName, remoteFile) in remoteFileMap) {
                val trashedFileName = ".$fileName"
                if (!localFiles.containsKey(fileName) && !localTrashedFiles.containsKey(trashedFileName)) {
                    val contentResult = gitHubRepository.getFile(token, repo, remoteFile.path)
                    if (contentResult.isSuccess) {
                        val content = contentResult.getOrThrow().content
                        File(pileDir, fileName).writeText(content, Charsets.UTF_8)
                        downloaded++
                    } else {
                        errors.add("Failed to download $fileName: ${contentResult.exceptionOrNull()?.message}")
                    }
                }
            }

            // Upload local trashed memos to remote trash
            for ((trashedFileName, memo) in localTrashedFiles) {
                val originalFileName = trashedFileName.removePrefix(".")
                val remoteFile = remoteFileMap[originalFileName]
                val remoteTrashedFile = remoteTrashedMap[trashedFileName]

                if (remoteFile != null) {
                    // Move from pile to trash on remote
                    val result = gitHubRepository.trashMemo(
                        token = token,
                        repo = repo,
                        path = remoteFile.path,
                        sha = remoteFile.sha,
                        content = memo.content
                    )
                    if (result.isSuccess) {
                        deleted++
                    } else {
                        errors.add("Failed to trash $originalFileName on remote: ${result.exceptionOrNull()?.message}")
                    }
                } else if (remoteTrashedFile == null) {
                    // Upload trashed file directly
                    val result = gitHubRepository.putFile(
                        token = token,
                        repo = repo,
                        path = "pile/$trashedFileName",
                        content = memo.content,
                        message = "Add trashed $trashedFileName"
                    )
                    if (result.isSuccess) {
                        uploaded++
                    } else {
                        errors.add("Failed to upload trashed $trashedFileName: ${result.exceptionOrNull()?.message}")
                    }
                }
            }

            // Download remote trashed memos (not in local trash and original not in local)
            for ((trashedFileName, remoteTrashedFile) in remoteTrashedMap) {
                if (!localTrashedFiles.containsKey(trashedFileName)) {
                    val originalFileName = trashedFileName.removePrefix(".")
                    // Only download if original is not in local files (not restored)
                    if (!localFiles.containsKey(originalFileName)) {
                        val contentResult = gitHubRepository.getFile(token, repo, remoteTrashedFile.path)
                        if (contentResult.isSuccess) {
                            val content = contentResult.getOrThrow().content
                            File(pileDir, trashedFileName).writeText(content, Charsets.UTF_8)
                            downloaded++
                        } else {
                            errors.add("Failed to download trashed $trashedFileName: ${contentResult.exceptionOrNull()?.message}")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            errors.add("Sync error: ${e.message}")
        }

        SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            deleted = deleted,
            errors = errors
        )
    }
}
