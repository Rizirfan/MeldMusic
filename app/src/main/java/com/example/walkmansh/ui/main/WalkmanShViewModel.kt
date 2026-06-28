package com.example.walkmansh.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkmansh.data.DataRepository
import com.example.walkmansh.data.model.Playlist
import com.example.walkmansh.data.model.Song
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class WalkmanShViewModel(private val repository: DataRepository) : ViewModel() {
    val playlists: List<Playlist> = repository.getPlaylists()
    
    // Auto-search after typing (debounce)
    private val queryFlow = MutableStateFlow("")

    var searchQuery by mutableStateOf("")
        private set

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    var apiKey by mutableStateOf(repository.getApiKey())
        private set

    var hasApiKey by mutableStateOf(repository.hasApiKey())
        private set

    // --- Library & Persistence State Flows ---
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    private val _customPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val customPlaylists: StateFlow<List<Playlist>> = _customPlaylists.asStateFlow()

    private val _playHistory = MutableStateFlow<List<Song>>(emptyList())
    val playHistory: StateFlow<List<Song>> = _playHistory.asStateFlow()

    private val _recentlyPlayedFlow = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayedFlow.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // Active simulated download progress map: songId -> progress (0.0f .. 1.0f)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    // Settings
    private val _isOfflineMode = MutableStateFlow(repository.isOfflineModeEnabled())
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _themeMode = MutableStateFlow(repository.getThemeMode())
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _songQuality = MutableStateFlow(repository.getSongQuality())
    val songQuality: StateFlow<Int> = _songQuality.asStateFlow()

    private val _fadeInOut = MutableStateFlow(repository.isFadeInOutEnabled())
    val fadeInOut: StateFlow<Boolean> = _fadeInOut.asStateFlow()

    init {
        refreshLibraryLists()

        queryFlow
            .debounce(600)
            .onEach { query ->
                if (query.isNotBlank()) {
                    performSearch(query)
                } else {
                    _searchResults.value = emptyList()
                    _searchError.value = null
                }
            }
            .launchIn(viewModelScope)
    }

    fun refreshLibraryLists() {
        _likedSongs.value = repository.getLikedSongs()
        _downloadedSongs.value = repository.getDownloadedSongs()
        _customPlaylists.value = repository.getCustomPlaylists()
        _playHistory.value = repository.getPlayHistory()
        _recentlyPlayedFlow.value = repository.getRecentlyPlayed()
        _searchHistory.value = repository.getSearchHistory()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        queryFlow.value = query
    }

    fun triggerSearch() {
        performSearch(searchQuery)
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            
            // Add keyword to history
            repository.addSearchKeyword(query)
            _searchHistory.value = repository.getSearchHistory()

            try {
                val results = repository.searchSongs(query)
                _searchResults.value = results
                if (results.isEmpty()) {
                    _searchError.value = "No results found"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val message = if (e is java.net.UnknownHostException || e.message?.contains("Unable to resolve host") == true) {
                    "No internet connection. Please check your network connection and try again."
                } else {
                    e.message ?: "An error occurred"
                }
                _searchError.value = message
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun updateApiKey(key: String) {
        repository.saveApiKey(key)
        apiKey = key
        hasApiKey = repository.hasApiKey()
    }

    // --- Library & Settings Actions ---

    fun toggleLike(song: Song) {
        repository.toggleLikeSong(song)
        _likedSongs.value = repository.getLikedSongs()
        // Save history entry if played
        refreshLibraryLists()
    }

    fun addSongToHistory(song: Song) {
        repository.addSongToHistory(song)
        refreshLibraryLists()
    }

    fun clearPlayHistory() {
        repository.clearHistory()
        refreshLibraryLists()
    }

    fun clearSearchHistory() {
        repository.clearSearchHistory()
        refreshLibraryLists()
    }

    // --- Downloads Simulator ---
    fun downloadSong(song: Song) {
        if (repository.isSongDownloaded(song.id)) return

        viewModelScope.launch {
            val progressMap = _downloadProgress.value.toMutableMap()
            progressMap[song.id] = 0.0f
            _downloadProgress.value = progressMap

            // Simulate progress increment over 2 seconds
            for (step in 1..20) {
                delay(100)
                val updateMap = _downloadProgress.value.toMutableMap()
                updateMap[song.id] = step / 20.0f
                _downloadProgress.value = updateMap
            }

            repository.saveDownloadedSong(song)
            
            val finalMap = _downloadProgress.value.toMutableMap()
            finalMap.remove(song.id)
            _downloadProgress.value = finalMap

            refreshLibraryLists()
        }
    }

    fun deleteDownload(songId: String) {
        repository.deleteDownloadedSong(songId)
        refreshLibraryLists()
    }

    fun clearAllDownloads() {
        repository.clearAllDownloads()
        refreshLibraryLists()
    }

    fun getSimulatedStorageSize(): String {
        return repository.getSimulatedStorageSize()
    }

    // --- Custom Playlists Actions ---
    fun createPlaylist(name: String, description: String) {
        repository.createCustomPlaylist(name, description)
        refreshLibraryLists()
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        repository.addSongToCustomPlaylist(playlistId, song)
        refreshLibraryLists()
    }

    fun deletePlaylist(playlistId: String) {
        repository.deleteCustomPlaylist(playlistId)
        refreshLibraryLists()
    }

    // --- Offline and Theme settings ---
    fun setOfflineMode(enabled: Boolean) {
        repository.setOfflineModeEnabled(enabled)
        _isOfflineMode.value = enabled
        // Clear search results to reflect offline availability
        _searchResults.value = emptyList()
        refreshLibraryLists()
    }

    fun setThemeMode(mode: Int) {
        repository.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setSongQuality(quality: Int) {
        repository.setSongQuality(quality)
        _songQuality.value = quality
    }

    fun setFadeInOut(enabled: Boolean) {
        repository.setFadeInOutEnabled(enabled)
        _fadeInOut.value = enabled
    }

    fun isSongLiked(songId: String): Boolean {
        return repository.isSongLiked(songId)
    }

    fun isSongDownloaded(songId: String): Boolean {
        return repository.isSongDownloaded(songId)
    }
}
