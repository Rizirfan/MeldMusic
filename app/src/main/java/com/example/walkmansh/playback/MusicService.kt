package com.example.walkmansh.playback

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import com.example.walkmansh.MainActivity
import com.example.walkmansh.R
import com.example.walkmansh.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "walkmansh_playback"
        const val NOTIFICATION_ID = 1001
        const val MEDIA_SESSION_TAG = "WalkmanSh"

        const val ACTION_TOGGLE = "com.example.walkmansh.TOGGLE"
        const val ACTION_SKIP_NEXT = "com.example.walkmansh.SKIP_NEXT"
        const val ACTION_SKIP_PREV = "com.example.walkmansh.SKIP_PREV"
        const val ACTION_STOP = "com.example.walkmansh.STOP"
    }

    private val accentColor = 0xFFFF2D55.toInt()

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (PlaybackManager.playbackState.value == PlaybackState.PLAYING) {
                    PlaybackManager.togglePlayPause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { }
            AudioManager.AUDIOFOCUS_GAIN -> { }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateJob: Job? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initMediaSession()
        // Show a minimal foreground notification immediately so the service is not killed
        showPlaceholderNotification()
        observeState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> PlaybackManager.togglePlayPause()
            ACTION_SKIP_NEXT -> PlaybackManager.skipNext()
            ACTION_SKIP_PREV -> PlaybackManager.skipPrevious()
            ACTION_STOP -> {
                PlaybackManager.clearQueue()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        // Re-show notification handle in case service was restarted
        val current = PlaybackManager.currentSong.value
        if (current != null) {
            updateNotification(current, PlaybackManager.playbackState.value)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stateJob?.cancel()
        scope.cancel()
        mediaSession.release()
        abandonAudioFocus()
        super.onDestroy()
    }

    private fun showPlaceholderNotification() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Starting playback…")
            .setContentIntent(openPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
        try {
            startForeground(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {
            // Permission denied or other transient issue – try again later
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG)
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { PlaybackManager.togglePlayPause() }
            override fun onPause() { PlaybackManager.togglePlayPause() }
            override fun onSkipToNext() { PlaybackManager.skipNext() }
            override fun onSkipToPrevious() { PlaybackManager.skipPrevious() }
            override fun onSeekTo(pos: Long) {
                PlaybackManager.seekTo(pos.toFloat() / 1000f)
            }
            override fun onStop() {
                PlaybackManager.clearQueue()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        })
        mediaSession.isActive = true

        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()
        )
    }

    private fun observeState() {
        stateJob = scope.launch {
            combine(
                PlaybackManager.currentSong,
                PlaybackManager.playbackState,
                PlaybackManager.progress,
                PlaybackManager.duration
            ) { song, state, progress, duration ->
                updateMediaSession(song, state, progress, duration)
                updateNotification(song, state)
                handleAudioFocus(state)
                handleForeground(song, state)
            }
        }
    }

    private fun updateMediaSession(
        song: Song?,
        state: PlaybackState,
        progress: Float,
        duration: Float
    ) {
        if (song == null) return

        val meta = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (duration * 1000).toLong())
            .build()
        mediaSession.setMetadata(meta)

        val playState = when (state) {
            PlaybackState.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            PlaybackState.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            PlaybackState.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            PlaybackState.ENDED -> PlaybackStateCompat.STATE_STOPPED
            PlaybackState.IDLE -> PlaybackStateCompat.STATE_NONE
        }

        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(playState, (progress * 1000).toLong(), 1.0f)
                .build()
        )
    }

    private fun updateNotification(song: Song?, state: PlaybackState) {
        if (song == null) return

        val isPlaying = state == PlaybackState.PLAYING

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePending = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPending = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_SKIP_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevPending = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).setAction(ACTION_SKIP_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPending = PendingIntent.getService(
            this, 4,
            Intent(this, MusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(openPending)
            .setDeleteIntent(stopPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setColor(accentColor)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(1)
                    .setShowCancelButton(true)
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevPending
            )
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                togglePending
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextPending
            )

        scope.launch {
            val bitmap = loadAlbumArt(song.thumbnailUrl)
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
            }
            NotificationManagerCompat.from(this@MusicService)
                .notify(NOTIFICATION_ID, builder.build())
        }
    }

    private suspend fun loadAlbumArt(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun handleForeground(song: Song?, state: PlaybackState) {
        // Keep service alive via placeholder notification; only stop on explicit ACTION_STOP
    }

    private fun handleAudioFocus(state: PlaybackState) {
        if (state == PlaybackState.PLAYING) {
            requestAudioFocus()
        } else {
            abandonAudioFocus()
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls music playback"
                setShowBadge(false)
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
