package fumi.day.literalmemo.data.github

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubFile(
    val path: String,
    val sha: String,
    val content: String = "",
    val lastModified: Long = 0L
)

@Singleton
class GitHubRepository @Inject constructor() {

    private val baseUrl = "https://api.github.com"

    private suspend fun makeRequest(
        method: String,
        url: String,
        token: String,
        body: String? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                }
            }

            val responseCode = connection.responseCode
            val responseBody = try {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } catch (e: Exception) {
                connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            }

            Pair(responseCode, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun listFiles(token: String, repo: String): Result<List<GitHubFile>> {
        return try {
            val url = "$baseUrl/repos/$repo/contents/pile"
            val (code, body) = makeRequest("GET", url, token)

            when (code) {
                200 -> {
                    val files = mutableListOf<GitHubFile>()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.getString("name")
                        if (name.endsWith(".md") && !name.startsWith(".")) {
                            files.add(
                                GitHubFile(
                                    path = obj.getString("path"),
                                    sha = obj.getString("sha")
                                )
                            )
                        }
                    }
                    Result.success(files)
                }
                404 -> Result.success(emptyList()) // pile directory doesn't exist yet
                else -> Result.failure(Exception("Failed to list files: $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listTrashedFiles(token: String, repo: String): Result<List<GitHubFile>> {
        return try {
            val url = "$baseUrl/repos/$repo/contents/pile"
            val (code, body) = makeRequest("GET", url, token)

            when (code) {
                200 -> {
                    val files = mutableListOf<GitHubFile>()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.getString("name")
                        if (name.endsWith(".md") && name.startsWith(".")) {
                            files.add(
                                GitHubFile(
                                    path = obj.getString("path"),
                                    sha = obj.getString("sha")
                                )
                            )
                        }
                    }
                    Result.success(files)
                }
                404 -> Result.success(emptyList())
                else -> Result.failure(Exception("Failed to list trashed files: $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFile(token: String, repo: String, path: String): Result<GitHubFile> {
        return try {
            val url = "$baseUrl/repos/$repo/contents/$path"
            val (code, body) = makeRequest("GET", url, token)

            when (code) {
                200 -> {
                    val obj = JSONObject(body)
                    val contentBase64 = obj.getString("content").replace("\n", "")
                    val content = String(Base64.decode(contentBase64, Base64.DEFAULT), Charsets.UTF_8)
                    Result.success(
                        GitHubFile(
                            path = obj.getString("path"),
                            sha = obj.getString("sha"),
                            content = content
                        )
                    )
                }
                else -> Result.failure(Exception("Failed to get file: $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun putFile(
        token: String,
        repo: String,
        path: String,
        content: String,
        sha: String? = null,
        message: String = "Update $path"
    ): Result<GitHubFile> {
        return try {
            val url = "$baseUrl/repos/$repo/contents/$path"
            val contentBase64 = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val bodyObj = JSONObject().apply {
                put("message", message)
                put("content", contentBase64)
                if (sha != null) {
                    put("sha", sha)
                }
            }

            val (code, body) = makeRequest("PUT", url, token, bodyObj.toString())

            when (code) {
                200, 201 -> {
                    val obj = JSONObject(body)
                    val contentObj = obj.getJSONObject("content")
                    Result.success(
                        GitHubFile(
                            path = contentObj.getString("path"),
                            sha = contentObj.getString("sha"),
                            content = content
                        )
                    )
                }
                else -> Result.failure(Exception("Failed to put file: $code - $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(
        token: String,
        repo: String,
        path: String,
        sha: String,
        message: String = "Delete $path"
    ): Result<Unit> {
        return try {
            val url = "$baseUrl/repos/$repo/contents/$path"
            val bodyObj = JSONObject().apply {
                put("message", message)
                put("sha", sha)
            }

            val (code, _) = makeRequest("DELETE", url, token, bodyObj.toString())

            when (code) {
                200 -> Result.success(Unit)
                else -> Result.failure(Exception("Failed to delete file: $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trashMemo(
        token: String,
        repo: String,
        path: String,
        sha: String,
        content: String
    ): Result<Unit> {
        return try {
            // Delete original file
            deleteFile(token, repo, path, sha, "Trash $path").getOrThrow()

            // Create trashed version (.filename)
            val fileName = path.substringAfterLast("/")
            val trashedPath = path.substringBeforeLast("/") + "/.$fileName"
            putFile(token, repo, trashedPath, content, null, "Move to trash: $fileName").getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
