package com.example.walkmansh.data

import android.content.Context
import com.example.walkmansh.BuildConfig
import com.example.walkmansh.data.api.YoutubeClient
import com.example.walkmansh.data.model.Playlist
import com.example.walkmansh.data.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface DataRepository {
    fun getPlaylists(): List<Playlist>
    fun getRecentlyPlayed(): List<Song>
    suspend fun searchSongs(query: String): List<Song>
    fun saveApiKey(key: String)
    fun getApiKey(): String
    fun hasApiKey(): Boolean

    // Liked Songs
    fun getLikedSongs(): List<Song>
    fun toggleLikeSong(song: Song): Boolean
    fun isSongLiked(songId: String): Boolean

    // Downloads
    fun getDownloadedSongs(): List<Song>
    fun saveDownloadedSong(song: Song)
    fun deleteDownloadedSong(songId: String)
    fun isSongDownloaded(songId: String): Boolean
    fun getSimulatedStorageSize(): String
    fun clearAllDownloads()

    // Play History
    fun getPlayHistory(): List<Song>
    fun addSongToHistory(song: Song)
    fun clearHistory()

    // Search History
    fun getSearchHistory(): List<String>
    fun addSearchKeyword(keyword: String)
    fun clearSearchHistory()

    // Custom Playlists
    fun getCustomPlaylists(): List<Playlist>
    fun createCustomPlaylist(name: String, description: String): Playlist
    fun addSongToCustomPlaylist(playlistId: String, song: Song)
    fun deleteCustomPlaylist(playlistId: String)

    // Settings
    fun isOfflineModeEnabled(): Boolean
    fun setOfflineModeEnabled(enabled: Boolean)
    fun getThemeMode(): Int // 0: System, 1: Light, 2: Dark
    fun setThemeMode(mode: Int)
    fun getSongQuality(): Int // 0: Auto, 1: High, 2: Medium, 3: Low
    fun setSongQuality(quality: Int)
    fun isFadeInOutEnabled(): Boolean
    fun setFadeInOutEnabled(enabled: Boolean)
}

class DefaultDataRepository(private val context: Context) : DataRepository {
    private val youtubeClient = YoutubeClient.getInstance()
    private val prefs = context.getSharedPreferences("walkmansh_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        // Load API key from generated BuildConfig
        youtubeClient.setApiKey(BuildConfig.YOUTUBE_API_KEY)
    }

    // Helper to cache song metadata globally
    private fun cacheSongMetadata(song: Song) {
        val cached = getCachedSongs().toMutableMap()
        cached[song.id] = song
        prefs.edit().putString("song_cache", gson.toJson(cached)).apply()
    }

    private fun getCachedSongs(): Map<String, Song> {
        val json = prefs.getString("song_cache", null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Song>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun lookupSong(songId: String): Song? {
        // First look in catalog
        val catalogSong = CatalogProvider.allSongs.firstOrNull { it.id == songId }
        if (catalogSong != null) return catalogSong
        
        // Otherwise look in metadata cache
        return getCachedSongs()[songId]
    }

    override fun getPlaylists(): List<Playlist> {
        return CatalogProvider.playlists
    }

    override fun getRecentlyPlayed(): List<Song> {
        // Combine Catalog's recently played with listening history
        val history = getPlayHistory()
        return if (history.isEmpty()) {
            CatalogProvider.recentlyPlayed
        } else {
            (history + CatalogProvider.recentlyPlayed).distinctBy { it.id }.take(10)
        }
    }

    override suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (isOfflineModeEnabled()) {
            // In offline mode, search only downloaded songs
            return@withContext getDownloadedSongs().filter {
                it.title.lowercase().contains(query.lowercase()) || it.artist.lowercase().contains(query.lowercase())
            }
        }

        if (youtubeClient.hasApiKey()) {
            try {
                val results = youtubeClient.search(query, 15)
                // Cache metadata of searched results
                results.forEach { cacheSongMetadata(it) }
                return@withContext results
            } catch (e: Exception) {
                // Fallback to local catalog search
                return@withContext filterLocalCatalog(query)
            }
        } else {
            return@withContext filterLocalCatalog(query)
        }
    }

    private fun filterLocalCatalog(query: String): List<Song> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()
        return CatalogProvider.allSongs.filter {
            it.title.lowercase().contains(lowerQuery) || it.artist.lowercase().contains(lowerQuery)
        }
    }

    override fun saveApiKey(key: String) {
        // Secured in buildConfig
    }

    override fun getApiKey(): String {
        return youtubeClient.apiKey ?: ""
    }

    override fun hasApiKey(): Boolean {
        return youtubeClient.hasApiKey()
    }

    // --- LIKED SONGS ---
    override fun getLikedSongs(): List<Song> {
        val idsJson = prefs.getString("liked_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: List<String> = gson.fromJson(idsJson, idsType)
        return ids.mapNotNull { lookupSong(it) }
    }

    override fun toggleLikeSong(song: Song): Boolean {
        cacheSongMetadata(song)
        val idsJson = prefs.getString("liked_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: MutableList<String> = gson.fromJson(idsJson, idsType)
        
        val isLiked = if (ids.contains(song.id)) {
            ids.remove(song.id)
            false
        } else {
            ids.add(song.id)
            true
        }
        
        prefs.edit().putString("liked_song_ids", gson.toJson(ids)).apply()
        return isLiked
    }

    override fun isSongLiked(songId: String): Boolean {
        val idsJson = prefs.getString("liked_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: List<String> = gson.fromJson(idsJson, idsType)
        return ids.contains(songId)
    }

    // --- DOWNLOADS ---
    override fun getDownloadedSongs(): List<Song> {
        val idsJson = prefs.getString("downloaded_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: List<String> = gson.fromJson(idsJson, idsType)
        return ids.mapNotNull { lookupSong(it) }
    }

    override fun saveDownloadedSong(song: Song) {
        cacheSongMetadata(song)
        val idsJson = prefs.getString("downloaded_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: MutableList<String> = gson.fromJson(idsJson, idsType)
        
        if (!ids.contains(song.id)) {
            ids.add(song.id)
            prefs.edit().putString("downloaded_song_ids", gson.toJson(ids)).apply()
        }
    }

    override fun deleteDownloadedSong(songId: String) {
        val idsJson = prefs.getString("downloaded_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: MutableList<String> = gson.fromJson(idsJson, idsType)
        
        if (ids.contains(songId)) {
            ids.remove(songId)
            prefs.edit().putString("downloaded_song_ids", gson.toJson(ids)).apply()
        }
    }

    override fun isSongDownloaded(songId: String): Boolean {
        val idsJson = prefs.getString("downloaded_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: List<String> = gson.fromJson(idsJson, idsType)
        return ids.contains(songId)
    }

    override fun getSimulatedStorageSize(): String {
        val count = getDownloadedSongs().size
        val sizeMB = count * 8.4 // Simulate 8.4 MB per track
        return String.format("%.1f MB", sizeMB)
    }

    override fun clearAllDownloads() {
        prefs.edit().putString("downloaded_song_ids", "[]").apply()
    }

    // --- PLAY HISTORY ---
    override fun getPlayHistory(): List<Song> {
        val idsJson = prefs.getString("history_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: List<String> = gson.fromJson(idsJson, idsType)
        return ids.mapNotNull { lookupSong(it) }
    }

    override fun addSongToHistory(song: Song) {
        cacheSongMetadata(song)
        val idsJson = prefs.getString("history_song_ids", "[]")
        val idsType = object : TypeToken<List<String>>() {}.type
        val ids: MutableList<String> = gson.fromJson(idsJson, idsType)
        
        // Move to top of history
        ids.remove(song.id)
        ids.add(0, song.id)
        
        // Cap history at 50 tracks
        val cappedIds = if (ids.size > 50) ids.take(50) else ids
        prefs.edit().putString("history_song_ids", gson.toJson(cappedIds)).apply()
    }

    override fun clearHistory() {
        prefs.edit().putString("history_song_ids", "[]").apply()
    }

    // --- SEARCH HISTORY ---
    override fun getSearchHistory(): List<String> {
        val json = prefs.getString("search_history", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    override fun addSearchKeyword(keyword: String) {
        if (keyword.isBlank()) return
        val json = prefs.getString("search_history", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val history: MutableList<String> = gson.fromJson(json, type)
        
        history.remove(keyword)
        history.add(0, keyword)
        
        val capped = if (history.size > 15) history.take(15) else history
        prefs.edit().putString("search_history", gson.toJson(capped)).apply()
    }

    override fun clearSearchHistory() {
        prefs.edit().putString("search_history", "[]").apply()
    }

    // --- CUSTOM PLAYLISTS ---
    override fun getCustomPlaylists(): List<Playlist> {
        val json = prefs.getString("custom_playlists", "[]")
        val type = object : TypeToken<List<Playlist>>() {}.type
        return gson.fromJson(json, type)
    }

    override fun createCustomPlaylist(name: String, description: String): Playlist {
        val playlists = getCustomPlaylists().toMutableList()
        val id = "custom_" + System.currentTimeMillis()
        val newPlaylist = Playlist(
            id,
            name,
            description,
            "https://images.unsplash.com/photo-1487180142328-0c4e37023af5?w=500&auto=format&fit=crop&q=60",
            emptyList()
        )
        playlists.add(newPlaylist)
        prefs.edit().putString("custom_playlists", gson.toJson(playlists)).apply()
        return newPlaylist
    }

    override fun addSongToCustomPlaylist(playlistId: String, song: Song) {
        cacheSongMetadata(song)
        val playlists = getCustomPlaylists().toMutableList()
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = playlists[index]
            val songs = playlist.songs.toMutableList()
            if (!songs.any { it.id == song.id }) {
                songs.add(song)
                playlists[index] = Playlist(
                    playlist.id,
                    playlist.name,
                    playlist.description,
                    song.thumbnailUrl, // Use latest song's thumbnail
                    songs
                )
                prefs.edit().putString("custom_playlists", gson.toJson(playlists)).apply()
            }
        }
    }

    override fun deleteCustomPlaylist(playlistId: String) {
        val playlists = getCustomPlaylists().toMutableList()
        playlists.removeAll { it.id == playlistId }
        prefs.edit().putString("custom_playlists", gson.toJson(playlists)).apply()
    }

    // --- SETTINGS CONFIG ---
    override fun isOfflineModeEnabled(): Boolean {
        return prefs.getBoolean("offline_mode", false)
    }

    override fun setOfflineModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("offline_mode", enabled).apply()
    }

    override fun getThemeMode(): Int {
        return prefs.getInt("theme_mode", 0)
    }

    override fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    override fun getSongQuality(): Int {
        return prefs.getInt("song_quality", 0)
    }

    override fun setSongQuality(quality: Int) {
        prefs.edit().putInt("song_quality", quality).apply()
    }

    override fun isFadeInOutEnabled(): Boolean {
        return prefs.getBoolean("fade_in_out", false)
    }

    override fun setFadeInOutEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("fade_in_out", enabled).apply()
    }
}
