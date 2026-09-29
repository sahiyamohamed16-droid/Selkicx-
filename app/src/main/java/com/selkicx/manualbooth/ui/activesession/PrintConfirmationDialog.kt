package com.selkicx.manualbooth.ui.activesession

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.selkicx.manualbooth.domain.model.PhotoMode

/**
 * The single required PRINT confirmation popup (spec rules #16, #17).
 * There is no separate print-preview page (spec rule #18).
 */
@Composable
fun PrintConfirmationDialog(
    templateName: String,
    photoCount: Int,
    photoMode: PhotoMode?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Print this session?") },
        text = {
            Text(
                buildString {
                    if (templateName.isNotBlank()) appendLine(templateName)
                    appendLine("$photoCount photos selected")
                    append(if (photoMode == PhotoMode.BLACK_AND_WHITE) "Black & White" else "Original")
                }
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("PRINT") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("CANCEL") } }
    )
}
