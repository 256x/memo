package fumi.day.literalmemo.data.git

/**
 * In-memory stand-in for a forge. Paths are stored exactly as the syncer addresses them
 * (`pile/foo.md`, `trash/foo.md`), and every write mints a fresh SHA the way a real commit would.
 */
class FakeForgeApi : GitForgeApi {

    private val contents = linkedMapOf<String, String>()
    private val shas = linkedMapOf<String, String>()
    private var shaCounter = 0

    var listPileError: Throwable? = null
    var putError: Throwable? = null
    var getError: Throwable? = null

    val paths: Set<String> get() = contents.keys

    fun seed(path: String, content: String): String {
        contents[path] = content
        val sha = "sha${++shaCounter}"
        shas[path] = sha
        return sha
    }

    fun contentOf(path: String): String? = contents[path]

    fun shaOf(path: String): String? = shas[path]

    private fun list(dir: String): Result<List<RemoteFile>> = Result.success(
        contents.keys
            .filter { it.startsWith("$dir/") }
            .map { RemoteFile(path = it, sha = shas.getValue(it)) }
    )

    override suspend fun listPileFiles(token: String, repo: String): Result<List<RemoteFile>> =
        listPileError?.let { Result.failure(it) } ?: list("pile")

    override suspend fun listTrashFiles(token: String, repo: String): Result<List<RemoteFile>> = list("trash")

    override suspend fun getFile(token: String, repo: String, path: String): Result<RemoteFile> {
        getError?.let { return Result.failure(it) }
        val content = contents[path] ?: return Result.failure(Exception("Failed to get file: 404"))
        return Result.success(RemoteFile(path = path, sha = shas.getValue(path), content = content))
    }

    override suspend fun putFile(
        token: String,
        repo: String,
        path: String,
        content: String,
        sha: String?,
        message: String
    ): Result<RemoteFile> {
        putError?.let { return Result.failure(it) }
        val newSha = seed(path, content)
        return Result.success(RemoteFile(path = path, sha = newSha, content = content))
    }

    override suspend fun deleteFile(
        token: String,
        repo: String,
        path: String,
        sha: String,
        message: String
    ): Result<Unit> {
        contents.remove(path) ?: return Result.failure(Exception("Failed to delete file: 404"))
        shas.remove(path)
        return Result.success(Unit)
    }
}
