package com.selkicx.manualbooth.printer

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import com.selkicx.manualbooth.domain.adapters.PrintJob
import com.selkicx.manualbooth.domain.adapters.PrintResult
import com.selkicx.manualbooth.domain.adapters.PrinterAdapter
import com.selkicx.manualbooth.domain.adapters.PrinterConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Initial printer implementation using the built-in Android Print
 * Framework (spec section 36). Later adapters (NetworkPrinterAdapter,
 * UsbPrinterAdapter, ManufacturerPrinterAdapter) implement the same
 * [PrinterAdapter] interface without touching calling code.
 */
class AndroidPrintAdapter(
    private val context: Context
) : PrinterAdapter {

    private val _connectionState =
        MutableStateFlow(PrinterConnectionState.CONNECTED) // OS handles discovery
    override val connectionState: Flow<PrinterConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect() {
        _connectionState.value = PrinterConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        _connectionState.value = PrinterConnectionState.DISCONNECTED
    }

    override suspend fun print(job: PrintJob): PrintResult {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val adapter = FinalOutputPrintDocumentAdapter(context, job.finalOutputPath)
            val attributes = PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
            printManager.print("SelkicX Final Output", adapter, attributes)
            PrintResult.Success
        } catch (e: Exception) {
            PrintResult.Failure(e.message ?: "Unable to print")
        }
    }
}
