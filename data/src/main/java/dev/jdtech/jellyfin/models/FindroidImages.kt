package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

data class FindroidImages(
    val primary: Uri? = null,
    val backdrop: Uri? = null,
    val logo: Uri? = null,
    val showPrimary: Uri? = null,
    val showBackdrop: Uri? = null,
    val showLogo: Uri? = null,
)

fun BaseItemDto.toFindroidImages(jellyfinRepository: JellyfinRepository): FindroidImages {
    return buildRemoteFindroidImages(
        baseUrl = jellyfinRepository.getBaseUrl(),
        itemId = id,
        primaryImageTag = imageTags?.get(ImageType.PRIMARY),
        backdropImageTag = backdropImageTags?.firstOrNull(),
        logoImageTag = imageTags?.get(ImageType.LOGO),
        seriesId = seriesId,
        seriesPrimaryImageTag = seriesPrimaryImageTag,
    )
}

fun buildRemoteFindroidImages(
    baseUrl: String,
    itemId: UUID,
    primaryImageTag: String? = null,
    backdropImageTag: String? = null,
    logoImageTag: String? = null,
    seriesId: UUID? = null,
    seriesPrimaryImageTag: String? = null,
): FindroidImages {
    val parsedBaseUrl = Uri.parse(baseUrl)
    val primary =
        primaryImageTag?.let { tag ->
            parsedBaseUrl
                .buildUpon()
                .appendEncodedPath("items/$itemId/Images/${ImageType.PRIMARY}")
                .appendQueryParameter("maxWidth", PRIMARY_MAX_WIDTH.toString())
                .appendQueryParameter("quality", IMAGE_QUALITY.toString())
                .appendQueryParameter("tag", tag)
                .build()
        }
    val backdrop =
        backdropImageTag?.let { tag ->
            parsedBaseUrl
                .buildUpon()
                .appendEncodedPath("items/$itemId/Images/${ImageType.BACKDROP}/0")
                .appendQueryParameter("maxWidth", BACKDROP_MAX_WIDTH.toString())
                .appendQueryParameter("quality", IMAGE_QUALITY.toString())
                .appendQueryParameter("tag", tag)
                .build()
        }
    val logo =
        logoImageTag?.let { tag ->
            parsedBaseUrl
                .buildUpon()
                .appendEncodedPath("items/$itemId/Images/${ImageType.LOGO}")
                .appendQueryParameter("maxWidth", LOGO_MAX_WIDTH.toString())
                .appendQueryParameter("quality", IMAGE_QUALITY.toString())
                .appendQueryParameter("tag", tag)
                .build()
        }
    val showPrimary =
        if (seriesId != null) {
            seriesPrimaryImageTag?.let { tag ->
                parsedBaseUrl
                    .buildUpon()
                    .appendEncodedPath("items/$seriesId/Images/${ImageType.PRIMARY}")
                    .appendQueryParameter("maxWidth", PRIMARY_MAX_WIDTH.toString())
                    .appendQueryParameter("quality", IMAGE_QUALITY.toString())
                    .appendQueryParameter("tag", tag)
                    .build()
            }
        } else {
            null
        }
    val showBackdrop =
        if (seriesId != null) {
            seriesPrimaryImageTag?.let { tag ->
                parsedBaseUrl
                    .buildUpon()
                    .appendEncodedPath("items/$seriesId/Images/${ImageType.BACKDROP}/0")
                    .appendQueryParameter("maxWidth", BACKDROP_MAX_WIDTH.toString())
                    .appendQueryParameter("quality", IMAGE_QUALITY.toString())
                    .appendQueryParameter("tag", tag)
                    .build()
            }
        } else {
            null
        }
    val showLogo =
        if (seriesId != null) {
            seriesPrimaryImageTag?.let { tag ->
                parsedBaseUrl
                    .buildUpon()
                    .appendEncodedPath("items/$seriesId/Images/${ImageType.LOGO}")
                    .appendQueryParameter("maxWidth", LOGO_MAX_WIDTH.toString())
                    .appendQueryParameter("quality", IMAGE_QUALITY.toString())
                    .appendQueryParameter("tag", tag)
                    .build()
            }
        } else {
            null
        }

    return FindroidImages(
        primary = primary,
        backdrop = backdrop,
        logo = logo,
        showPrimary = showPrimary,
        showBackdrop = showBackdrop,
        showLogo = showLogo,
    )
}

private const val PRIMARY_MAX_WIDTH = 500
private const val BACKDROP_MAX_WIDTH = 1280
private const val LOGO_MAX_WIDTH = 800
private const val IMAGE_QUALITY = 80

fun FindroidMovieDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
    )
}

fun FindroidShowDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
    )
}

fun FindroidSeasonDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        showPrimary = Uri.Builder().appendEncodedPath("images/$seriesId/primary").build(),
        showBackdrop = Uri.Builder().appendEncodedPath("images/$seriesId/backdrop").build(),
    )
}

fun FindroidEpisodeDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        showPrimary = Uri.Builder().appendEncodedPath("images/$seriesId/primary").build(),
        showBackdrop = Uri.Builder().appendEncodedPath("images/$seriesId/backdrop").build(),
    )
}
