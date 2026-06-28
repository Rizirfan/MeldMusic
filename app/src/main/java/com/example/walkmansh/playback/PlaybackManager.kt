package com.example.walkmansh.playback

import com.example.walkmansh.data.model.Song
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode {
    OFF, ONE, ALL
}

object PlaybackManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var sleepTimerJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    // --- Advanced Playback States ---
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow(0) // seconds
    val sleepTimerRemaining: StateFlow<Int> = _sleepTimerRemaining.asStateFlow()

    private val _isSleepTimerActive = MutableStateFlow(false)
    val isSleepTimerActive: StateFlow<Boolean> = _isSleepTimerActive.asStateFlow()

    private val _needsInit = MutableStateFlow(false)
    val needsInit: StateFlow<Boolean> = _needsInit.asStateFlow()

    private var currentQueueIndex = 0
    private var player: YouTubePlayer? = null
    
    // Pending playback: requested while player was not ready
    private var pendingPlayback: Pair<Song, List<Song>>? = null
    
    // Tracks current original queue (needed when shuffle is toggled on/off)
    private var originalQueue: List<Song> = emptyList()

    // Volume crossfade tracking
    private var targetVolume = 100
    private var crossfadeJob: Job? = null

    fun initializePlayer(youtubePlayer: YouTubePlayer) {
        android.util.Log.d("WalkmanSh", "PlaybackManager initializePlayer called")
        if (this.player == youtubePlayer) {
            android.util.Log.d("WalkmanSh", "Player instance is same, skip listener registration")
            return
        }
        this.player = youtubePlayer

        youtubePlayer.addListener(object : AbstractYouTubePlayerListener() {
            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                android.util.Log.d("WalkmanSh", "YouTube player state change: $state")
                when (state) {
                    PlayerConstants.PlayerState.PLAYING -> {
                        _playbackState.value = PlaybackState.PLAYING
                        // Restore speed on state change to ensure YouTube doesn't override it
                        setSpeed(_playbackSpeed.value)
                        triggerFadeIn()
                    }
                    PlayerConstants.PlayerState.PAUSED -> _playbackState.value = PlaybackState.PAUSED
                    PlayerConstants.PlayerState.BUFFERING -> _playbackState.value = PlaybackState.BUFFERING
                    PlayerConstants.PlayerState.ENDED -> {
                        _playbackState.value = PlaybackState.ENDED
                        handleSongEnded()
                    }
                    else -> _playbackState.value = PlaybackState.IDLE
                }
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                _progress.value = second
                checkCrossfadeTrigger(second)
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                _duration.value = duration
                _currentSong.value?.let { song ->
                    if (song.durationSeconds != duration.toInt()) {
                        song.durationSeconds = duration.toInt()
                    }
                }
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                android.util.Log.e("WalkmanSh", "YouTube Player Error: $error")
            }
        })
    }

    private fun checkCrossfadeTrigger(currentSecond: Float) {
        val dur = _duration.value
        if (dur > 0 && (dur - currentSecond) <= 3.0f && (dur - currentSecond) > 0f) {
            // Video is within last 3 seconds, fade out
            if (crossfadeJob == null || !crossfadeJob!!.isActive) {
                triggerFadeOut()
            }
        }
    }

    private fun triggerFadeIn() {
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            var vol = 0
            while (vol <= 100) {
                player?.setVolume(vol)
                vol += 10
                delay(100)
            }
            player?.setVolume(100)
        }
    }

    private fun triggerFadeOut() {
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            var vol = 100
            while (vol >= 0) {
                player?.setVolume(vol)
                vol -= 15
                delay(100)
            }
            player?.setVolume(0)
        }
    }

    private fun handleSongEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Play same video again
                _currentSong.value?.let { play(it) }
            }
            else -> {
                skipNext()
            }
        }
    }

    fun play(song: Song, newQueue: List<Song> = emptyList()) {
        if (newQueue.isNotEmpty()) {
            originalQueue = newQueue
            if (_isShuffleEnabled.value) {
                val shuffled = newQueue.shuffled().toMutableList()
                shuffled.remove(song)
                shuffled.add(0, song) // keep clicked song first
                _queue.value = shuffled
                currentQueueIndex = 0
            } else {
                _queue.value = newQueue
                currentQueueIndex = newQueue.indexOfFirst { it.id == song.id }
            }
        } else if (!_queue.value.any { it.id == song.id }) {
            val updatedQueue = _queue.value.toMutableList().apply { add(song) }
            _queue.value = updatedQueue
            originalQueue = originalQueue.toMutableList().apply { add(song) }
            currentQueueIndex = updatedQueue.size - 1
        } else {
            currentQueueIndex = _queue.value.indexOfFirst { it.id == song.id }
        }

        _currentSong.value = song
        _progress.value = 0f
        _duration.value = song.durationSeconds.toFloat()

        if (player != null) {
            player!!.setVolume(100)
            player!!.loadVideo(song.id, 0f)
            _playbackState.value = PlaybackState.PLAYING
        } else {
            pendingPlayback = song to newQueue
            _needsInit.value = true
        }
    }

    fun onPlayerReady(player: YouTubePlayer) {
        initializePlayer(player)
        _needsInit.value = false
        pendingPlayback?.let { (song, _) ->
            pendingPlayback = null
            player.loadVideo(song.id, 0f)
            _playbackState.value = PlaybackState.PLAYING
        }
    }

    fun togglePlayPause() {
        val currentState = _playbackState.value
        player?.let { p ->
            if (currentState == PlaybackState.PLAYING) {
                p.pause()
                _playbackState.value = PlaybackState.PAUSED
            } else if (currentState == PlaybackState.PAUSED || currentState == PlaybackState.IDLE || currentState == PlaybackState.ENDED) {
                p.play()
                _playbackState.value = PlaybackState.PLAYING
            }
        }
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (currentQueueIndex == q.size - 1) {
            if (_repeatMode.value == RepeatMode.ALL) {
                currentQueueIndex = 0
            } else {
                // RepeatMode.OFF, stop playback
                player?.pause()
                _playbackState.value = PlaybackState.IDLE
                return
            }
        } else {
            currentQueueIndex += 1
        }

        play(q[currentQueueIndex])
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (currentQueueIndex == 0) {
            currentQueueIndex = q.size - 1
        } else {
            currentQueueIndex -= 1
        }

        play(q[currentQueueIndex])
    }

    fun seekTo(seconds: Float) {
        player?.seekTo(seconds)
        _progress.value = seconds
    }

    fun setQueue(songs: List<Song>) {
        originalQueue = songs
        if (_isShuffleEnabled.value) {
            _queue.value = songs.shuffled()
        } else {
            _queue.value = songs
        }
        currentQueueIndex = _currentSong.value?.let { current -> _queue.value.indexOfFirst { it.id == current.id } } ?: -1
    }

    // --- Shuffle & Repeat Toggles ---
    fun toggleShuffle() {
        val newShuffle = !_isShuffleEnabled.value
        _isShuffleEnabled.value = newShuffle
        val current = _currentSong.value

        if (newShuffle) {
            // Shuffle active queue, keeping current song at current position or first
            val list = originalQueue.toMutableList()
            if (current != null) {
                list.remove(current)
                val shuffled = list.shuffled().toMutableList()
                shuffled.add(0, current)
                _queue.value = shuffled
                currentQueueIndex = 0
            } else {
                _queue.value = originalQueue.shuffled()
                currentQueueIndex = -1
            }
        } else {
            // Restore original order
            _queue.value = originalQueue
            currentQueueIndex = current?.let { originalQueue.indexOfFirst { song -> song.id == it.id } } ?: -1
        }
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
    }

    // --- Playback Speed Control ---
    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val rateEnum = when (speed) {
            0.5f -> PlayerConstants.PlaybackRate.RATE_0_5
            1.5f -> PlayerConstants.PlaybackRate.RATE_1_5
            2.0f -> PlayerConstants.PlaybackRate.RATE_2
            else -> PlayerConstants.PlaybackRate.RATE_1
        }
        player?.setPlaybackRate(rateEnum)
    }

    // --- Sleep Timer Implementation ---
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _isSleepTimerActive.value = false
            _sleepTimerRemaining.value = 0
            return
        }
        _isSleepTimerActive.value = true
        _sleepTimerRemaining.value = minutes * 60

        sleepTimerJob = scope.launch {
            while (_sleepTimerRemaining.value > 0) {
                delay(1000)
                _sleepTimerRemaining.value -= 1
            }
            _isSleepTimerActive.value = false
            // Timer expired, pause music
            player?.pause()
            _playbackState.value = PlaybackState.PAUSED
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _isSleepTimerActive.value = false
        _sleepTimerRemaining.value = 0
    }

    // --- Queue Manipulation ---
    fun addToQueue(song: Song) {
        val q = _queue.value.toMutableList()
        if (!q.any { it.id == song.id }) {
            q.add(song)
            _queue.value = q
            originalQueue = originalQueue.toMutableList().apply { add(song) }
        }
    }

    fun removeFromQueue(songId: String) {
        val q = _queue.value.toMutableList()
        val index = q.indexOfFirst { it.id == songId }
        if (index != -1) {
            q.removeAt(index)
            _queue.value = q
            originalQueue = originalQueue.toMutableList().apply { removeAll { it.id == songId } }
            if (index == currentQueueIndex) {
                // If removed current playing song, skip to next
                if (q.isNotEmpty()) {
                    currentQueueIndex = index % q.size
                    play(q[currentQueueIndex])
                } else {
                    currentQueueIndex = -1
                    _currentSong.value = null
                    _playbackState.value = PlaybackState.IDLE
                    player?.pause()
                }
            } else if (index < currentQueueIndex) {
                currentQueueIndex -= 1
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        originalQueue = emptyList()
        currentQueueIndex = -1
        _currentSong.value = null
        _playbackState.value = PlaybackState.IDLE
        player?.pause()
    }
}
