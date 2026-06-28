package com.example.walkmansh

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkmansh.data.DefaultDataRepository
import com.example.walkmansh.playback.MusicService
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not - we handle gracefully */ }

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
            // Defer service + permission request to after first frame
            LaunchedEffect(Unit) {
                startService(Intent(this@MainActivity, MusicService::class.java))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
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
                            AppleMusicUi(
                                viewModel = viewModel,
                                onOpenPlayer = { isPlayerOpen = true },
                                onTriggerVoiceSearch = { triggerVoiceSearch() }
                            )

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

                            // YouTube player container — lightweight hidden view
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(1.dp)
                                    .alpha(0.01f)
                            ) {
                                AndroidView(
                                    factory = { context ->
                                        YouTubePlayerView(context).apply {
                                            enableAutomaticInitialization = false
                                            enableBackgroundPlayback(true)
                                            initialize(
                                                object : AbstractYouTubePlayerListener() {
                                                    override fun onReady(youTubePlayer: YouTubePlayer) {
                                                        PlaybackManager.onPlayerReady(youTubePlayer)
                                                    }
                                                },
                                                IFramePlayerOptions.Builder(this@MainActivity)
                                                    .controls(0)
                                                    .build()
                                            )
                                        }
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
}
