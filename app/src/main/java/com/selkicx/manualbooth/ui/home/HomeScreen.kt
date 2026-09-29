package com.selkicx.manualbooth.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.domain.adapters.CameraConnectionState
import com.selkicx.manualbooth.domain.adapters.PrinterConnectionState
import com.selkicx.manualbooth.domain.model.PrintStatus
import com.selkicx.manualbooth.ui.common.ViewModelFactory

/**
 * Home screen (spec section 5): camera/printer status, NEW SESSION,
 * recent sessions, Admin entry. Deliberately plain - no onboarding,
 * no decorative animation (spec section 3).
 */
@Composable
fun HomeScreen(
    appContainer: AppContainer,
    onNewSession: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenSession: (Long) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory {
            HomeViewModel(
                cameraAdapter = appContainer.cameraAdapter,
                printerAdapter = appContainer.printerAdapter,
                sessionRepository = appContainer.sessionRepository
            )
        }
    )

    val cameraState by viewModel.cameraState.collectAsState()
    val printerState by viewModel.printerState.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("SELKICX MANUAL BOOTH", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(24.dp))
        StatusRow(label = "Camera", connected = cameraState == CameraConnectionState.CONNECTED)
        Spacer(Modifier.height(8.dp))
        StatusRow(label = "Printer", connected = printerState == PrinterConnectionState.CONNECTED)

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNewSession,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("+ NEW SESSION", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(32.dp))
        Text("Recent Sessions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(recentSessions.take(10)) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenSession(session.id) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(session.displayNumber)
                    Text(if (session.printStatus == PrintStatus.PRINTED) "Printed" else "Pending")
                }
            }
        }

        OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
            Text("Admin")
        }
    }
}

@Composable
private fun StatusRow(label: String, connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label  ")
        Text(
            text = if (connected) "\u25CF Connected" else "\u25CB Disconnected",
            color = if (connected) Color(0xFF2E7D32) else Color(0xFF9E9E9E)
        )
    }
}
