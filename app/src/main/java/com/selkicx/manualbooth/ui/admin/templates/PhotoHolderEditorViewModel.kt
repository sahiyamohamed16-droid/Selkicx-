package com.selkicx.manualbooth.ui.admin.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.local.entity.TemplateEntity
import com.selkicx.manualbooth.data.repository.TemplateRepository
import com.selkicx.manualbooth.domain.model.PhotoHolderShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DEFAULT_HOLDER_SIZE = 0.35f

/**
 * Admin > Photo Holder editor (spec sections 26-28): add/move/resize/
 * delete holders over the template artwork. Coordinates stay normalized
 * (0..1) throughout, independent of the editing screen's own pixel size.
 * Required photo count = holder count, always derived from this list -
 * never entered manually (spec rule #13).
 */
class PhotoHolderEditorViewModel(
    private val templateId: Long,
    private val templateRepository: TemplateRepository
) : ViewModel() {

    private val _template = MutableStateFlow<TemplateEntity?>(null)
    val template: StateFlow<TemplateEntity?> = _template.asStateFlow()

    private val _holders = MutableStateFlow<List<EditableHolder>>(emptyList())
    val holders: StateFlow<List<EditableHolder>> = _holders.asStateFlow()

    private val deletedEntityIds = mutableListOf<Long>()

    init {
        viewModelScope.launch {
            _template.value = templateRepository.getTemplate(templateId)
            _holders.value = templateRepository.getHoldersOnce(templateId)
                .sortedBy { it.slotNumber }
                .map { it.toEditable() }
        }
    }

    fun addHolder(shape: PhotoHolderShape) {
        val nextSlot = _holders.value.size + 1
        val newHolder = EditableHolder(
            localId = java.util.UUID.randomUUID().toString(),
            entityId = null,
            slotNumber = nextSlot,
            shape = shape,
            x = (1f - DEFAULT_HOLDER_SIZE) / 2f,
            y = (1f - DEFAULT_HOLDER_SIZE) / 2f,
            width = DEFAULT_HOLDER_SIZE,
            height = DEFAULT_HOLDER_SIZE
        )
        _holders.value = _holders.value + newHolder
    }

    /** [dx]/[dy] are normalized deltas (fraction of canvas width/height moved). */
    fun updatePosition(localId: String, dx: Float, dy: Float) {
        _holders.value = _holders.value.map { holder ->
            if (holder.localId != localId) holder
            else holder.copy(
                x = (holder.x + dx).coerceIn(0f, (1f - holder.width).coerceAtLeast(0f)),
                y = (holder.y + dy).coerceIn(0f, (1f - holder.height).coerceAtLeast(0f))
            )
        }
    }

    /** [dw]/[dh] are normalized deltas applied to the holder's width/height. */
    fun updateSize(localId: String, dw: Float, dh: Float) {
        _holders.value = _holders.value.map { holder ->
            if (holder.localId != localId) holder
            else {
                val newWidth = (holder.width + dw).coerceIn(0.05f, (1f - holder.x).coerceAtLeast(0.05f))
                val newHeight = (holder.height + dh).coerceIn(0.05f, (1f - holder.y).coerceAtLeast(0.05f))
                holder.copy(width = newWidth, height = newHeight)
            }
        }
    }

    fun deleteHolder(localId: String) {
        val target = _holders.value.firstOrNull { it.localId == localId } ?: return
        target.entityId?.let { deletedEntityIds.add(it) }
        _holders.value = _holders.value.filterNot { it.localId == localId }.renumberSlots()
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            deletedEntityIds.forEach { id -> templateRepository.deleteHolderById(id) }
            deletedEntityIds.clear()
            _holders.value.forEach { holder ->
                templateRepository.upsertHolder(holder.toEntity(templateId))
            }
            onSaved()
        }
    }
}
