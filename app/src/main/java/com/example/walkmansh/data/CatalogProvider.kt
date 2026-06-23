package com.example.walkmansh.data

import com.example.walkmansh.data.model.Playlist
import com.example.walkmansh.data.model.Song

object CatalogProvider {
    val chillLofiPlaylist: Playlist by lazy {
        Playlist(
            "lofi_vibes",
            "Chill Lofi Beats",
            "Relax and focus with this collection of lofi hip-hop and synthwave beats.",
            "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("5qap5aO4i9A", "1 A.M Study Session 📚", "Lofi Girl", "Study Beats", "https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg", 1402),
                Song("tNkZs5WMC_Y", "Late Night Melancholy 🌙", "Lofi Records", "Late Night Chill", "https://i.ytimg.com/vi/tNkZs5WMC_Y/hqdefault.jpg", 180),
                Song("jfKfPfyJRdk", "Lofi Girl Radio Theme", "ChilledCow", "Lofi Essentials", "https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg", 300),
                Song("7NOSDKb0HJA", "Warm Breeze", "Linearwave", "Comfort Zone", "https://i.ytimg.com/vi/7NOSDKb0HJA/hqdefault.jpg", 152)
            )
        )
    }

    val popHitsPlaylist: Playlist by lazy {
        Playlist(
            "pop_hits",
            "Essential Pop",
            "The biggest chart-toppers and global anthems from today's pop stars.",
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("bM7SZ5SBzyY", "Fade", "Alan Walker", "NCS Single", "https://i.ytimg.com/vi/bM7SZ5SBzyY/hqdefault.jpg", 260),
                Song("K4DyBUG242c", "On & On", "Cartoon", "NCS Release", "https://i.ytimg.com/vi/K4DyBUG242c/hqdefault.jpg", 207),
                Song("J2X5mJ3HDYE", "Invincible", "DEAF KEV", "NCS Release", "https://i.ytimg.com/vi/J2X5mJ3HDYE/hqdefault.jpg", 273),
                Song("3nqd75OC9AI", "Heroes Tonight", "Janji ft. Johnning", "Heroes", "https://i.ytimg.com/vi/3nqd75OC9AI/hqdefault.jpg", 208)
            )
        )
    }

    val electroDancePlaylist: Playlist by lazy {
        Playlist(
            "electro_dance",
            "Electronic Focus",
            "Vibrant synths and deep basslines to energize your workflow.",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("EP625xQIGzs", "Hope", "Tobu", "Hope Album", "https://i.ytimg.com/vi/EP625xQIGzs/hqdefault.jpg", 275),
                Song("ux8-EbW6D94", "Infectious", "Tobu", "Infectious Single", "https://i.ytimg.com/vi/ux8-EbW6D94/hqdefault.jpg", 256),
                Song("hr-3I5jsB6Y", "Sky High", "Elektronomia", "Sky High Single", "https://i.ytimg.com/vi/hr-3I5jsB6Y/hqdefault.jpg", 242),
                Song("1dzQ35n14h8", "Cloud 9", "Itro & Tobu", "Cloud 9 Single", "https://i.ytimg.com/vi/1dzQ35n14h8/hqdefault.jpg", 275)
            )
        )
    }

    val workoutEnergyPlaylist: Playlist by lazy {
        Playlist(
            "workout_energy",
            "Workout Energy",
            "High BPM and power tracks to push your physical limits.",
            "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("J2X5mJ3HDYE", "Invincible", "DEAF KEV", "Workout Anthems", "https://i.ytimg.com/vi/J2X5mJ3HDYE/hqdefault.jpg", 273),
                Song("hr-3I5jsB6Y", "Sky High", "Elektronomia", "Workout Beats", "https://i.ytimg.com/vi/hr-3I5jsB6Y/hqdefault.jpg", 242),
                Song("EP625xQIGzs", "Hope", "Tobu", "Motivation", "https://i.ytimg.com/vi/EP625xQIGzs/hqdefault.jpg", 275)
            )
        )
    }

    val focusStudyPlaylist: Playlist by lazy {
        Playlist(
            "focus_study",
            "Focus & Study",
            "Deep focus waves and alpha-frequency beats for learning and coding.",
            "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("5qap5aO4i9A", "1 A.M Study Session 📚", "Lofi Girl", "Study Beats", "https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg", 1402),
                Song("7NOSDKb0HJA", "Warm Breeze", "Linearwave", "Comfort Zone", "https://i.ytimg.com/vi/7NOSDKb0HJA/hqdefault.jpg", 152)
            )
        )
    }

    val lateNightLofiPlaylist: Playlist by lazy {
        Playlist(
            "late_night_lofi",
            "Late Night Lofi",
            "Chill beats for the midnight hours and quiet moments.",
            "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("tNkZs5WMC_Y", "Late Night Melancholy 🌙", "Lofi Records", "Late Night Chill", "https://i.ytimg.com/vi/tNkZs5WMC_Y/hqdefault.jpg", 180),
                Song("jfKfPfyJRdk", "Lofi Girl Radio Theme", "ChilledCow", "Lofi Essentials", "https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg", 300)
            )
        )
    }

    val summerBeatsPlaylist: Playlist by lazy {
        Playlist(
            "summer_beats",
            "Summer Beats",
            "Sunny synths and tropical rhythms to brighten up your day.",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("3nqd75OC9AI", "Heroes Tonight", "Janji ft. Johnning", "Summer Hits", "https://i.ytimg.com/vi/3nqd75OC9AI/hqdefault.jpg", 208),
                Song("1dzQ35n14h8", "Cloud 9", "Itro & Tobu", "Sunny Days", "https://i.ytimg.com/vi/1dzQ35n14h8/hqdefault.jpg", 275)
            )
        )
    }

    val trendingPlaylist: Playlist by lazy {
        Playlist(
            "trending_charts",
            "Top Charts & Trending",
            "The most streamed and trending tracks on the Walkman networks.",
            "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("8uTrvb7Jl1w", "Die With A Smile", "Lady Gaga & Bruno Mars", "Single", "https://i.ytimg.com/vi/8uTrvb7Jl1w/hqdefault.jpg", 251),
                Song("ekr0t949-c4", "APT.", "ROSÉ & Bruno Mars", "Single", "https://i.ytimg.com/vi/ekr0t949-c4/hqdefault.jpg", 170),
                Song("eVli-tstM5E", "Espresso", "Sabrina Carpenter", "Short n' Sweet", "https://i.ytimg.com/vi/eVli-tstM5E/hqdefault.jpg", 171),
                Song("YH6HJb_F-LE", "Birds of a Feather", "Billie Eilish", "HIT ME HARD AND SOFT", "https://i.ytimg.com/vi/YH6HJb_F-LE/hqdefault.jpg", 210),
                Song("Oa_RSwwpPaA", "Beautiful Things", "Benson Boone", "Single", "https://i.ytimg.com/vi/Oa_RSwwpPaA/hqdefault.jpg", 180),
                Song("54H5F1N9dC4", "I Had Some Help", "Post Malone ft. Morgan Wallen", "Single", "https://i.ytimg.com/vi/54H5F1N9dC4/hqdefault.jpg", 178),
                Song("KQ2NggJ28Uo", "Good Luck, Babe!", "Chappell Roan", "Single", "https://i.ytimg.com/vi/KQ2NggJ28Uo/hqdefault.jpg", 218),
                Song("srxsB-kd3YA", "The Emptiness Machine", "Linkin Park", "From Zero", "https://i.ytimg.com/vi/srxsB-kd3YA/hqdefault.jpg", 190)
            )
        )
    }

    val newReleasesPlaylist: Playlist by lazy {
        Playlist(
            "new_releases",
            "New Releases",
            "Fresh additions to the Walkman catalog.",
            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=60",
            listOf(
                Song("ekr0t949-c4", "APT.", "ROSÉ & Bruno Mars", "Single", "https://i.ytimg.com/vi/ekr0t949-c4/hqdefault.jpg", 170),
                Song("n67CRWp8DBs", "Please Please Please", "Sabrina Carpenter", "Short n' Sweet", "https://i.ytimg.com/vi/n67CRWp8DBs/hqdefault.jpg", 186),
                Song("2O72M-90Q9E", "CHIHIRO", "Billie Eilish", "HIT ME HARD AND SOFT", "https://i.ytimg.com/vi/2O72M-90Q9E/hqdefault.jpg", 303),
                Song("q3os0s4Ld5Q", "Fortnight", "Taylor Swift ft. Post Malone", "THE TORTURED POETS DEPARTMENT", "https://i.ytimg.com/vi/q3os0s4Ld5Q/hqdefault.jpg", 230),
                Song("kTJczUoc26U", "STAY", "The Kid LAROI & Justin Bieber", "F*CK LOVE 3: OVER YOU", "https://i.ytimg.com/vi/kTJczUoc26U/hqdefault.jpg", 141),
                Song("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "After Hours", "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg", 200),
                Song("srxsB-kd3YA", "The Emptiness Machine", "Linkin Park", "From Zero", "https://i.ytimg.com/vi/srxsB-kd3YA/hqdefault.jpg", 190),
                Song("YH6HJb_F-LE", "Birds of a Feather", "Billie Eilish", "HIT ME HARD AND SOFT", "https://i.ytimg.com/vi/YH6HJb_F-LE/hqdefault.jpg", 210)
            )
        )
    }

    val playlists: List<Playlist> = listOf(
        chillLofiPlaylist,
        popHitsPlaylist,
        electroDancePlaylist,
        workoutEnergyPlaylist,
        focusStudyPlaylist,
        lateNightLofiPlaylist,
        summerBeatsPlaylist
    )

    val allSongs: List<Song> = (playlists + trendingPlaylist + newReleasesPlaylist).flatMap { it.songs }.distinctBy { it.id }

    val recentlyPlayed: List<Song> = listOf(
        allSongs[0],
        allSongs[4],
        allSongs[8],
        allSongs[1]
    )

    fun getRecommendations(likedSongIds: List<String>): List<Song> {
        if (likedSongIds.isEmpty()) {
            return allSongs.shuffled().take(6)
        }
        
        // Find artists of liked songs
        val likedArtists = allSongs.filter { likedSongIds.contains(it.id) }.map { it.artist }.toSet()
        
        // Recommend other songs by the same artists, or fallback to random
        val recommended = allSongs.filter { likedArtists.contains(it.artist) && !likedSongIds.contains(it.id) }
        
        return if (recommended.size >= 4) {
            recommended.take(6)
        } else {
            (recommended + allSongs.filter { !likedSongIds.contains(it.id) }).distinctBy { it.id }.shuffled().take(6)
        }
    }
}
