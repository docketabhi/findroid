package dev.jdtech.jellyfin.models

import androidx.room.Entity
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

@Entity(tableName = "userdata", primaryKeys = ["userId", "itemId"])
data class FindroidUserDataDto(
    val userId: UUID,
    val itemId: UUID,
    val played: Boolean,
    val favorite: Boolean,
    val playbackPositionTicks: Long,
    val toBeSynced: Boolean = false,
)

fun FindroidItem.toFindroidUserDataDto(userId: UUID): FindroidUserDataDto {
    return FindroidUserDataDto(
        userId = userId,
        itemId = id,
        played = played,
        favorite = favorite,
        playbackPositionTicks = playbackPositionTicks,
    )
}

fun BaseItemDto.toFindroidUserDataDto(userId: UUID): FindroidUserDataDto {
    return FindroidUserDataDto(
        userId = userId,
        itemId = id,
        played = userData?.played == true,
        favorite = userData?.isFavorite == true,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
    )
}
