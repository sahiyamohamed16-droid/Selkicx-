package com.selkicx.manualbooth.ui.admin.templates

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Copies a JPG/PNG selected via the Storage Access Framework (spec
 * section 25: gallery, Android Files, or mounted USB/pendrive - no
 * hardcoded USB paths) into app-private storage. A content:// Uri isn't
 * a stable file path, and [com.selkicx.manualbooth.rendering.FinalOutputRenderer]
 * needs one to decode from later.
 */
class ArtworkImporter(private val context: Context) {

    suspend fun importArtwork(uri: Uri): String = withContext(Dispatchers.IO) {
        val isPng = context.contentResolver.getType(uri)?.contains("png") == true
        val extension = if (isPng) "png" else "jpg"

        val dir = File(context.filesDir, "templates").apply { mkdirs() }
        val outFile = File(dir, "artwork_${UUID.randomUUID()}.$extension")

        context.contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to read the selected file")

        outFile.absolutePath
    }
}
