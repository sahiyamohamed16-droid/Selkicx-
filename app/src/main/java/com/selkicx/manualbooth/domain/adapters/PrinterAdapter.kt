package com.selkicx.manualbooth.domain.adapters

import kotlinx.coroutines.flow.Flow

enum class PrinterConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class PrintJob(
    val finalOutputPath: String,
    val printSizeName: String,
    val copies: Int = 1
)

sealed class PrintResult {
    object Success : PrintResult()
    data class Failure(val message: String) : PrintResult()
}

/**
 * Implemented per printer connection type (Android print service, network,
 * USB, manufacturer API). PRINT must always be an explicit, confirmed
 * action initiated by the UI - this interface never triggers printing on
 * its own (spec rules #16, #17).
 */
interface PrinterAdapter {
    val connectionState: Flow<PrinterConnectionState>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun print(job: PrintJob): PrintResult
}
