package com.selkicx.manualbooth.di

import android.content.Context
import com.selkicx.manualbooth.camera.ManualImportCameraAdapter
import com.selkicx.manualbooth.cloud.CloudSyncScheduler
import com.selkicx.manualbooth.data.local.AppDatabase
import com.selkicx.manualbooth.data.repository.AppSettingsRepository
import com.selkicx.manualbooth.data.repository.QrRepository
import com.selkicx.manualbooth.data.repository.SessionRepository
import com.selkicx.manualbooth.data.repository.TemplateRepository
import com.selkicx.manualbooth.domain.adapters.CameraAdapter
import com.selkicx.manualbooth.domain.adapters.PrinterAdapter
import com.selkicx.manualbooth.printer.AndroidPrintAdapter
import com.selkicx.manualbooth.qr.QrCodeGenerator
import com.selkicx.manualbooth.rendering.FinalOutputUseCase
import com.selkicx.manualbooth.ui.admin.templates.ArtworkImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Minimal hand-rolled DI container. Kept deliberately simple (spec
 * priorities: reliability, speed, simplicity, maintainability) rather
 * than pulling in a DI framework for a small single-module app.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Shared with CloudSyncWorker via AppDatabase.getInstance() - see its docs. */
    val database: AppDatabase = AppDatabase.getInstance(appContext)

    /**
     * Swap this for a real CanonCameraAdapter/NikonCameraAdapter/etc. later;
     * nothing outside this container needs to change (spec section 15).
     */
    val cameraAdapter: CameraAdapter = ManualImportCameraAdapter(appContext)

    val printerAdapter: PrinterAdapter = AndroidPrintAdapter(appContext)

    val templateRepository: TemplateRepository by lazy {
        TemplateRepository(
            printSizeDao = database.printSizeDao(),
            folderDao = database.templateFolderDao(),
            templateDao = database.templateDao(),
            photoHolderDao = database.photoHolderDao()
        )
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(
            sessionDao = database.sessionDao(),
            sessionPhotoDao = database.sessionPhotoDao(),
            finalOutputDao = database.finalOutputDao(),
            cameraAdapter = cameraAdapter
        )
    }

    /** Milestone 8: schedules/retries CloudSyncWorker (spec sections 44-46). */
    val cloudSyncScheduler: CloudSyncScheduler by lazy { CloudSyncScheduler(appContext) }

    /** Milestone 4: crop/mask/B&W/composite pipeline, invoked from ActiveSessionViewModel. */
    val finalOutputUseCase: FinalOutputUseCase by lazy {
        FinalOutputUseCase(
            context = appContext,
            sessionDao = database.sessionDao(),
            sessionPhotoDao = database.sessionPhotoDao(),
            templateDao = database.templateDao(),
            photoHolderDao = database.photoHolderDao(),
            printSizeDao = database.printSizeDao(),
            finalOutputDao = database.finalOutputDao(),
            cloudSyncJobDao = database.cloudSyncJobDao(),
            cloudSyncScheduler = cloudSyncScheduler
        )
    }

    /** Milestone 6: copies SAF-selected template artwork into app-private storage. */
    val artworkImporter: ArtworkImporter by lazy { ArtworkImporter(appContext) }

    /** Milestone 8: QR Sharing ON/OFF (spec section 42). */
    val appSettingsRepository: AppSettingsRepository by lazy { AppSettingsRepository(appContext) }

    /** Milestone 8: manual-only QR share links (spec sections 42-46). */
    val qrRepository: QrRepository by lazy { QrRepository(database.shareLinkDao()) }
    val qrCodeGenerator: QrCodeGenerator by lazy { QrCodeGenerator() }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        applicationScope.launch {
            templateRepository.seedDefaultPrintSizesIfEmpty()
        }
    }
}
