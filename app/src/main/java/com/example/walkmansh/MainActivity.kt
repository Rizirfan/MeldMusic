package com.example.walkmansh

import android.content.Intent
import android.os.Bundle

import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkmansh.data.DefaultDataRepository
import com.example.walkmansh.playback.PlaybackManager
import com.example.walkmansh.theme.WalkmanshTheme
import com.example.walkmansh.ui.components.AppleMusicUi
import com.example.walkmansh.ui.main.WalkmanShViewModel
import com.example.walkmansh.ui.player.FullPlayerScreen
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions

class MainActivity : ComponentActivity() {
    private var youtubePlayerView: YouTubePlayerView? = null
    private var onVoiceSearchResult: ((String) -> Unit)? = null

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            spokenText?.let {
                onVoiceSearchResult?.invoke(it)
            }
        }
    }

    private fun triggerVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search music")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("WalkmanSh", "Voice search launch failed: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = DefaultDataRepository(applicationContext)

        setContent {
            val viewModel: WalkmanShViewModel = viewModel {
                WalkmanShViewModel(repository)
            }
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            WalkmanshTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    com.example.walkmansh.ui.components.PremiumDynamicBackground(isDark = isDarkTheme)

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {

                        var isPlayerOpen by remember { mutableStateOf(false) }

                        // Sync Voice Search Result with ViewModel
                        DisposableEffect(Unit) {
                            onVoiceSearchResult = { text ->
                                viewModel.onSearchQueryChange(text)
                                viewModel.triggerSearch()
                            }
                            onDispose {
                                onVoiceSearchResult = null
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Main Apple Music styled tabs UI
                            AppleMusicUi(
                                viewModel = viewModel,
                                onOpenPlayer = { isPlayerOpen = true },
                                onTriggerVoiceSearch = { triggerVoiceSearch() }
                            )

                            // Slide up full screen player
                            AnimatedVisibility(
                                visible = isPlayerOpen,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                FullPlayerScreen(
                                    onDismiss = { isPlayerOpen = false },
                                    viewModel = viewModel
                                )
                            }

                            // Hidden YouTube Player View (audio-only playback companion)
                            // Placed last in Z-order, size 1.dp with tiny alpha, enabling background playback to prevent suspension
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(1.dp)
                                    .alpha(0.01f)
                            ) {
                                AndroidView(
                                    factory = { context ->
                                        android.util.Log.d("WalkmanSh", "YouTubePlayerView factory called")
                                        val playerView = YouTubePlayerView(context).apply {
                                            enableAutomaticInitialization = false
                                            enableBackgroundPlayback(true)
                                            val options = IFramePlayerOptions.Builder(context)
                                                .controls(0)
                                                .autoplay(1)
                                                .origin("https://com.example.walkmansh")
                                                .build()
                                            initialize(object : AbstractYouTubePlayerListener() {
                                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                                    android.util.Log.d("WalkmanSh", "YouTubePlayerView onReady called")
                                                    PlaybackManager.initializePlayer(youTubePlayer)
                                                }
                                            }, options)
                                        }
                                        this@MainActivity.youtubePlayerView = playerView
                                        playerView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        youtubePlayerView?.release()
    }
}
