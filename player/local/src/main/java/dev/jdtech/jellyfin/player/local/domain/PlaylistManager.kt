package dev.jdtech.jellyfin.player.local.domain

import androidx.core.net.toUri
import androidx.media3.common.MimeTypes
import dev.jdtech.jellyfin.models.FindroidChapter
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.FindroidSources
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import dev.jdtech.jellyfin.player.core.domain.models.ExternalSubtitle
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.TrickplayInfo
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

class PlaylistManager @Inject internal constructor(private val repository: JellyfinRepository) {
    private var startItem: FindroidItem? = null
    private var items: List<FindroidItem> = emptyList()
    private val playerItems: MutableList<PlayerItem> = mutableListOf()
    var currentItemIndex: Int = 0

    suspend fun getInitialItem(
        itemId: UUID,
        itemKind: BaseItemKind,
        mediaSourceIndex: Int? = null,
        startFromBeginning: Boolean = false,
        queueParentId: UUID? = null,
    ): PlayerItem? {
        Timber.d("Retrieving initial player item")
        playerItems.clear()

        val initialItem =
            when (itemKind) {
                BaseItemKind.MOVIE,
                BaseItemKind.VIDEO,
                BaseItemKind.MUSIC_VIDEO,
                BaseItemKind.AUDIO,
                BaseItemKind.AUDIO_BOOK,
                BaseItemKind.TRAILER,
                -> {
                    val movie = repository.getMovie(itemId)
                    val queueItems =
                        if (queueParentId == null) {
                            listOf(movie)
                        } else {
                            val includeTypes = queueIncludeTypes(itemKind)
                            val siblings =
                                repository
                                    .getItems(
                                        parentId = queueParentId,
                                        includeTypes = includeTypes,
                                        recursive = false,
                                        sortBy = SortBy.NAME,
                                        sortOrder = SortOrder.ASCENDING,
                                    )
                                    .filterIsInstance<FindroidMovie>()
                                    .filter { it.canPlay }
                            if (siblings.any { it.id == itemId }) siblings else listOf(movie)
                        }

                    items = queueItems
                    queueItems.firstOrNull { it.id == itemId } ?: movie
                }
                BaseItemKind.SERIES -> {
                    val nextUpEpisode = repository.getNextUp(itemId).firstOrNull()

                    val season =
                        if (nextUpEpisode != null) {
                            repository.getSeason(nextUpEpisode.seasonId)
                        } else {
                            val seasons = repository.getSeasons(itemId)
                            if (seasons.isEmpty()) {
                                return null
                            }
                            seasons.first()
                        }

                    val episodes =
                        repository
                            .getEpisodes(
                                seriesId = itemId,
                                seasonId = season.id,
                                fields = listOf(ItemFields.CHAPTERS, ItemFields.TRICKPLAY),
                            )
                            .filter { !it.missing }

                    if (episodes.isEmpty()) {
                        return null
                    }

                    val episode = nextUpEpisode ?: episodes.first()

                    items = episodes
                    episode
                }
                BaseItemKind.SEASON -> {
                    val season = repository.getSeason(itemId)
                    val episodes =
                        repository
                            .getEpisodes(
                                seriesId = season.seriesId,
                                seasonId = season.id,
                                fields = listOf(ItemFields.CHAPTERS, ItemFields.TRICKPLAY),
                            )
                            .filter { !it.missing }

                    if (episodes.isEmpty()) {
                        return null
                    }

                    val episode = episodes.first()

                    items = episodes
                    episode
                }
                BaseItemKind.EPISODE -> {
                    val episode = repository.getEpisode(itemId)

                    val episodes =
                        repository
                            .getEpisodes(
                                seriesId = episode.seriesId,
                                seasonId = episode.seasonId,
                                fields = listOf(ItemFields.CHAPTERS, ItemFields.TRICKPLAY),
                            )
                            .filter { !it.missing }

                    items = episodes
                    episode
                }
                else -> null
            }

        if (initialItem == null) {
            return null
        }

        startItem = initialItem

        currentItemIndex = items.indexOfFirst { it.id == initialItem.id }

        val playbackPosition =
            if (!startFromBeginning) initialItem.playbackPositionTicks.div(10000) else 0
        val playerItem = initialItem.toPlayerItem(mediaSourceIndex, playbackPosition)
        playerItems.add(playerItem)

        return playerItem
    }

    suspend fun getPreviousPlayerItem(): PlayerItem? {
        Timber.d("Retrieving previous player item")

        val itemIndex = currentItemIndex - 1
        val playerItem =
            when (startItem) {
                is FindroidMovie,
                is FindroidEpisode,
                -> {
                    if (currentItemIndex == 0) {
                        null
                    } else {
                        val item = items[itemIndex]
                        if (playerItems.firstOrNull { it.itemId == item.id } == null) {
                            try {
                                item.toPlayerItem(null, 0L)
                            } catch (e: Exception) {
                                Timber.e("Failed to retrieve previous player item: $e")
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
                else -> null
            }

        if (playerItem != null) {
            playerItems.add(playerItem)
        }

        return playerItem
    }

    suspend fun getNextPlayerItem(): PlayerItem? {
        Timber.d("Retrieving next player item")

        val itemIndex = currentItemIndex + 1
        val playerItem =
            when (startItem) {
                is FindroidMovie,
                is FindroidEpisode,
                -> {
                    if (currentItemIndex == items.lastIndex) {
                        null
                    } else {
                        val item = items[itemIndex]
                        if (playerItems.firstOrNull { it.itemId == item.id } == null) {
                            try {
                                item.toPlayerItem(null, 0L)
                            } catch (e: Exception) {
                                Timber.e("Failed to retrieve next player item: $e")
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
                else -> null
            }

        if (playerItem != null) {
            playerItems.add(playerItem)
        }

        return playerItem
    }

    fun setCurrentMediaItemIndex(itemId: UUID) {
        currentItemIndex = items.indexOfFirst { it.id == itemId }
    }

    private suspend fun FindroidItem.toPlayerItem(
        mediaSourceIndex: Int?,
        playbackPosition: Long,
    ): PlayerItem {
        Timber.d("Converting FindroidItem ${this.id} to PlayerItem")

        val mediaSources = repository.getMediaSources(id, true)
        val mediaSource =
            if (mediaSourceIndex == null) {
                mediaSources.firstOrNull { it.type == FindroidSourceType.LOCAL } ?: mediaSources[0]
            } else {
                mediaSources[mediaSourceIndex]
            }
        val externalSubtitles =
            mediaSource.mediaStreams
                .filter { mediaStream ->
                    mediaStream.isExternal &&
                        mediaStream.type == MediaStreamType.SUBTITLE &&
                        !mediaStream.path.isNullOrBlank()
                }
                .map { mediaStream ->
                    ExternalSubtitle(
                        mediaStream.title,
                        mediaStream.language,
                        mediaStream.path!!.toUri(),
                        when (mediaStream.codec) {
                            "subrip" -> MimeTypes.APPLICATION_SUBRIP
                            "webvtt" -> MimeTypes.APPLICATION_SUBRIP
                            "ass" -> MimeTypes.TEXT_SSA
                            else -> MimeTypes.TEXT_UNKNOWN
                        },
                    )
                }
        val trickplayInfo =
            when (this) {
                is FindroidSources -> {
                    this.trickplayInfo?.get(mediaSource.id)?.let {
                        TrickplayInfo(
                            width = it.width,
                            height = it.height,
                            tileWidth = it.tileWidth,
                            tileHeight = it.tileHeight,
                            thumbnailCount = it.thumbnailCount,
                            interval = it.interval,
                            bandwidth = it.bandwidth,
                        )
                    }
                }
                else -> null
            }
        return PlayerItem(
            name = name,
            itemId = id,
            mediaSourceId = mediaSource.id,
            mediaSourceUri = mediaSource.path,
            playbackPosition = playbackPosition,
            parentIndexNumber = if (this is FindroidEpisode) parentIndexNumber else null,
            indexNumber = if (this is FindroidEpisode) indexNumber else null,
            indexNumberEnd = if (this is FindroidEpisode) indexNumberEnd else null,
            externalSubtitles = externalSubtitles,
            chapters = chapters.toPlayerChapters(),
            trickplayInfo = trickplayInfo,
        )
    }

    private fun List<FindroidChapter>.toPlayerChapters(): List<PlayerChapter> {
        return this.map { chapter ->
            PlayerChapter(startPosition = chapter.startPosition, name = chapter.name)
        }
    }

    private fun queueIncludeTypes(itemKind: BaseItemKind): List<BaseItemKind> {
        return when (itemKind) {
            BaseItemKind.AUDIO,
            BaseItemKind.AUDIO_BOOK,
            -> listOf(BaseItemKind.AUDIO, BaseItemKind.AUDIO_BOOK)
            else ->
                listOf(
                    BaseItemKind.VIDEO,
                    BaseItemKind.MOVIE,
                    BaseItemKind.MUSIC_VIDEO,
                    BaseItemKind.TRAILER,
                )
        }
    }
}
