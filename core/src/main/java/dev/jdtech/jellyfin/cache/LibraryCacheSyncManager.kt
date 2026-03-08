package dev.jdtech.jellyfin.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.toCachedLibraryDto
import dev.jdtech.jellyfin.models.toCachedLibraryItemDto
import dev.jdtech.jellyfin.models.toFindroidEpisodeDto
import dev.jdtech.jellyfin.models.toFindroidMovieDto
import dev.jdtech.jellyfin.models.toFindroidSeasonDto
import dev.jdtech.jellyfin.models.toFindroidShowDto
import dev.jdtech.jellyfin.models.toFindroidUserDataDto
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder as ApiSortOrder

@Singleton
class LibraryCacheSyncManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
    private val statusStore: LibraryCacheStatusStore,
) {
    suspend fun syncCurrentServer(): LibraryCacheSyncSummary = withContext(Dispatchers.IO) {
        val serverId = appPreferences.getValue(appPreferences.currentServer)
            ?: return@withContext LibraryCacheSyncSummary(0, 0)
        val user = database.getServerCurrentUser(serverId)
            ?: return@withContext LibraryCacheSyncSummary(0, 0)
        val serverAddress = database.getServerCurrentAddress(serverId)
            ?: return@withContext LibraryCacheSyncSummary(0, 0)

        statusStore.markSyncing()

        val jellyfinApi =
            JellyfinApi(
                androidContext = context.applicationContext,
                requestTimeout = appPreferences.getValue(appPreferences.requestTimeout),
                connectTimeout = appPreferences.getValue(appPreferences.connectTimeout),
                socketTimeout = appPreferences.getValue(appPreferences.socketTimeout),
            ).apply {
                api.update(baseUrl = serverAddress.address, accessToken = user.accessToken)
                userId = user.id
            }

        val cachedLibraries =
            jellyfinApi.viewsApi.getUserViews(user.id).content.items.mapNotNull {
                it.toCachedLibraryDto(serverId)
            }

        database.deleteCachedLibrariesByServerId(serverId)
        if (cachedLibraries.isNotEmpty()) {
            database.insertCachedLibraries(cachedLibraries)
        }

        val seenMovieIds = mutableSetOf<UUID>()
        val seenShowIds = mutableSetOf<UUID>()
        val seenEpisodeIds = mutableSetOf<UUID>()
        val seenSeasonIdsByShow = mutableMapOf<UUID, MutableSet<UUID>>()
        var itemCount = 0

        for (library in cachedLibraries) {
            val libraryType = CollectionType.fromString(library.collectionType)
            val libraryItems = fetchLibraryItems(jellyfinApi, user.id, library.id, libraryType)
            database.deleteCachedLibraryItemsByLibraryId(serverId, library.id)
            if (libraryItems.isNotEmpty()) {
                database.insertCachedLibraryItems(
                    libraryItems.mapNotNull { it.toCachedLibraryItemDto(serverId, library.id) }
                )
            }
            itemCount += libraryItems.size
            syncDetailedMetadata(
                serverId = serverId,
                userId = user.id,
                items = libraryItems,
                seenMovieIds = seenMovieIds,
                seenShowIds = seenShowIds,
                seenEpisodeIds = seenEpisodeIds,
                seenSeasonIdsByShow = seenSeasonIdsByShow,
            )
        }

        cleanupStaleMetadata(
            serverId = serverId,
            seenMovieIds = seenMovieIds,
            seenShowIds = seenShowIds,
            seenEpisodeIds = seenEpisodeIds,
            seenSeasonIdsByShow = seenSeasonIdsByShow,
        )

        statusStore.markReady(cachedLibraries.size, itemCount)
        LibraryCacheSyncSummary(cachedLibraries.size, itemCount)
    }

    private suspend fun fetchLibraryItems(
        jellyfinApi: JellyfinApi,
        userId: UUID,
        libraryId: UUID,
        libraryType: CollectionType,
    ): List<BaseItemDto> {
        val includeTypes = includeTypesForLibrary(libraryType)
        val items = mutableListOf<BaseItemDto>()
        var startIndex = 0

        while (true) {
            val page =
                jellyfinApi.itemsApi
                    .getItems(
                        userId = userId,
                        parentId = libraryId,
                        includeItemTypes = includeTypes,
                        recursive = true,
                        sortBy = listOf(ItemSortBy.SORT_NAME),
                        sortOrder = listOf(ApiSortOrder.ASCENDING),
                        startIndex = startIndex,
                        limit = SYNC_PAGE_SIZE,
                    )
                    .content
                    .items

            if (page.isEmpty()) break

            items += page
            startIndex += page.size

            if (page.size < SYNC_PAGE_SIZE) break
        }

        return items
    }

    private fun includeTypesForLibrary(libraryType: CollectionType): List<BaseItemKind>? {
        return when (libraryType) {
            CollectionType.Movies ->
                listOf(
                    BaseItemKind.MOVIE,
                    BaseItemKind.VIDEO,
                    BaseItemKind.MUSIC_VIDEO,
                    BaseItemKind.TRAILER,
                )
            CollectionType.TvShows ->
                listOf(BaseItemKind.SERIES, BaseItemKind.SEASON, BaseItemKind.EPISODE)
            CollectionType.BoxSets ->
                listOf(BaseItemKind.BOX_SET, BaseItemKind.FOLDER, BaseItemKind.MOVIE)
            CollectionType.Music ->
                listOf(
                    BaseItemKind.FOLDER,
                    BaseItemKind.MUSIC_ARTIST,
                    BaseItemKind.MUSIC_ALBUM,
                    BaseItemKind.AUDIO,
                )
            CollectionType.MusicVideos ->
                listOf(BaseItemKind.FOLDER, BaseItemKind.MUSIC_VIDEO, BaseItemKind.VIDEO)
            CollectionType.HomeVideos ->
                listOf(
                    BaseItemKind.FOLDER,
                    BaseItemKind.VIDEO,
                    BaseItemKind.MOVIE,
                    BaseItemKind.PHOTO,
                    BaseItemKind.MUSIC_VIDEO,
                )
            CollectionType.Mixed,
            CollectionType.Folders ->
                listOf(
                    BaseItemKind.FOLDER,
                    BaseItemKind.MOVIE,
                    BaseItemKind.SERIES,
                    BaseItemKind.SEASON,
                    BaseItemKind.EPISODE,
                    BaseItemKind.BOX_SET,
                    BaseItemKind.VIDEO,
                    BaseItemKind.MUSIC_VIDEO,
                    BaseItemKind.MUSIC_ALBUM,
                    BaseItemKind.MUSIC_ARTIST,
                    BaseItemKind.AUDIO,
                    BaseItemKind.PHOTO,
                )
            else -> null
        }
    }

    private fun syncDetailedMetadata(
        serverId: String,
        userId: UUID,
        items: List<BaseItemDto>,
        seenMovieIds: MutableSet<UUID>,
        seenShowIds: MutableSet<UUID>,
        seenEpisodeIds: MutableSet<UUID>,
        seenSeasonIdsByShow: MutableMap<UUID, MutableSet<UUID>>,
    ) {
        items.sortedBy { syncPriority(it.type) }.forEach { item ->
            when (item.type) {
                BaseItemKind.MOVIE,
                BaseItemKind.VIDEO,
                BaseItemKind.MUSIC_VIDEO,
                BaseItemKind.AUDIO,
                BaseItemKind.TRAILER -> {
                    seenMovieIds += item.id
                    database.upsertMovie(item.toFindroidMovieDto(serverId))
                }
                BaseItemKind.SERIES -> {
                    seenShowIds += item.id
                    database.upsertShow(item.toFindroidShowDto(serverId))
                }
                BaseItemKind.SEASON -> {
                    val season = item.toFindroidSeasonDto() ?: return@forEach
                    seenSeasonIdsByShow.getOrPut(season.seriesId) { mutableSetOf() } += season.id
                    database.upsertSeason(season)
                }
                BaseItemKind.EPISODE -> {
                    val episode = item.toFindroidEpisodeDto(serverId) ?: return@forEach
                    seenEpisodeIds += episode.id
                    database.upsertEpisode(episode)
                }
                else -> Unit
            }

            val existingUserData = database.getUserData(item.id, userId)
            if (existingUserData?.toBeSynced != true) {
                database.insertUserData(item.toFindroidUserDataDto(userId))
            }
        }
    }

    private fun cleanupStaleMetadata(
        serverId: String,
        seenMovieIds: Set<UUID>,
        seenShowIds: Set<UUID>,
        seenEpisodeIds: Set<UUID>,
        seenSeasonIdsByShow: Map<UUID, Set<UUID>>,
    ) {
        database.getMoviesByServerId(serverId)
            .map { it.id }
            .filterNot(seenMovieIds::contains)
            .forEach(database::deleteMovie)

        database.getEpisodesByServerId(serverId)
            .map { it.id }
            .filterNot(seenEpisodeIds::contains)
            .forEach(database::deleteEpisode)

        database.getShowsByServerId(serverId)
            .map { it.id }
            .filterNot(seenShowIds::contains)
            .forEach(database::deleteShow)

        seenShowIds.forEach { showId ->
            val seasonIds = seenSeasonIdsByShow[showId].orEmpty()
            database.getSeasonsByShowId(showId)
                .map { it.id }
                .filterNot(seasonIds::contains)
                .forEach(database::deleteSeason)
        }
    }

    private fun syncPriority(kind: BaseItemKind?): Int {
        return when (kind) {
            BaseItemKind.SERIES -> 0
            BaseItemKind.SEASON -> 1
            BaseItemKind.EPISODE -> 2
            else -> 0
        }
    }

    private companion object {
        private const val SYNC_PAGE_SIZE = 200
    }
}

data class LibraryCacheSyncSummary(
    val libraryCount: Int,
    val itemCount: Int,
)
