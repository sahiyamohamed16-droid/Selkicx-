package com.selkicx.manualbooth.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Admin root (spec section 47). Templates and Session History are wired;
 * Print Sizes, Camera, Printer, QR Sharing, SelkicX Account, Storage,
 * and About are later milestones, listed here as plain text for now.
 */
@Composable
fun AdminScreen(onOpenTemplates: () -> Unit, onOpenHistory: () -> Unit, onOpenQrSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Admin", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedButton(onClick = onOpenTemplates, modifier = Modifier.fillMaxWidth()) {
            Text("Templates")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
            Text("Session History")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenQrSettings, modifier = Modifier.fillMaxWidth()) {
            Text("QR Sharing")
        }

        Spacer(Modifier.height(24.dp))
        listOf(
            "Print Sizes", "Camera", "Printer",
            "SelkicX Account", "Storage", "About"
        ).forEach { label ->
            Text(
                "$label (coming soon)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
