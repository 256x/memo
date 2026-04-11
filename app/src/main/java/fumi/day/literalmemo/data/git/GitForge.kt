package fumi.day.literalmemo.data.git

enum class GitForge { GITHUB, GITEA }

data class RemoteFile(
    val path: String,
    val sha: String,
    val content: String = ""
)

interface GitForgeApi {
    suspend fun listPileFiles(token: String, repo: String): Result<List<RemoteFile>>
    suspend fun listTrashFiles(token: String, repo: String): Result<List<RemoteFile>>
    suspend fun getFile(token: String, repo: String, path: String): Result<RemoteFile>
    suspend fun putFile(token: String, repo: String, path: String, content: String, sha: String? = null, message: String = "Update $path"): Result<RemoteFile>
    suspend fun deleteFile(token: String, repo: String, path: String, sha: String, message: String = "Delete $path"): Result<Unit>

    suspend fun moveToTrash(token: String, repo: String, fileName: String, sha: String, content: String): Result<Unit> {
        return try {
            putFile(token, repo, "trash/$fileName", content, null, "Trash: $fileName").getOrThrow()
            deleteFile(token, repo, "pile/$fileName", sha, "Move to trash: $fileName").getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
