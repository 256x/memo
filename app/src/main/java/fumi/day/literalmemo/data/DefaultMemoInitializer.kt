package fumi.day.literalmemo.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMemoInitializer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val pileDir: File by lazy {
        File(context.filesDir, "pile").also { it.mkdirs() }
    }

    private val prefs by lazy {
        context.getSharedPreferences("literal_memo_init", Context.MODE_PRIVATE)
    }

    fun initializeIfNeeded() {
        if (prefs.getBoolean("default_memos_copied", false)) {
            return
        }

        try {
            val assetManager = context.assets
            val baseTime = System.currentTimeMillis()
            
            val orderedFiles = listOf(
                "README.md" to 0L,
                "User_Guide.md" to -1000L,
                "Markdown_Guide.md" to -2000L
            )

            for ((fileName, offset) in orderedFiles) {
                val targetFile = File(pileDir, fileName)
                if (!targetFile.exists()) {
                    assetManager.open("default_memos/$fileName").use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    targetFile.setLastModified(baseTime + offset)
                }
            }

            prefs.edit { putBoolean("default_memos_copied", true) }
        } catch (e: Exception) {
        }
    }
}
