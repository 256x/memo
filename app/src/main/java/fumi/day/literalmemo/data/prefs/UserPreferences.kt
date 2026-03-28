package fumi.day.literalmemo.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

enum class AppFont {
    DEFAULT, SERIF, MONOSPACE, SCOPE_ONE
}

data class UserPrefs(
    val font: AppFont = AppFont.DEFAULT,
    val fontSize: Float = 16f,
    val backgroundColorHex: String = "#F5F5DC",
    val textColorHex: String = "#000000",
    val accentColorHex: String = "#6650A4",
    val gitHubEnabled: Boolean = false,
    val gitHubToken: String = "",
    val gitHubRepo: String = "",
    val lastSyncedAt: Long? = null
)

@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val FONT = stringPreferencesKey("font")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val BACKGROUND_COLOR = stringPreferencesKey("background_color")
        val TEXT_COLOR = stringPreferencesKey("text_color")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val GITHUB_ENABLED = booleanPreferencesKey("github_enabled")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }

    val userPrefs: Flow<UserPrefs> = context.dataStore.data.map { prefs ->
        UserPrefs(
            font = prefs[Keys.FONT]?.let { AppFont.valueOf(it) } ?: AppFont.DEFAULT,
            fontSize = prefs[Keys.FONT_SIZE] ?: 16f,
            backgroundColorHex = prefs[Keys.BACKGROUND_COLOR] ?: "#F5F5DC",
            textColorHex = prefs[Keys.TEXT_COLOR] ?: "#000000",
            accentColorHex = prefs[Keys.ACCENT_COLOR] ?: "#6650A4",
            gitHubEnabled = prefs[Keys.GITHUB_ENABLED] ?: false,
            gitHubToken = prefs[Keys.GITHUB_TOKEN] ?: "",
            gitHubRepo = prefs[Keys.GITHUB_REPO] ?: "",
            lastSyncedAt = prefs[Keys.LAST_SYNCED_AT]
        )
    }

    suspend fun setFont(font: AppFont) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT] = font.name
        }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE] = size
        }
    }

    suspend fun setBackgroundColor(hex: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_COLOR] = hex
        }
    }

    suspend fun setTextColor(hex: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEXT_COLOR] = hex
        }
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCENT_COLOR] = hex
        }
    }

    suspend fun setGitHubConfig(enabled: Boolean, token: String, repo: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GITHUB_ENABLED] = enabled
            prefs[Keys.GITHUB_TOKEN] = token
            prefs[Keys.GITHUB_REPO] = repo
        }
    }

    suspend fun setLastSyncedAt(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNCED_AT] = timestamp
        }
    }

    suspend fun clearGitHubConfig() {
        context.dataStore.edit { prefs ->
            prefs[Keys.GITHUB_ENABLED] = false
            prefs[Keys.GITHUB_TOKEN] = ""
            prefs[Keys.GITHUB_REPO] = ""
            prefs.remove(Keys.LAST_SYNCED_AT)
        }
    }
}
