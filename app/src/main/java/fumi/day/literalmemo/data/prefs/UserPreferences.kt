package fumi.day.literalmemo.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
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
    val fabOnLeft: Boolean = false,
    val gitHubEnabled: Boolean = false,
    val gitHubToken: String = "",
    val gitHubRepo: String = "",
    val lastSyncedAt: Long? = null,
    val lastSyncedShas: Map<String, String> = emptyMap()
)

@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val encryptedPrefs: SharedPreferences = run {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _gitHubToken = MutableStateFlow(
        encryptedPrefs.getString("github_token", "") ?: ""
    )

    private object Keys {
        val FONT = stringPreferencesKey("font")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val BACKGROUND_COLOR = stringPreferencesKey("background_color")
        val TEXT_COLOR = stringPreferencesKey("text_color")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val FAB_ON_LEFT = booleanPreferencesKey("fab_on_left")
        val GITHUB_ENABLED = booleanPreferencesKey("github_enabled")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
        val LAST_SYNCED_SHAS = stringPreferencesKey("last_synced_shas")
    }

    val userPrefs: Flow<UserPrefs> = combine(
        context.dataStore.data,
        _gitHubToken
    ) { prefs, token ->
        UserPrefs(
            font = prefs[Keys.FONT]?.let { AppFont.valueOf(it) } ?: AppFont.DEFAULT,
            fontSize = prefs[Keys.FONT_SIZE] ?: 16f,
            backgroundColorHex = prefs[Keys.BACKGROUND_COLOR] ?: "#F5F5DC",
            textColorHex = prefs[Keys.TEXT_COLOR] ?: "#000000",
            accentColorHex = prefs[Keys.ACCENT_COLOR] ?: "#6650A4",
            fabOnLeft = prefs[Keys.FAB_ON_LEFT] ?: false,
            gitHubEnabled = prefs[Keys.GITHUB_ENABLED] ?: false,
            gitHubToken = token,
            gitHubRepo = prefs[Keys.GITHUB_REPO] ?: "",
            lastSyncedAt = prefs[Keys.LAST_SYNCED_AT],
            lastSyncedShas = prefs[Keys.LAST_SYNCED_SHAS]?.let { parseShas(it) } ?: emptyMap()
        )
    }

    private fun parseShas(json: String): Map<String, String> {
        return try {
            val obj = org.json.JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } catch (e: Exception) {
            emptyMap()
        }
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

    suspend fun setFabOnLeft(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FAB_ON_LEFT] = enabled
        }
    }

    suspend fun setGitHubConfig(enabled: Boolean, token: String, repo: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GITHUB_ENABLED] = enabled
            prefs[Keys.GITHUB_REPO] = repo
        }
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().putString("github_token", token).apply()
        }
        _gitHubToken.value = token
    }

    suspend fun setLastSyncedAt(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNCED_AT] = timestamp
        }
    }

    suspend fun setLastSyncedShas(shas: Map<String, String>) {
        val json = org.json.JSONObject(shas).toString()
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNCED_SHAS] = json
        }
    }

    suspend fun clearGitHubConfig() {
        context.dataStore.edit { prefs ->
            prefs[Keys.GITHUB_ENABLED] = false
            prefs[Keys.GITHUB_REPO] = ""
            prefs.remove(Keys.LAST_SYNCED_AT)
            prefs.remove(Keys.LAST_SYNCED_SHAS)
        }
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().remove("github_token").apply()
        }
        _gitHubToken.value = ""
    }
}
