package com.selkicx.manualbooth.ui.history

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.domain.model.CloudSyncStatus
import com.selkicx.manualbooth.domain.model.PrintStatus
import com.selkicx.manualbooth.ui.common.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Session History (spec section 48): every session, newest first, with
 * print/sync status; tapping one opens its detail for viewing originals,
 * the Final Output, and reprinting.
 */
@Composable
fun SessionHistoryScreen(
    appContainer: AppContainer,
    onOpenSession: (Long) -> Unit
) {
    val viewModel: SessionHistoryViewModel = viewModel(
        factory = ViewModelFactory { SessionHistoryViewModel(appContainer.sessionRepository) }
    )
    val sessions by viewModel.sessions.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Session History", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(sessions, key = { it.id }) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable { onOpenSession(session.id) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(session.displayNumber, style = MaterialTheme.typography.titleMedium)
                        Text(formatSessionDate(session.startTime), style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (session.printStatus == PrintStatus.PRINTED) "Printed" else "Not Printed")
                        Text(
                            text = if (session.cloudSyncStatus == CloudSyncStatus.SYNCED) "Synced" else "Pending",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatSessionDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
