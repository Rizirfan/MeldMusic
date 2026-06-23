package com.example.walkmansh.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.walkmansh.data.model.Song
import com.example.walkmansh.playback.PlaybackManager
import com.example.walkmansh.playback.PlaybackState
import com.example.walkmansh.playback.RepeatMode
import com.example.walkmansh.ui.main.WalkmanShViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    onDismiss: () -> Unit,
    viewModel: WalkmanShViewModel = viewModel(),
    showCollapseButton: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColorSecondary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    val currentSong by PlaybackManager.currentSong.collectAsState()
    val playbackState by PlaybackManager.playbackState.collectAsState()
    val progress by PlaybackManager.progress.collectAsState()
    val duration by PlaybackManager.duration.collectAsState()
    val queue by PlaybackManager.queue.collectAsState()

    val isShuffleEnabled by PlaybackManager.isShuffleEnabled.collectAsState()
    val repeatMode by PlaybackManager.repeatMode.collectAsState()
    val sleepRemaining by PlaybackManager.sleepTimerRemaining.collectAsState()
    val isSleepActive by PlaybackManager.isSleepTimerActive.collectAsState()

    val likedSongs by viewModel.likedSongs.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    var showQueue by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No track playing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select a song from Search to start playing.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val song = currentSong!!
    val isPlaying = playbackState == PlaybackState.PLAYING
    val isLiked = likedSongs.any { it.id == song.id }

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.85f,
        animationSpec = tween(durationMillis = 300),
        label = "ArtworkScale"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        com.example.walkmansh.ui.components.PremiumDynamicBackground(isDark = isDark)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.04f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showCollapseButton) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse Player",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Text(
                    text = "PLAYING FROM YOUTUBE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColorSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Middle Section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showQueue) {
                    QueuePanel(
                        queue = queue,
                        currentSong = song,
                        onRemove = { PlaybackManager.removeFromQueue(it) }
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .sizeIn(maxWidth = 180.dp, maxHeight = 180.dp)
                            .aspectRatio(1f)
                            .scale(artworkScale)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Bottom Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            fontSize = 13.sp,
                            color = textColorSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { viewModel.toggleLike(song) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Song",
                            tint = if (isLiked) primaryColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                var sliderSeekingValue by remember { mutableStateOf<Float?>(null) }
                val displayProgress = sliderSeekingValue ?: progress
                val progressFraction = if (duration > 0) displayProgress / duration else 0f

                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = progressFraction,
                        onValueChange = { fraction ->
                            sliderSeekingValue = fraction * duration
                        },
                        onValueChangeFinished = {
                            sliderSeekingValue?.let { seekTarget ->
                                PlaybackManager.seekTo(seekTarget)
                            }
                            sliderSeekingValue = null
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(displayProgress.toInt()),
                            fontSize = 12.sp,
                            color = textColorSecondary
                        )
                        val remainingSeconds = max(0, (duration - displayProgress).toInt())
                        Text(
                            text = "-" + formatTime(remainingSeconds),
                            fontSize = 12.sp,
                            color = textColorSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Playback Navigation controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { PlaybackManager.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleEnabled) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(onClick = { PlaybackManager.skipPrevious() }) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous Song",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { PlaybackManager.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                        Icon(
                            imageVector = icon,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = { PlaybackManager.skipNext() }) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Song",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = { PlaybackManager.toggleRepeat() }) {
                        val repeatIcon = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        }
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repeat Mode",
                            tint = if (repeatMode != RepeatMode.OFF) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Row: Sleep Timer, Add to Playlist, Queue
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showSleepDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (isSleepActive) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (isSleepActive) {
                            Text(
                                text = formatTime(sleepRemaining),
                                fontSize = 8.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(y = 12.dp)
                            )
                        }
                    }

                    IconButton(onClick = { showAddToPlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(onClick = { showQueue = !showQueue }) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = "Queue Toggle",
                            tint = if (showQueue) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

    // Include dialogs
    if (showSleepDialog) {
        SleepTimerDialog(
            isSleepActive = isSleepActive,
            onDismiss = { showSleepDialog = false }
        )
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}

@Composable
fun CompactPlayerCard(
    viewModel: WalkmanShViewModel = viewModel()
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColorSecondary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    val currentSong by PlaybackManager.currentSong.collectAsState()
    val playbackState by PlaybackManager.playbackState.collectAsState()
    val progress by PlaybackManager.progress.collectAsState()
    val duration by PlaybackManager.duration.collectAsState()
    val queue by PlaybackManager.queue.collectAsState()

    val isShuffleEnabled by PlaybackManager.isShuffleEnabled.collectAsState()
    val repeatMode by PlaybackManager.repeatMode.collectAsState()
    val sleepRemaining by PlaybackManager.sleepTimerRemaining.collectAsState()
    val isSleepActive by PlaybackManager.isSleepTimerActive.collectAsState()

    val likedSongs by viewModel.likedSongs.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No track playing",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        return
    }

    val song = currentSong!!
    val isPlaying = playbackState == PlaybackState.PLAYING
    val isLiked = likedSongs.any { it.id == song.id }

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.9f,
        animationSpec = tween(durationMillis = 300),
        label = "ArtworkScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upper row: Artwork, titles, like button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .size(80.dp)
                        .scale(artworkScale)
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        fontSize = 12.sp,
                        color = textColorSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { viewModel.toggleLike(song) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Song",
                        tint = if (isLiked) primaryColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Optional Queue panel inside card
            if (showQueue) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                ) {
                    QueuePanel(
                        queue = queue,
                        currentSong = song,
                        onRemove = { PlaybackManager.removeFromQueue(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Slider
            var sliderSeekingValue by remember { mutableStateOf<Float?>(null) }
            val displayProgress = sliderSeekingValue ?: progress
            val progressFraction = if (duration > 0) displayProgress / duration else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progressFraction,
                    onValueChange = { fraction ->
                        sliderSeekingValue = fraction * duration
                    },
                    onValueChangeFinished = {
                        sliderSeekingValue?.let { seekTarget ->
                            PlaybackManager.seekTo(seekTarget)
                        }
                        sliderSeekingValue = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(displayProgress.toInt()),
                        fontSize = 11.sp,
                        color = textColorSecondary
                    )
                    val remainingSeconds = max(0, (duration - displayProgress).toInt())
                    Text(
                        text = "-" + formatTime(remainingSeconds),
                        fontSize = 11.sp,
                        color = textColorSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { PlaybackManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleEnabled) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.skipPrevious() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { PlaybackManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    val icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                    Icon(
                        imageVector = icon,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.skipNext() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.toggleRepeat() }) {
                    val repeatIcon = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showSleepDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sleep Timer",
                        tint = if (isSleepActive) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { showAddToPlaylistDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { showQueue = !showQueue }) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = "Queue Toggle",
                        tint = if (showQueue) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Include Dialogs
    if (showSleepDialog) {
        SleepTimerDialog(
            isSleepActive = isSleepActive,
            onDismiss = { showSleepDialog = false }
        )
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}

@Composable
fun SleepTimerDialog(
    isSleepActive: Boolean,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                listOf(
                    Pair(5, "5 minutes"),
                    Pair(15, "15 minutes"),
                    Pair(30, "30 minutes"),
                    Pair(60, "1 hour")
                ).forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PlaybackManager.startSleepTimer(pair.first)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(text = pair.second, fontSize = 16.sp)
                    }
                }
                if (isSleepActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PlaybackManager.cancelSleepTimer()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(text = "Turn off timer", fontSize = 16.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    song: Song,
    viewModel: WalkmanShViewModel,
    onDismiss: () -> Unit
) {
    val customPlaylists by viewModel.customPlaylists.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = primaryColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Playlist", color = primaryColor)
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (customPlaylists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No custom playlists yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    ) {
                        itemsIndexed(customPlaylists) { _, playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, song)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = primaryColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(playlist.name, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                TextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Playlist Name") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = primaryColor
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName, "Custom user playlist")
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun QueuePanel(
    queue: List<Song>,
    currentSong: Song,
    onRemove: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColorSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(12.dp)
    ) {
        Text(
            text = "Playing Next",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Queue is empty", color = textColorSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(queue) { _, song ->
                    val isPlaying = song.id == currentSong.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPlaying) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) else Color.Transparent)
                            .clickable { PlaybackManager.play(song) }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = if (isPlaying) primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = textColorSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isPlaying) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Playing", tint = primaryColor, modifier = Modifier.size(18.dp))
                        } else {
                            IconButton(onClick = { onRemove(song.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = textColorSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
