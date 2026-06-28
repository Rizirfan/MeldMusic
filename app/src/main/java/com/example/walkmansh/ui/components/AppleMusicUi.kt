package com.example.walkmansh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.walkmansh.data.model.Playlist
import com.example.walkmansh.data.model.Song
import com.example.walkmansh.playback.PlaybackManager
import com.example.walkmansh.playback.PlaybackState
import com.example.walkmansh.ui.main.WalkmanShViewModel
import androidx.compose.ui.res.painterResource
import com.example.walkmansh.R
import com.example.walkmansh.ui.player.FullPlayerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicUi(
    viewModel: WalkmanShViewModel,
    onOpenPlayer: () -> Unit,
    onTriggerVoiceSearch: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var activePlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val currentSong by PlaybackManager.currentSong.collectAsState()
    val playbackState by PlaybackManager.playbackState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Scaffold(
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                ) {
                    if (currentSong != null) {
                        MiniPlayer(
                            song = currentSong!!,
                            isPlaying = playbackState == PlaybackState.PLAYING,
                            onPlayPause = { PlaybackManager.togglePlayPause() },
                            onSkipNext = { PlaybackManager.skipNext() },
                            onSkipPrevious = { PlaybackManager.skipPrevious() },
                            onClick = onOpenPlayer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    MusicBottomNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it; activePlaylist = null }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (activePlaylist != null) {
                    PlaylistDetailScreen(
                        playlist = activePlaylist!!,
                        onBack = { activePlaylist = null },
                        onPlaySong = { song ->
                            viewModel.addSongToHistory(song)
                            PlaybackManager.play(song, activePlaylist!!.songs)
                        },
                        onDeletePlaylist = {
                            viewModel.deletePlaylist(activePlaylist!!.id)
                            activePlaylist = null
                        }
                    )
                } else {
                    when (selectedTab) {
                        0 -> HomeTab(
                            viewModel = viewModel,
                            onOpenSettings = { showSettingsDialog = true },
                            onPlaylistSelect = { activePlaylist = it }
                        )
                        1 -> SearchTab(viewModel, onTriggerVoiceSearch)
                        2 -> LibraryTab(
                            viewModel = viewModel,
                            onPlaylistSelect = { activePlaylist = it }
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SimpleSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun MusicBottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(painterResource(R.drawable.ic_home), "Home", 0),
                Triple(painterResource(R.drawable.ic_search), "Search", 1),
                Triple(painterResource(R.drawable.ic_library_music), "Library", 2)
            )
            tabs.forEach { (icon, label, index) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = label,
                        tint = if (selectedTab == index) primaryColor else inactiveColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) primaryColor else inactiveColor
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTab(
    viewModel: WalkmanShViewModel,
    onOpenSettings: () -> Unit,
    onPlaylistSelect: (Playlist) -> Unit
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val customPlaylists by viewModel.customPlaylists.collectAsState()

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Home",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isDark = when (themeMode) {
                        1 -> false
                        2 -> true
                        else -> androidx.compose.foundation.isSystemInDarkTheme()
                    }

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                viewModel.setThemeMode(if (isDark) 1 else 2)
                            },
                        shape = CircleShape,
                        color = surfaceColor.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = if (isDark) painterResource(R.drawable.ic_light_mode) else painterResource(R.drawable.ic_dark_mode),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onOpenSettings() },
                        shape = CircleShape,
                        color = surfaceColor.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Played",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(recentlyPlayed) { song ->
                        RecentSongCard(song = song, onClick = {
                            viewModel.addSongToHistory(song)
                            PlaybackManager.play(song, recentlyPlayed)
                        })
                    }
                }
            }
        }

        item {
            Text(
                text = "Your Playlists",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (customPlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No playlists found",
                        fontSize = 12.sp,
                        color = onSurfaceVariant
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(customPlaylists) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = {
                            onPlaylistSelect(playlist)
                        })
                    }
                }
            }
        }

        item {
            Text(
                text = "Liked Songs",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
        }

        if (likedSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No liked songs yet. Like a song to see it here!",
                        fontSize = 12.sp,
                        color = onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(likedSongs) { song ->
                SongListItem(
                    song = song,
                    onClick = {
                        viewModel.addSongToHistory(song)
                        PlaybackManager.play(song, likedSongs)
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { viewModel.toggleLike(song) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(
    viewModel: WalkmanShViewModel,
    onTriggerVoiceSearch: () -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Search",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent
        ) {
            TextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                placeholder = { Text("Artists, Songs, Lyrics", color = onSurfaceVariant, fontSize = 13.sp) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    Row {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }, modifier = Modifier.size(36.dp)) {
                                Icon(painterResource(R.drawable.ic_clear), contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = onTriggerVoiceSearch, modifier = Modifier.size(36.dp)) {
                            Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = surfaceColor.copy(alpha = 0.5f),
                    unfocusedContainerColor = surfaceColor.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = primaryColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.triggerSearch()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                })
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else if (searchError != null && viewModel.searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(R.drawable.ic_search_off), contentDescription = null, modifier = Modifier.size(40.dp), tint = onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(searchError!!, color = onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else if (searchResults.isEmpty()) {
            Column {
                Text(
                    text = "Browse Categories",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                val genres = listOf("Lofi Chill", "Synthwave", "Pop Hits", "Workout", "Rock Classics", "Acoustic Jazz", "Focus", "Mood Booster")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(genres) { genre ->
                        SuggestionChip(
                            onClick = {
                                viewModel.onSearchQueryChange(genre)
                                viewModel.triggerSearch()
                            },
                            label = { Text(genre, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = surfaceColor.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = null,
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Searches",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Clear",
                                fontSize = 12.sp,
                                color = primaryColor,
                                modifier = Modifier.clickable { viewModel.clearSearchHistory() }
                            )
                        }
                    }
                    items(searchHistory) { keyword ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onSearchQueryChange(keyword)
                                    viewModel.triggerSearch()
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.ic_history), contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(keyword, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                item {
                    Text(
                        text = "Trending Searches",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                val suggestions = listOf("Lofi study", "Synthwave", "Cartoon On & On", "Tobu hope", "Gym workout tracks")
                items(suggestions) { keyword ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.onSearchQueryChange(keyword)
                                viewModel.triggerSearch()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_trending_up), contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(keyword, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(searchResults) { song ->
                    SongListItem(song = song, onClick = {
                        viewModel.addSongToHistory(song)
                        PlaybackManager.play(song, searchResults)
                    })
                }
            }
        }
    }
}

@Composable
fun LibraryTab(
    viewModel: WalkmanShViewModel,
    onPlaylistSelect: (Playlist) -> Unit
) {
    val likedSongs by viewModel.likedSongs.collectAsState()
    val customPlaylists by viewModel.customPlaylists.collectAsState()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Library",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* navigate to liked songs */ }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_favorite),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Liked Songs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface
                    )
                    Text(
                        text = "${likedSongs.size} songs",
                        fontSize = 12.sp,
                        color = onSurfaceVariant
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        items(customPlaylists) { playlist ->
            var showDeleteConfirm by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistSelect(playlist) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Playlist",
                        fontSize = 12.sp,
                        color = onSurfaceVariant
                    )
                }
                if (playlist.id.startsWith("custom_")) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { showDeleteConfirm = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete Playlist", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to delete \"${playlist.name}\"?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deletePlaylist(playlist.id)
                            showDeleteConfirm = false
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            var showCreateDialog by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = onSurfaceVariant.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Create Playlist",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
            }

            if (showCreateDialog) {
                var newName by remember { mutableStateOf("") }
                var newDesc by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("New Playlist", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Playlist Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newDesc,
                                onValueChange = { newDesc = it },
                                label = { Text("Description") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.createPlaylist(newName, newDesc)
                                }
                                showCreateDialog = false
                            }
                        ) {
                            Text("Create", color = primaryColor)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = onSurfaceVariant)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SimpleSettingsDialog(
    viewModel: WalkmanShViewModel,
    onDismiss: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val songQuality by viewModel.songQuality.collectAsState()
    val fadeInOut by viewModel.fadeInOut.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Appearance", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("System", "Light", "Dark").forEachIndexed { index, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.setThemeMode(index) }
                        ) {
                            RadioButton(
                                selected = themeMode == index,
                                onClick = { viewModel.setThemeMode(index) },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                            Text(name, fontSize = 14.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Text("Song Quality", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Column {
                    listOf("Auto", "High", "Medium", "Low").forEachIndexed { index, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSongQuality(index) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = songQuality == index,
                                onClick = { viewModel.setSongQuality(index) },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                            Text(name, fontSize = 15.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Fade In/Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Crossfade between songs",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = fadeInOut,
                        onCheckedChange = { viewModel.setFadeInOut(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = primaryColor,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = if (viewModel.hasApiKey) "Active" else "Inactive"
                    val statusColor = if (viewModel.hasApiKey) Color(0xFF4CAF50) else Color(0xFFE53935)

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("YouTube API Status", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(statusText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                }

                Text(
                    text = "WalkmanSh v1.0",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = primaryColor)
            }
        }
    )
}

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onDeletePlaylist: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val errorColor = MaterialTheme.colorScheme.error

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBack() },
                    shape = CircleShape,
                    color = surfaceColor.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                            tint = onSurface
                        )
                    }
                }

                if (playlist.id.startsWith("custom_") && onDeletePlaylist != null) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onDeletePlaylist() },
                        shape = CircleShape,
                        color = errorColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                tint = errorColor
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(playlist.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Playlist", fontSize = 12.sp, color = onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    playlist.description,
                    fontSize = 14.sp,
                    color = onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { if (playlist.songs.isNotEmpty()) onPlaySong(playlist.songs[0]) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }

                OutlinedButton(
                    onClick = { if (playlist.songs.isNotEmpty()) onPlaySong(playlist.songs.shuffled()[0]) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_shuffle), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
        }

        items(playlist.songs) { song ->
            SongListItem(song = song, onClick = { onPlaySong(song) })
        }
    }
}

@Composable
fun PlaylistCard(playlist: Playlist, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = playlist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = playlist.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Playlist",
            fontSize = 12.sp,
            color = onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentSongCard(song: Song, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = song.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            fontSize = 11.sp,
            color = onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 12.sp,
                color = onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            IconButton(
                onClick = { /* More actions */ },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_horiz),
                    contentDescription = null,
                    tint = onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onClick: () -> Unit
) {
    val progress by PlaybackManager.progress.collectAsState()
    val duration by PlaybackManager.duration.collectAsState()
    val progressFraction = if (duration > 0) progress / duration else 0f
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    val context = LocalContext.current
    var gradientColors by remember(song.thumbnailUrl) { mutableStateOf(listOf<Color>()) }

    LaunchedEffect(song.thumbnailUrl) {
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(song.thumbnailUrl)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult && result.drawable is BitmapDrawable) {
                    gradientColors = extractColors(
                        (result.drawable as BitmapDrawable).bitmap
                    )
                }
            } catch (_: Exception) {}
        }
    }

    val bgGradient = if (gradientColors.size >= 2) {
        Brush.verticalGradient(gradientColors)
    } else null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, onSurface.copy(alpha = 0.08f))
    ) {
        Box {
            bgGradient?.let { grad ->
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(grad)
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(surfaceColor.copy(alpha = 0.55f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(onSurface.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(primaryColor)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            fontSize = 12.sp,
                            color = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onSkipPrevious, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_previous),
                            contentDescription = "Previous",
                            tint = onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onPlayPause() },
                        shape = CircleShape,
                        color = primaryColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = if (isPlaying) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play_arrow),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onSkipNext, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_next),
                            contentDescription = "Next",
                            tint = onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun extractColors(bitmap: Bitmap): List<Color> {
    val step = 4
    val w = bitmap.width
    val h = bitmap.height
    if (w == 0 || h == 0) return emptyList()

    val topColors = mutableListOf<Int>()
    val bottomColors = mutableListOf<Int>()

    for (y in 0 until h step step) {
        for (x in 0 until w step step) {
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 10) continue
            if (y < h / 2) topColors.add(pixel) else bottomColors.add(pixel)
        }
    }

    fun average(pixels: List<Int>): Color {
        if (pixels.isEmpty()) return Color.Transparent
        var r = 0L; var g = 0L; var b = 0L
        for (p in pixels) {
            r += android.graphics.Color.red(p)
            g += android.graphics.Color.green(p)
            b += android.graphics.Color.blue(p)
        }
        val n = pixels.size
        return Color(r / n / 255f, g / n / 255f, b / n / 255f)
    }

    val topAvg = average(topColors)
    val bottomAvg = average(bottomColors)
    return if (topAvg == Color.Transparent && bottomAvg == Color.Transparent) emptyList()
    else listOf(topAvg, bottomAvg)
}

@Composable
fun CompactSongThumbnail(song: Song, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(85.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(85.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            fontSize = 10.sp,
            color = onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
