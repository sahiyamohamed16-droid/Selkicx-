package com.selkicx.manualbooth.ui.admin.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.domain.model.PhotoHolderShape
import com.selkicx.manualbooth.ui.common.ViewModelFactory

/**
 * Admin > Photo Holder editor (spec sections 26-28): place rectangle/
 * heart/unique holders over the template artwork; drag a holder to
 * move it, drag its corner handle to resize, tap Delete to remove it.
 * Every holder gets a visible slot number (spec section 27); required
 * photo count = holder count, shown live and never entered manually.
 */
@Composable
fun PhotoHolderEditorScreen(
    appContainer: AppContainer,
    templateId: Long,
    onSaved: () -> Unit
) {
    val viewModel: PhotoHolderEditorViewModel = viewModel(
        key = "holder_editor_$templateId",
        factory = ViewModelFactory { PhotoHolderEditorViewModel(templateId, appContainer.templateRepository) }
    )
    val holders by viewModel.holders.collectAsState()
    val template by viewModel.template.collectAsState()

    var selectedLocalId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showShapePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(template?.name ?: "", style = MaterialTheme.typography.titleLarge)
        Text("Required Photos = ${holders.size}", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { canvasSize = it }
        ) {
            template?.artworkPath?.let { path ->
                AsyncImage(model = path, contentDescription = null, modifier = Modifier.fillMaxSize())
            }

            holders.forEach { holder ->
                HolderBox(
                    holder = holder,
                    canvasSize = canvasSize,
                    isSelected = holder.localId == selectedLocalId,
                    onSelect = { selectedLocalId = holder.localId },
                    onDrag = { dx, dy -> viewModel.updatePosition(holder.localId, dx, dy) },
                    onResize = { dw, dh -> viewModel.updateSize(holder.localId, dw, dh) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showShapePicker = true }, modifier = Modifier.weight(1f)) {
                Text("+ ADD PHOTO HOLDER")
            }
            if (selectedLocalId != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteHolder(selectedLocalId!!)
                        selectedLocalId = null
                    }
                ) {
                    Text("Delete")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.save(onSaved = onSaved) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save Template")
        }
    }

    if (showShapePicker) {
        ShapePickerDialog(
            onDismiss = { showShapePicker = false },
            onPick = { shape ->
                viewModel.addHolder(shape)
                showShapePicker = false
            }
        )
    }
}

@Composable
private fun HolderBox(
    holder: EditableHolder,
    canvasSize: IntSize,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit
) {
    if (canvasSize.width == 0 || canvasSize.height == 0) return
    val density = LocalDensity.current

    val leftPx = holder.x * canvasSize.width
    val topPx = holder.y * canvasSize.height
    val widthPx = holder.width * canvasSize.width
    val heightPx = holder.height * canvasSize.height

    val offsetX = with(density) { leftPx.toDp() }
    val offsetY = with(density) { topPx.toDp() }
    val boxWidth = with(density) { widthPx.toDp() }
    val boxHeight = with(density) { heightPx.toDp() }

    Box(modifier = Modifier.offset(x = offsetX, y = offsetY).size(width = boxWidth, height = boxHeight)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                )
                .pointerInput(holder.localId) {
                    detectDragGestures(onDragStart = { onSelect() }) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x / canvasSize.width, dragAmount.y / canvasSize.height)
                    }
                }
        ) {
            Text(
                "${holder.slotNumber}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 4.dp)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .pointerInput(holder.localId) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x / canvasSize.width, dragAmount.y / canvasSize.height)
                        }
                    }
            )
        }
    }
}

/** Only 3 shapes exist for V1 (spec rule #12) - Rectangle, Heart, Unique. */
@Composable
private fun ShapePickerDialog(onDismiss: () -> Unit, onPick: (PhotoHolderShape) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Holder Shape") },
        text = {
            Column {
                TextButton(onClick = { onPick(PhotoHolderShape.RECTANGLE) }) { Text("Rectangle") }
                TextButton(onClick = { onPick(PhotoHolderShape.HEART) }) { Text("Heart") }
                TextButton(onClick = { onPick(PhotoHolderShape.UNIQUE) }) { Text("Unique") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
