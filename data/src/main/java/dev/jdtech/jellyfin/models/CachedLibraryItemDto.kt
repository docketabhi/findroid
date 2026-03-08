package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.Index
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.PlayAccess

@Entity(
    tableName = "cachedLibraryItems",
    primaryKeys = ["libraryId", "id"],
    indices = [Index("serverId"), Index("parentId"), Index("type")],
)
data class CachedLibraryItemDto(
    val id: UUID,
    val libraryId: UUID,
    val serverId: String,
    val parentId: UUID?,
    val type: String,
    val collectionType: String?,
    val name: String,
    val originalTitle: String?,
    val overview: String,
    val runtimeTicks: Long,
    val unplayedItemCount: Int?,
    val canPlay: Boolean,
    val canDownload: Boolean,
    val primaryImageTag: String?,
    val backdropImageTag: String?,
    val logoImageTag: String?,
    val seriesId: UUID?,
    val seriesName: String?,
    val seriesPrimaryImageTag: String?,
    val seasonId: UUID?,
    val seasonName: String?,
    val indexNumber: Int?,
    val indexNumberEnd: Int?,
    val parentIndexNumber: Int?,
    val communityRating: Float?,
    val officialRating: String?,
    val status: String?,
    val productionYear: Int?,
    val album: String?,
    val albumArtist: String?,
    val artists: List<String>?,
)

fun BaseItemDto.toCachedLibraryItemDto(serverId: String, libraryId: UUID): CachedLibraryItemDto? {
    val itemType = type
    return CachedLibraryItemDto(
        id = id,
        libraryId = libraryId,
        serverId = serverId,
        parentId = parentId ?: libraryId,
        type = itemType.serialName,
        collectionType = collectionType?.serialName,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        runtimeTicks = runTimeTicks ?: 0L,
        unplayedItemCount = userData?.unplayedItemCount,
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload == true,
        primaryImageTag = imageTags?.get(ImageType.PRIMARY),
        backdropImageTag = backdropImageTags?.firstOrNull(),
        logoImageTag = imageTags?.get(ImageType.LOGO),
        seriesId = seriesId,
        seriesName = seriesName,
        seriesPrimaryImageTag = seriesPrimaryImageTag,
        seasonId = seasonId,
        seasonName = seasonName,
        indexNumber = indexNumber,
        indexNumberEnd = indexNumberEnd,
        parentIndexNumber = parentIndexNumber,
        communityRating = communityRating,
        officialRating = officialRating,
        status = status,
        productionYear = productionYear,
        album = album,
        albumArtist = albumArtist,
        artists = artists,
    )
}

fun CachedLibraryItemDto.toFindroidItem(
    database: ServerDatabaseDao,
    userId: UUID,
    baseUrl: String,
): FindroidItem? {
    val userData = database.getUserData(id, userId)
    val played = userData?.played == true
    val favorite = userData?.favorite == true
    val playbackPositionTicks = userData?.playbackPositionTicks ?: 0L
    val images =
        buildRemoteFindroidImages(
            baseUrl = baseUrl,
            itemId = id,
            primaryImageTag = primaryImageTag,
            backdropImageTag = backdropImageTag,
            logoImageTag = logoImageTag,
            seriesId = seriesId,
            seriesPrimaryImageTag = seriesPrimaryImageTag,
        )

    return when (BaseItemKind.fromName(type)) {
        BaseItemKind.MOVIE,
        BaseItemKind.VIDEO,
        BaseItemKind.MUSIC_VIDEO,
        BaseItemKind.AUDIO,
        BaseItemKind.TRAILER ->
            FindroidMovie(
                id = id,
                name = name,
                originalTitle = originalTitle,
                overview = overview,
                sources = emptyList(),
                played = played,
                favorite = favorite,
                canPlay = canPlay,
                canDownload = canDownload,
                runtimeTicks = runtimeTicks,
                playbackPositionTicks = playbackPositionTicks,
                premiereDate = null,
                people = emptyList(),
                genres = emptyList(),
                communityRating = communityRating,
                officialRating = officialRating,
                status = status ?: "Ended",
                productionYear = productionYear,
                endDate = null,
                trailer = null,
                unplayedItemCount = unplayedItemCount,
                images = images,
                chapters = emptyList(),
                trickplayInfo = null,
                album = album,
                albumArtist = albumArtist,
                artists = artists.orEmpty(),
            )
        BaseItemKind.SERIES ->
            FindroidShow(
                id = id,
                name = name,
                originalTitle = originalTitle,
                overview = overview,
                sources = emptyList(),
                seasons = emptyList(),
                played = played,
                favorite = favorite,
                canPlay = canPlay,
                canDownload = canDownload,
                playbackPositionTicks = playbackPositionTicks,
                unplayedItemCount = unplayedItemCount,
                genres = emptyList(),
                people = emptyList(),
                runtimeTicks = runtimeTicks,
                communityRating = communityRating,
                officialRating = officialRating,
                status = status ?: "Ended",
                productionYear = productionYear,
                endDate = null,
                trailer = null,
                images = images,
            )
        BaseItemKind.EPISODE ->
            if (seriesId != null && seasonId != null) {
                FindroidEpisode(
                    id = id,
                    name = name,
                    originalTitle = originalTitle,
                    overview = overview,
                    indexNumber = indexNumber ?: 0,
                    indexNumberEnd = indexNumberEnd,
                    parentIndexNumber = parentIndexNumber ?: 0,
                    sources = emptyList(),
                    played = played,
                    favorite = favorite,
                    canPlay = canPlay,
                    canDownload = canDownload,
                    runtimeTicks = runtimeTicks,
                    playbackPositionTicks = playbackPositionTicks,
                    premiereDate = null,
                    seriesId = seriesId,
                    seriesName = seriesName.orEmpty(),
                    seasonId = seasonId,
                    seasonName = seasonName,
                    communityRating = communityRating,
                    people = emptyList(),
                    unplayedItemCount = unplayedItemCount,
                    images = images,
                    chapters = emptyList(),
                    trickplayInfo = null,
                )
            } else {
                null
            }
        BaseItemKind.FOLDER,
        BaseItemKind.MUSIC_ARTIST,
        BaseItemKind.MUSIC_ALBUM,
        BaseItemKind.BOX_SET,
        BaseItemKind.SEASON,
        BaseItemKind.PHOTO,
        BaseItemKind.PHOTO_ALBUM ->
            FindroidFolder(
                id = id,
                name = name,
                originalTitle = originalTitle,
                overview = overview,
                played = played,
                favorite = favorite,
                canPlay = canPlay,
                canDownload = canDownload,
                runtimeTicks = runtimeTicks,
                playbackPositionTicks = playbackPositionTicks,
                unplayedItemCount = unplayedItemCount,
                images = images,
            )
        else -> null
    }
}
