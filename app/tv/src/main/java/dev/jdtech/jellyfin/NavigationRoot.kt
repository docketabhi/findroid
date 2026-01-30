package dev.jdtech.jellyfin

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidBoxSet
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.presentation.film.CollectionScreen
import dev.jdtech.jellyfin.presentation.film.DownloadsScreen
import dev.jdtech.jellyfin.presentation.film.EpisodeScreen
import dev.jdtech.jellyfin.presentation.film.FavoritesScreen
import dev.jdtech.jellyfin.presentation.film.LibraryScreen
import dev.jdtech.jellyfin.presentation.film.PersonScreen
import dev.jdtech.jellyfin.presentation.film.SeasonScreen
import dev.jdtech.jellyfin.presentation.film.ShowScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsSubScreen
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.servers.ServersScreen
import dev.jdtech.jellyfin.presentation.setup.users.UsersScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import dev.jdtech.jellyfin.ui.MainScreen
import dev.jdtech.jellyfin.ui.MovieScreen
import dev.jdtech.jellyfin.ui.PlayerScreen
import dev.jdtech.jellyfin.utils.base64ToByteArray
import dev.jdtech.jellyfin.utils.toBase64Str
import java.util.UUID
import kotlinx.parcelize.parcelableCreator
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemKind

inline fun <reified T : Parcelable> T.toBase64(): String {
    val parcel = Parcel.obtain()
    this.writeToParcel(parcel, 0)
    val bytearray = parcel.marshall()
    parcel.recycle()
    return bytearray.toBase64Str()
}

inline fun <reified T : Parcelable> String.base64ToParcelable(): T {
    val bytearray = this.base64ToByteArray()
    val parcel = Parcel.obtain()
    parcel.unmarshall(bytearray, 0, bytearray.size)
    parcel.setDataPosition(0)
    val item = parcelableCreator<T>().createFromParcel(parcel)
    return item
}

@Serializable data object WelcomeRoute

@Serializable data object ServersRoute

@Serializable data object AddServerRoute

@Serializable data object UsersRoute

@Serializable data class LoginRoute(val username: String? = null)

@Serializable data object MainRoute

@Serializable
data class LibraryRoute(
    val libraryId: String,
    val libraryName: String,
    val libraryType: CollectionType,
)

@Serializable data class MovieRoute(val itemId: String)

@Serializable data class ShowRoute(val itemId: String)

@Serializable data class SeasonRoute(val seasonId: String)

@Serializable data class PlayerRoute(val itemId: String, val itemKind: String)

@Serializable data object SettingsRoute

@Serializable data class SettingsSubRoute(val indexes: IntArray)

@Serializable data class EpisodeRoute(val episodeId: String)

@Serializable data class PersonRoute(val personId: String)

@Serializable data class CollectionRoute(val collectionId: String, val collectionName: String)

@Serializable data object FavoritesRoute

@Serializable data object DownloadsRoute

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun NavigationRoot(
    navController: NavHostController,
    hasServers: Boolean,
    hasCurrentServer: Boolean,
    hasCurrentUser: Boolean,
) {
    val startDestination =
        when {
            hasServers && hasCurrentServer && hasCurrentUser -> MainRoute
            hasServers && hasCurrentServer -> UsersRoute
            hasServers -> ServersRoute
            else -> WelcomeRoute
        }
    NavHost(navController = navController, startDestination = startDestination) {
        composable<WelcomeRoute> {
            WelcomeScreen(onContinueClick = { navController.navigate(ServersRoute) })
        }
        composable<ServersRoute> {
            ServersScreen(
                navigateToUsers = { navController.navigate(UsersRoute) },
                onAddClick = { navController.navigate(AddServerRoute) },
            )
        }
        composable<AddServerRoute> {
            AddServerScreen(onSuccess = { navController.navigate(UsersRoute) })
        }
        composable<UsersRoute> {
            UsersScreen(
                navigateToHome = {
                    navController.navigate(MainRoute) {
                        popUpTo(startDestination) { inclusive = true }
                    }
                },
                onChangeServerClick = {
                    navController.navigate(ServersRoute) {
                        popUpTo(ServersRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onAddClick = { navController.navigate(LoginRoute()) },
                onPublicUserClick = { username ->
                    navController.navigate(LoginRoute(username = username))
                },
            )
        }
        composable<LoginRoute> { backStackEntry ->
            val route: LoginRoute = backStackEntry.toRoute()
            LoginScreen(
                onSuccess = {
                    navController.navigate(MainRoute) {
                        popUpTo(startDestination) { inclusive = true }
                    }
                },
                onChangeServerClick = {
                    navController.navigate(ServersRoute) {
                        popUpTo(ServersRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                prefilledUsername = route.username,
            )
        }
        composable<MainRoute> {
            MainScreen(
                navigateToSettings = { navController.navigate(SettingsRoute) },
                navigateToLibrary = { libraryId, libraryName, libraryType ->
                    navController.navigate(
                        LibraryRoute(
                            libraryId = libraryId.toString(),
                            libraryName = libraryName,
                            libraryType = libraryType,
                        )
                    )
                },
                navigateToMovie = { itemId ->
                    navController.navigate(MovieRoute(itemId.toString()))
                },
                navigateToShow = { itemId -> navController.navigate(ShowRoute(itemId.toString())) },
                navigateToPlayer = { itemId, itemKind ->
                    navController.navigate(
                        PlayerRoute(itemId = itemId.toString(), itemKind = itemKind.serialName)
                    )
                },
            )
        }
        composable<LibraryRoute> { backStackEntry ->
            val route: LibraryRoute = backStackEntry.toRoute()
            LibraryScreen(
                libraryId = UUID.fromString(route.libraryId),
                libraryName = route.libraryName,
                libraryType = route.libraryType,
                navigateToLibrary = { libraryId, libraryName, libraryType ->
                    navController.navigate(
                        LibraryRoute(
                            libraryId = libraryId.toString(),
                            libraryName = libraryName,
                            libraryType = libraryType,
                        )
                    )
                },
                navigateToMovie = { itemId ->
                    navController.navigate(MovieRoute(itemId.toString()))
                },
                navigateToShow = { itemId -> navController.navigate(ShowRoute(itemId.toString())) },
            )
        }
        composable<CollectionRoute> { backStackEntry ->
            val route: CollectionRoute = backStackEntry.toRoute()
            CollectionScreen(
                collectionId = UUID.fromString(route.collectionId),
                collectionName = route.collectionName,
                onItemClick = { item ->
                    navigateToItem(navController = navController, item = item)
                },
            )
        }
        composable<MovieRoute> { backStackEntry ->
            val route: MovieRoute = backStackEntry.toRoute()
            MovieScreen(
                movieId = UUID.fromString(route.itemId),
                navigateToPlayer = { itemId ->
                    navController.navigate(
                        PlayerRoute(
                            itemId = itemId.toString(),
                            itemKind = BaseItemKind.MOVIE.serialName,
                        )
                    )
                },
            )
        }
        composable<ShowRoute> { backStackEntry ->
            val route: ShowRoute = backStackEntry.toRoute()
            ShowScreen(
                showId = UUID.fromString(route.itemId),
                navigateToItem = { item ->
                    when (item) {
                        is FindroidSeason -> {
                            navController.navigate(SeasonRoute(seasonId = item.id.toString()))
                        }
                    }
                },
                navigateToPerson = { personId ->
                    navController.navigate(PersonRoute(personId = personId.toString()))
                },
                navigateToPlayer = { itemId ->
                    navController.navigate(
                        PlayerRoute(
                            itemId = itemId.toString(),
                            itemKind = BaseItemKind.SERIES.serialName,
                        )
                    )
                },
            )
        }
        composable<SeasonRoute> { backStackEntry ->
            val route: SeasonRoute = backStackEntry.toRoute()
            SeasonScreen(
                seasonId = UUID.fromString(route.seasonId),
                navigateToEpisode = { episodeId ->
                    navController.navigate(EpisodeRoute(episodeId = episodeId.toString()))
                },
            )
        }
        composable<EpisodeRoute> { backStackEntry ->
            val route: EpisodeRoute = backStackEntry.toRoute()
            EpisodeScreen(
                episodeId = UUID.fromString(route.episodeId),
                navigateToPlayer = { itemId ->
                    navController.navigate(
                        PlayerRoute(
                            itemId = itemId.toString(),
                            itemKind = BaseItemKind.EPISODE.serialName,
                        )
                    )
                },
            )
        }
        composable<PersonRoute> { backStackEntry ->
            val route: PersonRoute = backStackEntry.toRoute()
            PersonScreen(
                personId = UUID.fromString(route.personId),
                navigateToItem = { item ->
                    when (item) {
                        is FindroidMovie -> {
                            navController.navigate(MovieRoute(itemId = item.id.toString()))
                        }
                        is FindroidShow -> {
                            navController.navigate(ShowRoute(itemId = item.id.toString()))
                        }
                    }
                },
            )
        }
        composable<PlayerRoute> { backStackEntry ->
            val route: PlayerRoute = backStackEntry.toRoute()
            PlayerScreen(
                itemId = UUID.fromString(route.itemId),
                itemKind = route.itemKind,
                startFromBeginning = false,
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                navigateToUsers = { navController.navigate(UsersRoute) },
                navigateToServers = { navController.navigate(ServersRoute) },
                navigateToSubSettings = { indexes ->
                    navController.navigate(SettingsSubRoute(indexes = indexes))
                },
            )
        }
        composable<SettingsSubRoute> { backStackEntry ->
            val route: SettingsSubRoute = backStackEntry.toRoute()
            SettingsSubScreen(
                indexes = route.indexes,
                navigateToUsers = { navController.navigate(UsersRoute) },
                navigateToServers = { navController.navigate(ServersRoute) },
                navigateToSubSettings = { indexes ->
                    navController.navigate(SettingsSubRoute(indexes = indexes))
                },
            )
        }
        composable<FavoritesRoute> {
            FavoritesScreen(
                onItemClick = { item ->
                    navigateToItem(navController = navController, item = item)
                },
            )
        }
        composable<DownloadsRoute> {
            DownloadsScreen(
                onItemClick = { item ->
                    navigateToItem(navController = navController, item = item)
                },
            )
        }
    }
}

private fun navigateToItem(navController: NavHostController, item: FindroidItem) {
    when (item) {
        is FindroidBoxSet ->
            navController.navigate(
                CollectionRoute(collectionId = item.id.toString(), collectionName = item.name)
            )
        is FindroidMovie -> navController.navigate(MovieRoute(itemId = item.id.toString()))
        is FindroidShow -> navController.navigate(ShowRoute(itemId = item.id.toString()))
        is FindroidSeason -> navController.navigate(SeasonRoute(seasonId = item.id.toString()))
        else -> Unit
    }
}
