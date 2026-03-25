package fumi.day.literalmemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import fumi.day.literalmemo.data.DefaultMemoInitializer
import fumi.day.literalmemo.data.prefs.UserPreferences
import fumi.day.literalmemo.ui.App
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var defaultMemoInitializer: DefaultMemoInitializer

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        defaultMemoInitializer.initializeIfNeeded()

        setContent {
            App(userPreferences = userPreferences)
        }
    }
}
