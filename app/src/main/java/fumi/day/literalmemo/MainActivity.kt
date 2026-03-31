package fumi.day.literalmemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint
import fumi.day.literalmemo.data.DefaultMemoInitializer
import fumi.day.literalmemo.data.github.GitHubSyncManager
import fumi.day.literalmemo.data.prefs.UserPreferences
import fumi.day.literalmemo.ui.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var defaultMemoInitializer: DefaultMemoInitializer

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var syncManager: GitHubSyncManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sharedTextState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        defaultMemoInitializer.initializeIfNeeded()

        sharedTextState.value = handleShareIntent(intent)

        setContent {
            App(
                userPreferences = userPreferences,
                sharedText = sharedTextState.value,
                onSharedTextConsumed = { sharedTextState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedTextState.value = handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        syncInBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun handleShareIntent(intent: Intent): String? {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

            return if (title != null && (text.startsWith("http://") || text.startsWith("https://"))) {
                "# $title\n\n[$title]($text)"
            } else if (text.startsWith("http://") || text.startsWith("https://")) {
                "# Shared link\n\n[Link]($text)"
            } else if (title != null) {
                "# $title\n\n$text"
            } else {
                "# Shared text\n\n$text"
            }
        }
        return null
    }

    private fun syncInBackground() {
        scope.launch {
            try {
                val prefs = userPreferences.userPrefs.first()
                if (prefs.gitHubEnabled && prefs.gitHubToken.isNotBlank() && prefs.gitHubRepo.isNotBlank()) {
                    val result = syncManager.sync(prefs.gitHubToken, prefs.gitHubRepo, prefs.lastSyncedAt)
                    if (result.errors.isEmpty()) {
                        userPreferences.setLastSyncedAt(System.currentTimeMillis())
                    }
                }
            } catch (e: Exception) {
                // Silent fail for background sync
            }
        }
    }
}
