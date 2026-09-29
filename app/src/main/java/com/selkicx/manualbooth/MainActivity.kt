package com.selkicx.manualbooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.ui.navigation.BoothNavGraph

/**
 * Single-activity app (spec sections 3-4: minimal navigation, no
 * unnecessary page levels). AppContainer is created once here and
 * threaded down to every screen - no DI framework needed for this size
 * of app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = AppContainer(applicationContext)
        appContainer.cloudSyncScheduler.schedulePeriodic()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    BoothNavGraph(navController = navController, appContainer = appContainer)
                }
            }
        }
    }
}
