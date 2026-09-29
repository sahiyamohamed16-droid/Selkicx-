package com.selkicx.manualbooth.data.local

import androidx.room.TypeConverter
import com.selkicx.manualbooth.domain.model.CloudSyncStatus
import com.selkicx.manualbooth.domain.model.CropMode
import com.selkicx.manualbooth.domain.model.PhotoHolderShape
import com.selkicx.manualbooth.domain.model.PhotoMode
import com.selkicx.manualbooth.domain.model.PrintStatus
import com.selkicx.manualbooth.domain.model.SessionState

class Converters {
    @TypeConverter fun fromPhotoHolderShape(v: PhotoHolderShape) = v.name
    @TypeConverter fun toPhotoHolderShape(v: String) = PhotoHolderShape.valueOf(v)

    @TypeConverter fun fromCropMode(v: CropMode) = v.name
    @TypeConverter fun toCropMode(v: String) = CropMode.valueOf(v)

    @TypeConverter fun fromPhotoMode(v: PhotoMode) = v.name
    @TypeConverter fun toPhotoMode(v: String) = PhotoMode.valueOf(v)

    @TypeConverter fun fromSessionState(v: SessionState) = v.name
    @TypeConverter fun toSessionState(v: String) = SessionState.valueOf(v)

    @TypeConverter fun fromPrintStatus(v: PrintStatus) = v.name
    @TypeConverter fun toPrintStatus(v: String) = PrintStatus.valueOf(v)

    @TypeConverter fun fromCloudSyncStatus(v: CloudSyncStatus) = v.name
    @TypeConverter fun toCloudSyncStatus(v: String) = CloudSyncStatus.valueOf(v)
}
