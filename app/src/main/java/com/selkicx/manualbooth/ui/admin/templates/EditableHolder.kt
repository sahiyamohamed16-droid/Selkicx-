package com.selkicx.manualbooth.ui.admin.templates

import com.selkicx.manualbooth.data.local.entity.PhotoHolderEntity
import com.selkicx.manualbooth.domain.model.CropMode
import com.selkicx.manualbooth.domain.model.PhotoHolderShape
import java.util.UUID

/**
 * UI-local representation of a photo holder while it's being edited
 * (spec sections 26-28). [entityId] is null until the holder has been
 * saved at least once; [localId] is a stable Compose key independent of
 * persistence, so drag gestures don't restart when a not-yet-saved
 * holder is later assigned a real database id.
 */
data class EditableHolder(
    val localId: String,
    val entityId: Long?,
    val slotNumber: Int,
    val shape: PhotoHolderShape,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

fun PhotoHolderEntity.toEditable(): EditableHolder = EditableHolder(
    localId = UUID.randomUUID().toString(),
    entityId = id,
    slotNumber = slotNumber,
    shape = shape,
    x = x,
    y = y,
    width = width,
    height = height
)

fun EditableHolder.toEntity(templateId: Long): PhotoHolderEntity = PhotoHolderEntity(
    id = entityId ?: 0,
    templateId = templateId,
    slotNumber = slotNumber,
    shape = shape,
    x = x,
    y = y,
    width = width,
    height = height,
    cropMode = CropMode.CENTER_CROP
)

/** Keeps slot numbers contiguous (1..N) in current list order (spec section 27). */
fun List<EditableHolder>.renumberSlots(): List<EditableHolder> =
    mapIndexed { index, holder -> holder.copy(slotNumber = index + 1) }
