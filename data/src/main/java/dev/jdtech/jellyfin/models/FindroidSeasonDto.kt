package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

@Entity(
    tableName = "seasons",
    foreignKeys =
        [
            ForeignKey(
                entity = FindroidShowDto::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("seriesId"),
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("seriesId")],
)
data class FindroidSeasonDto(
    @PrimaryKey val id: UUID,
    val seriesId: UUID,
    val name: String,
    val seriesName: String,
    val overview: String,
    val indexNumber: Int,
)

fun FindroidSeason.toFindroidSeasonDto(): FindroidSeasonDto {
    return FindroidSeasonDto(
        id = id,
        seriesId = seriesId,
        name = name,
        seriesName = seriesName,
        overview = overview,
        indexNumber = indexNumber,
    )
}

fun BaseItemDto.toFindroidSeasonDto(): FindroidSeasonDto? {
    val showId = seriesId ?: return null
    return FindroidSeasonDto(
        id = id,
        seriesId = showId,
        name = name.orEmpty(),
        seriesName = seriesName.orEmpty(),
        overview = overview.orEmpty(),
        indexNumber = indexNumber ?: 0,
    )
}
