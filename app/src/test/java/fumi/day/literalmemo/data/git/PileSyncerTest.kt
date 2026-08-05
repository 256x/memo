package fumi.day.literalmemo.data.git

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PileSyncerTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var pileDir: File
    private lateinit var syncer: PileSyncer
    private val forge = FakeForgeApi()

    /** Any time far enough in the past that files written by a test count as locally changed. */
    private val lastSyncedAt = 1_000_000L

    @Before
    fun setUp() {
        pileDir = temp.newFolder("pile")
        syncer = PileSyncer(pileDir)
    }

    private fun localFile(name: String, content: String, modifiedAt: Long = lastSyncedAt + 1000): File =
        File(pileDir, name).apply {
            writeText(content)
            setLastModified(modifiedAt)
        }

    private suspend fun sync(lastSyncedShas: Map<String, String> = emptyMap()) =
        syncer.sync(forge, "token", "owner/repo", lastSyncedAt, lastSyncedShas)

    @Test
    fun `untracked local file is uploaded`() = runTest {
        localFile("a.md", "hello")

        val result = sync()

        assertEquals(1, result.uploaded)
        assertEquals("hello", forge.contentOf("pile/a.md"))
        assertEquals(forge.shaOf("pile/a.md"), result.remoteShas["a.md"])
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `untracked remote file is downloaded`() = runTest {
        val sha = forge.seed("pile/a.md", "from remote")

        val result = sync()

        assertEquals(1, result.downloaded)
        assertEquals("from remote", File(pileDir, "a.md").readText())
        assertEquals(sha, result.remoteShas["a.md"])
    }

    @Test
    fun `file deleted locally is moved to remote trash`() = runTest {
        val sha = forge.seed("pile/a.md", "gone locally")

        val result = sync(lastSyncedShas = mapOf("a.md" to sha))

        assertNull(forge.contentOf("pile/a.md"))
        assertEquals("gone locally", forge.contentOf("trash/a.md"))
        assertFalse(File(pileDir, "a.md").exists())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `file trashed on another device is deleted locally`() = runTest {
        localFile("a.md", "still here")
        forge.seed("trash/a.md", "still here")

        sync()

        assertFalse(File(pileDir, "a.md").exists())
    }

    @Test
    fun `file gone from remote without trash entry is deleted locally`() = runTest {
        localFile("a.md", "was synced once")

        sync(lastSyncedShas = mapOf("a.md" to "sha-old"))

        assertFalse(File(pileDir, "a.md").exists())
    }

    @Test
    fun `local-only edit is uploaded over the remote copy`() = runTest {
        val sha = forge.seed("pile/a.md", "remote")
        localFile("a.md", "local edit")

        val result = sync(lastSyncedShas = mapOf("a.md" to sha))

        assertEquals(1, result.uploaded)
        assertEquals(0, result.downloaded)
        assertEquals("local edit", forge.contentOf("pile/a.md"))
        assertEquals(forge.shaOf("pile/a.md"), result.remoteShas["a.md"])
    }

    @Test
    fun `remote-only edit overwrites the local copy`() = runTest {
        val sha = forge.seed("pile/a.md", "remote edit")
        localFile("a.md", "stale local", modifiedAt = lastSyncedAt - 1000)

        val result = sync(lastSyncedShas = mapOf("a.md" to sha))

        assertEquals(1, result.downloaded)
        assertEquals(0, result.uploaded)
        assertEquals("remote edit", File(pileDir, "a.md").readText())
        assertEquals(sha, result.remoteShas["a.md"])
    }

    @Test
    fun `edits on both sides keep the local copy as a conflict file`() = runTest {
        forge.seed("pile/a.md", "remote edit")
        localFile("a.md", "local edit")

        // The known SHA is stale, so the remote counts as changed too.
        val result = sync(lastSyncedShas = mapOf("a.md" to "sha-old"))

        assertEquals("remote edit", File(pileDir, "a.md").readText())
        val conflict = pileDir.listFiles()!!.singleOrNull { it.name.startsWith("a_conflict_") }
        assertNotNull("expected a conflict copy", conflict)
        assertEquals("local edit", conflict!!.readText())
        assertEquals(1, result.downloaded)
    }

    @Test
    fun `identical contents are left alone but keep tracking the remote sha`() = runTest {
        val sha = forge.seed("pile/a.md", "same")
        localFile("a.md", "same")

        val result = sync(lastSyncedShas = mapOf("a.md" to sha))

        assertEquals(0, result.uploaded)
        assertEquals(0, result.downloaded)
        assertEquals(sha, result.remoteShas["a.md"])
    }

    @Test
    fun `listing failure reports the underlying cause and stops`() = runTest {
        localFile("a.md", "hello")
        forge.listPileError = Exception("Failed to list files in pile: 401")

        val result = sync()

        assertEquals(1, result.errors.size)
        assertTrue(result.errors.first().contains("401"))
        assertEquals(0, result.uploaded)
    }

    @Test
    fun `upload failure is reported per file`() = runTest {
        localFile("a.md", "hello")
        forge.putError = Exception("Failed to put file: 403 - forbidden")

        val result = sync()

        assertEquals(0, result.uploaded)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors.first().startsWith("a.md: upload failed"))
        assertTrue(result.errors.first().contains("403"))
    }

    @Test
    fun `long error messages are truncated`() = runTest {
        localFile("a.md", "hello")
        forge.putError = Exception("x".repeat(500))

        val result = sync()

        assertTrue(result.errors.first().length < 250)
        assertTrue(result.errors.first().endsWith("…"))
    }

    @Test
    fun `clearLocalData removes every memo`() {
        localFile("a.md", "one")
        localFile("b.md", "two")

        syncer.clearLocalData()

        assertEquals(0, pileDir.listFiles()!!.size)
    }
}
