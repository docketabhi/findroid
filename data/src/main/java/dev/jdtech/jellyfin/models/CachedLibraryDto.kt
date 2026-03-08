package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

@Entity(tableName = "cachedLibraries", indices = [Index("serverId")])
data class CachedLibraryDto(
    @PrimaryKey val id: UUID,
    val serverId: String,
    val name: String,
    val collectionType: String,
    val primaryImageTag: String? = null,
    val backdropImageTag: String? = null,
    val logoImageTag: String? = null,
)

fun BaseItemDto.toCachedLibraryDto(serverId: String): CachedLibraryDto? {
    val libraryType = CollectionType.fromString(collectionType?.serialName)
    if (libraryType !in CollectionType.supported) {
        return null
    }

    return CachedLibraryDto(
        id = id,
        serverId = serverId,
        name = name.orEmpty(),
        collectionType = libraryType.type,
        primaryImageTag = imageTags?.get(ImageType.PRIMARY),
        backdropImageTag = backdropImageTags?.firstOrNull(),
        logoImageTag = imageTags?.get(ImageType.LOGO),
    )
}

fun CachedLibraryDto.toFindroidCollection(baseUrl: String): FindroidCollection {
    return FindroidCollection(
        id = id,
        name = name,
        type = CollectionType.fromString(collectionType),
        images =
            buildRemoteFindroidImages(
                baseUrl = baseUrl,
                itemId = id,
                primaryImageTag = primaryImageTag,
                backdropImageTag = backdropImageTag,
                logoImageTag = logoImageTag,
            ),
    )
}
