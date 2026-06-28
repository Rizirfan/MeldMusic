package com.example.walkmansh.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.walkmansh.data.model.Song
import com.example.walkmansh.playback.PlaybackManager
import com.example.walkmansh.playback.PlaybackState
import com.example.walkmansh.playback.RepeatMode
import com.example.walkmansh.ui.main.WalkmanShViewModel
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private data class WaveBar(val heightFactor: Float, val phase: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    onDismiss: () -> Unit,
    viewModel: WalkmanShViewModel = viewModel(),
    showCollapseButton: Boolean = true
) {
    val currentSong by PlaybackManager.currentSong.collectAsState()
    val playbackState by PlaybackManager.playbackState.collectAsState()
    val progress by PlaybackManager.progress.collectAsState()
    val duration by PlaybackManager.duration.collectAsState()
    val queue by PlaybackManager.queue.collectAsState()

    val isShuffleEnabled by PlaybackManager.isShuffleEnabled.collectAsState()
    val repeatMode by PlaybackManager.repeatMode.collectAsState()
    val sleepRemaining by PlaybackManager.sleepTimerRemaining.collectAsState()
    val isSleepActive by PlaybackManager.isSleepTimerActive.collectAsState()
    val playbackSpeed by PlaybackManager.playbackSpeed.collectAsState()

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
    var showSpeedDialog by remember { mutableStateOf(false) }

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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    val song = currentSong!!
    val isPlaying = playbackState == PlaybackState.PLAYING
    val isLiked = likedSongs.any { it.id == song.id }

    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val snapAnim = remember { Animatable(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
    val displayOffset = if (snapAnim.isRunning) snapAnim.value else dragOffset
    val contentAlpha = (1f - (displayOffset / swipeThresholdPx).coerceIn(0f, 1f) * 0.4f).coerceIn(0f, 1f)

    var sliderSeekingValue by remember { mutableStateOf<Float?>(null) }
    val displayProgress = sliderSeekingValue ?: progress
    val progressFraction = if (duration > 0) displayProgress / duration else 0f

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "artworkScale"
    )

    val waveAnim by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f, targetValue = (2.0 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Restart),
        label = "wave"
    )

    val waveBars = remember(song) {
        val random = java.util.Random(song.id.hashCode().toLong())
        List(48) { i -> WaveBar(0.3f + random.nextFloat() * 0.7f, i * 0.3f) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .offset { IntOffset(0, displayOffset.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dragOffset > swipeThresholdPx) {
                            onDismiss()
                        } else {
                            scope.launch { snapAnim.animateTo(0f, spring()) }
                        }
                    },
                    onDragCancel = {
                        scope.launch { snapAnim.animateTo(0f, spring()) }
                    }
                )
            }
    ) {
        com.example.walkmansh.ui.components.PremiumDynamicBackground(isDark = isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .graphicsLayer(alpha = contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .statusBarsPadding()
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            )

            Spacer(modifier = Modifier.weight(0.08f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .aspectRatio(1f)
                    .scale(artworkScale)
            ) {
                if (showQueue) {
                    QueuePanel(
                        queue = queue,
                        currentSong = song,
                        onRemove = { PlaybackManager.removeFromQueue(it) }
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxSize()
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

            Spacer(modifier = Modifier.weight(0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.toggleLike(song) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = song.artist,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            WaveformProgressBar(
                progressFraction = progressFraction,
                isPlaying = isPlaying,
                waveAnim = waveAnim,
                waveBars = waveBars,
                accentColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                onSeek = { fraction ->
                    sliderSeekingValue = fraction * duration
                },
                onSeekFinished = { fraction ->
                    PlaybackManager.seekTo(fraction * duration)
                    sliderSeekingValue = null
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(displayProgress.toInt()),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(duration.toInt()),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { PlaybackManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.skipPrevious() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { PlaybackManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.skipNext() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.toggleRepeat() }) {
                    val repeatIcon = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = null,
                        tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showSpeedDialog = true }) {
                    Text(
                        text = String.format("%.1fx", playbackSpeed),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = { showSleepDialog = true }) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isSleepActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                        if (isSleepActive) {
                            Text(
                                text = formatTime(sleepRemaining),
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(y = 12.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = { showQueue = !showQueue }) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = if (showQueue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { showAddToPlaylistDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepDialog = false },
            onSetTimer = { minutes ->
                PlaybackManager.startSleepTimer(minutes)
                showSleepDialog = false
            }
        )
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                Column {
                    listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PlaybackManager.setSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%.1fx", speed),
                                fontSize = 16.sp,
                                fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal,
                                color = if (speed == playbackSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Cancel") }
            }
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
private fun WaveformProgressBar(
    progressFraction: Float,
    isPlaying: Boolean,
    waveAnim: Float,
    waveBars: List<WaveBar>,
    accentColor: Color,
    inactiveColor: Color,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                var lastFraction = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        lastFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(lastFraction)
                    },
                    onHorizontalDrag = { change, _ ->
                        lastFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(lastFraction)
                    },
                    onDragEnd = {
                        onSeekFinished(lastFraction)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount = waveBars.size
            val spacing = 2.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (size.width - totalSpacing) / barCount
            val canvasHeight = size.height

            waveBars.forEachIndexed { index, waveBar ->
                val baseHeight = waveBar.heightFactor * canvasHeight * 0.45f
                val pulseHeight = if (isPlaying) {
                    sin(waveAnim + waveBar.phase) * 4f
                } else {
                    0f
                }
                val barHeight = (baseHeight + pulseHeight).coerceAtLeast(4f)
                val x = index * (barWidth + spacing)
                val color = if (index / barCount.toFloat() <= progressFraction) accentColor else inactiveColor

                drawRect(
                    color = color,
                    topLeft = Offset(x, canvasHeight - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                listOf(
                    15 to "15 minutes",
                    30 to "30 minutes",
                    45 to "45 minutes",
                    60 to "60 minutes"
                ).forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimer(minutes) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(text = label, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
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
            text = "Queue",
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
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                        } else {
                            IconButton(onClick = { onRemove(song.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = textColorSecondary, modifier = Modifier.size(16.dp))
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
