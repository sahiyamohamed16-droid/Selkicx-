package com.selkicx.manualbooth.domain.model

/**
 * Shape of a photo holder mask placed on a template.
 * Only these three shapes exist for V1 (spec rule #12).
 */
enum class PhotoHolderShape {
    RECTANGLE,
    HEART,
    UNIQUE
}

/**
 * How a photo is cropped to fill its holder. CENTER_CROP is the only
 * supported mode for V1 (spec section 28).
 */
enum class CropMode {
    CENTER_CROP
}

/**
 * Photo mode belongs to the SESSION, never to the template (spec rule #4).
 * Only these two modes exist for V1 (spec rules #5, #6).
 */
enum class PhotoMode {
    ORIGINAL,
    BLACK_AND_WHITE
}

/** Explicit session lifecycle states (spec section 54). */
enum class SessionState {
    CREATED,
    CAPTURING,
    READY_TO_PRINT,
    RENDERING,
    PRINTING,
    PRINTED,
    COMPLETED,
    ERROR
}

/** Print status, kept separate from cloud sync status. */
enum class PrintStatus {
    NOT_PRINTED,
    PRINTING,
    PRINTED,
    FAILED
}

/** Cloud sync status, independent of QR sharing (spec rule #28). */
enum class CloudSyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}
