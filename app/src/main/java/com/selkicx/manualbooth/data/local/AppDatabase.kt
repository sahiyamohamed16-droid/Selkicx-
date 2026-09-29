package com.selkicx.manualbooth.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.selkicx.manualbooth.data.local.dao.AccountDao
import com.selkicx.manualbooth.data.local.dao.CloudSyncJobDao
import com.selkicx.manualbooth.data.local.dao.DeviceDao
import com.selkicx.manualbooth.data.local.dao.FinalOutputDao
import com.selkicx.manualbooth.data.local.dao.PhotoHolderDao
import com.selkicx.manualbooth.data.local.dao.PrintSizeDao
import com.selkicx.manualbooth.data.local.dao.PrinterProfileDao
import com.selkicx.manualbooth.data.local.dao.SessionDao
import com.selkicx.manualbooth.data.local.dao.SessionPhotoDao
import com.selkicx.manualbooth.data.local.dao.ShareLinkDao
import com.selkicx.manualbooth.data.local.dao.TemplateDao
import com.selkicx.manualbooth.data.local.dao.TemplateFolderDao
import com.selkicx.manualbooth.data.local.entity.AccountEntity
import com.selkicx.manualbooth.data.local.entity.CloudSyncJobEntity
import com.selkicx.manualbooth.data.local.entity.DeviceEntity
import com.selkicx.manualbooth.data.local.entity.FinalOutputEntity
import com.selkicx.manualbooth.data.local.entity.PhotoHolderEntity
import com.selkicx.manualbooth.data.local.entity.PrintSizeEntity
import com.selkicx.manualbooth.data.local.entity.PrinterProfileEntity
import com.selkicx.manualbooth.data.local.entity.SessionEntity
import com.selkicx.manualbooth.data.local.entity.SessionPhotoEntity
import com.selkicx.manualbooth.data.local.entity.ShareLinkEntity
import com.selkicx.manualbooth.data.local.entity.TemplateEntity
import com.selkicx.manualbooth.data.local.entity.TemplateFolderEntity

@Database(
    entities = [
        PrintSizeEntity::class,
        TemplateFolderEntity::class,
        TemplateEntity::class,
        PhotoHolderEntity::class,
        SessionEntity::class,
        SessionPhotoEntity::class,
        FinalOutputEntity::class,
        PrinterProfileEntity::class,
        CloudSyncJobEntity::class,
        ShareLinkEntity::class,
        AccountEntity::class,
        DeviceEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun printSizeDao(): PrintSizeDao
    abstract fun templateFolderDao(): TemplateFolderDao
    abstract fun templateDao(): TemplateDao
    abstract fun photoHolderDao(): PhotoHolderDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionPhotoDao(): SessionPhotoDao
    abstract fun finalOutputDao(): FinalOutputDao
    abstract fun printerProfileDao(): PrinterProfileDao
    abstract fun cloudSyncJobDao(): CloudSyncJobDao
    abstract fun shareLinkDao(): ShareLinkDao
    abstract fun accountDao(): AccountDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        const val DATABASE_NAME = "selkicx_manual_booth.db"

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Single shared instance so AppContainer and the WorkManager-instantiated
         * [com.selkicx.manualbooth.cloud.CloudSyncWorker] (which only receives a
         * Context, not the hand-rolled DI container) talk to the same database.
         */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
