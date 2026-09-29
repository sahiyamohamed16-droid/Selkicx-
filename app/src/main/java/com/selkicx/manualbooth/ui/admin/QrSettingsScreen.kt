package com.selkicx.manualbooth.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selkicx.manualbooth.di.AppContainer

/**
 * Admin > QR Sharing (spec section 42): a single ON/OFF toggle. Turning
 * this on does NOT make QR appear automatically anywhere - it only
 * controls whether the QR action is offered at all (spec rules #25-26).
 */
@Composable
fun QrSettingsScreen(appContainer: AppContainer) {
    val enabled by appContainer.appSettingsRepository.qrSharingEnabled.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("QR Sharing", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Enabled", modifier = Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { appContainer.appSettingsRepository.setQrSharingEnabled(it) }
            )
        }
    }
}
