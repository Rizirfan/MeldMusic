// app.js - Frontend application controller for MeldMusic Desktop
const { ipcRenderer } = require('electron');

// 1. Local Catalog Data Repository (Matching Android CatalogProvider.kt)
const catalogSongs = [
  { id: "8uTrvb7Jl1w", title: "Die With A Smile", artist: "Lady Gaga & Bruno Mars", album: "Single", artwork: "https://i.ytimg.com/vi/8uTrvb7Jl1w/hqdefault.jpg", duration: 251 },
  { id: "5qap5aO4i9A", title: "1 A.M Study Session 📚", artist: "Lofi Girl", album: "Study Beats", artwork: "https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg", duration: 1402 },
  { id: "tNkZs5WMC_Y", title: "Late Night Melancholy 🌙", artist: "Lofi Records", album: "Late Night Chill", artwork: "https://i.ytimg.com/vi/tNkZs5WMC_Y/hqdefault.jpg", duration: 180 },
  { id: "jfKfPfyJRdk", title: "Lofi Girl Radio Theme", artist: "ChilledCow", album: "Lofi Essentials", artwork: "https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg", duration: 300 },
  { id: "7NOSDKb0HJA", title: "Warm Breeze", artist: "Linearwave", album: "Comfort Zone", artwork: "https://i.ytimg.com/vi/7NOSDKb0HJA/hqdefault.jpg", duration: 152 },
  { id: "bM7SZ5SBzyY", title: "Fade", artist: "Alan Walker", album: "NCS Single", artwork: "https://i.ytimg.com/vi/bM7SZ5SBzyY/hqdefault.jpg", duration: 260 },
  { id: "K4DyBUG242c", title: "On & On", artist: "Cartoon", album: "NCS Release", artwork: "https://i.ytimg.com/vi/K4DyBUG242c/hqdefault.jpg", duration: 207 },
  { id: "J2X5mJ3HDYE", title: "Invincible", artist: "DEAF KEV", album: "NCS Release", artwork: "https://i.ytimg.com/vi/J2X5mJ3HDYE/hqdefault.jpg", duration: 273 },
  { id: "3nqd75OC9AI", title: "Heroes Tonight", artist: "Janji ft. Johnning", album: "Heroes", artwork: "https://i.ytimg.com/vi/3nqd75OC9AI/hqdefault.jpg", duration: 208 },
  { id: "EP625xQIGzs", title: "Hope", artist: "Tobu", album: "Hope Album", artwork: "https://i.ytimg.com/vi/EP625xQIGzs/hqdefault.jpg", duration: 275 },
  { id: "ux8-EbW6D94", title: "Infectious", artist: "Tobu", album: "Infectious Single", artwork: "https://i.ytimg.com/vi/ux8-EbW6D94/hqdefault.jpg", duration: 256 },
  { id: "hr-3I5jsB6Y", title: "Sky High", artist: "Elektronomia", album: "Sky High Single", artwork: "https://i.ytimg.com/vi/hr-3I5jsB6Y/hqdefault.jpg", duration: 242 },
  { id: "1dzQ35n14h8", title: "Cloud 9", artist: "Itro & Tobu", album: "Cloud 9 Single", artwork: "https://i.ytimg.com/vi/1dzQ35n14h8/hqdefault.jpg", duration: 275 }
];

const curatedPlaylists = [
  {
    id: "lofi_vibes",
    name: "Chill Lofi Beats",
    desc: "Relax and focus with this collection of lofi hip-hop and synthwave beats.",
    art: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&auto=format&fit=crop&q=60",
    songs: ["5qap5aO4i9A", "tNkZs5WMC_Y", "jfKfPfyJRdk", "7NOSDKb0HJA"]
  },
  {
    id: "pop_hits",
    name: "Essential Pop",
    desc: "The biggest chart-toppers and global anthems from today's pop stars.",
    art: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=60",
    songs: ["bM7SZ5SBzyY", "K4DyBUG242c", "J2X5mJ3HDYE", "3nqd75OC9AI"]
  },
  {
    id: "electro_dance",
    name: "Electronic Focus",
    desc: "Vibrant synths and deep basslines to energize your workflow.",
    art: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=60",
    songs: ["EP625xQIGzs", "ux8-EbW6D94", "hr-3I5jsB6Y", "1dzQ35n14h8"]
  },
  {
    id: "workout_energy",
    name: "Workout Energy",
    desc: "High BPM and power tracks to push your physical limits.",
    art: "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500&auto=format&fit=crop&q=60",
    songs: ["J2X5mJ3HDYE", "hr-3I5jsB6Y", "EP625xQIGzs"]
  }
];

// 2. State & Storage Keys
let likedSongs = JSON.parse(localStorage.getItem('liked_songs') || '[]');
let customPlaylists = JSON.parse(localStorage.getItem('custom_playlists') || '[]');
let searchHistory = JSON.parse(localStorage.getItem('search_history') || '[]');
let recentlyPlayed = JSON.parse(localStorage.getItem('recently_played') || '[]');

let settings = {
  theme: localStorage.getItem('settings_theme') || 'dark',
  offline: localStorage.getItem('settings_offline') === 'true',
  apiKey: localStorage.getItem('settings_apikey') || ''
};

// Playback State variables
let currentSong = catalogSongs[0]; // Default song: Die With A Smile
let playQueue = [catalogSongs[0]];
let currentQueueIndex = 0;
let isPlaying = false;
let isShuffle = false;
let isRepeat = false; // false = off, true = repeat all
let playbackSpeed = 1.0;
let volume = parseInt(localStorage.getItem('player_volume') || '100');
let isMuted = false;
let playerReady = false;
let updateProgressInterval = null;

// YouTube Iframe Player Reference
let ytPlayer = null;

// 3. Application Lifecycle Initialization
document.addEventListener('DOMContentLoaded', () => {
  setupElectronWindowControls();
  applySettings();
  initNavigation();
  renderCuratedPlaylists();
  renderRecentlyPlayed();
  renderCustomPlaylists();
  renderLikedSongs();
  setupPlayerUI();
  setupSearch();
  setupDialogs();
});

// 4. Frameless Electron Window Operations
function setupElectronWindowControls() {
  const win = require('electron').remote?.getCurrentWindow() || {
    minimize: () => ipcRenderer.send('window-minimize'),
    maximize: () => ipcRenderer.send('window-maximize'),
    close: () => ipcRenderer.send('window-close')
  };

  document.getElementById('btn-minimize').addEventListener('click', () => {
    ipcRenderer.send('window-minimize');
  });

  document.getElementById('btn-maximize').addEventListener('click', () => {
    ipcRenderer.send('window-maximize');
  });

  document.getElementById('btn-close').addEventListener('click', () => {
    ipcRenderer.send('window-close');
  });
}

// 5. IPC Main Communication (Bound in main.js process or standard actions)
// Note: Handlers are registered on the main process to handle window actions:
ipcRenderer.on = () => {};

// 6. Navigation Tabs
function initNavigation() {
  const navItems = document.querySelectorAll('.nav-item');
  const tabs = document.querySelectorAll('.tab-content');

  navItems.forEach(item => {
    item.addEventListener('click', () => {
      const targetTabId = item.getAttribute('data-target');
      
      navItems.forEach(nav => nav.classList.remove('active'));
      tabs.forEach(tab => tab.classList.remove('active'));

      item.classList.add('active');
      document.getElementById(targetTabId).classList.add('active');
    });
  });

  // Back button in playlist details
  document.getElementById('btn-back-playlist-detail').addEventListener('click', () => {
    document.getElementById('playlist-detail').classList.remove('active');
    // Reactivate previous tab (Player Hub)
    const activeNav = document.querySelector('.nav-item.active');
    const targetTabId = activeNav.getAttribute('data-target');
    document.getElementById(targetTabId).classList.add('active');
  });
}

// 7. Dynamic UI Rendering Methods
function renderCuratedPlaylists() {
  const grid = document.getElementById('curated-playlists');
  grid.innerHTML = '';

  curatedPlaylists.forEach(playlist => {
    const card = document.createElement('div');
    card.className = 'playlist-card';
    card.innerHTML = `
      <div class="playlist-art-wrapper">
        <img src="${playlist.art}" alt="${playlist.name}">
      </div>
      <h4>${playlist.name}</h4>
      <p>${playlist.desc}</p>
    `;
    card.addEventListener('click', () => showPlaylistDetail(playlist, false));
    grid.appendChild(card);
  });
}

function showPlaylistDetail(playlist, isCustom = false) {
  // Hide active tabs and show detail
  document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));
  document.getElementById('playlist-detail').classList.add('active');

  document.getElementById('detail-playlist-title').textContent = playlist.name;
  document.getElementById('detail-playlist-desc').textContent = playlist.desc || (isCustom ? "Custom playlist created by user" : "");
  document.getElementById('detail-playlist-art').src = playlist.art || 'icon.png';

  const songsListContainer = document.getElementById('detail-playlist-songs');
  songsListContainer.innerHTML = '';

  const songs = isCustom 
    ? playlist.songs 
    : playlist.songs.map(sid => catalogSongs.find(s => s.id === sid)).filter(Boolean);

  if (songs.length === 0) {
    songsListContainer.innerHTML = `<div class="empty-list-label" style="text-align:center; padding: 20px; color: var(--text-secondary);">No songs in this playlist. Use search to add songs!</div>`;
    return;
  }

  songs.forEach((song, idx) => {
    const row = createSongRow(song, idx, songs);
    
    // If custom playlist, add a delete song option
    if (isCustom) {
      const deleteCell = document.createElement('button');
      deleteCell.className = 'song-row-delete-btn';
      deleteCell.innerHTML = `<span class="material-icons-round">remove_circle_outline</span>`;
      deleteCell.addEventListener('click', (e) => {
        e.stopPropagation();
        removeSongFromPlaylist(playlist.id, song.id);
        showPlaylistDetail(playlist, true); // Refresh detail
      });
      // Replace target column or append before duration
      row.insertBefore(deleteCell, row.lastElementChild);
    }

    songsListContainer.appendChild(row);
  });

  // Action Buttons
  const playBtn = document.getElementById('btn-play-playlist');
  const shuffleBtn = document.getElementById('btn-shuffle-playlist');

  // Remove existing listeners
  playBtn.replaceWith(playBtn.cloneNode(true));
  shuffleBtn.replaceWith(shuffleBtn.cloneNode(true));

  document.getElementById('btn-play-playlist').addEventListener('click', () => {
    playQueue = [...songs];
    currentQueueIndex = 0;
    loadAndPlaySong(playQueue[0]);
  });

  document.getElementById('btn-shuffle-playlist').addEventListener('click', () => {
    playQueue = shuffleArray([...songs]);
    currentQueueIndex = 0;
    loadAndPlaySong(playQueue[0]);
  });
}

function createSongRow(song, idx, queueContext) {
  const row = document.createElement('div');
  row.className = `song-row ${currentSong && currentSong.id === song.id ? 'playing' : ''}`;
  
  const isLiked = likedSongs.some(s => s.id === song.id);

  row.innerHTML = `
    <img class="song-row-thumb" src="${song.artwork || 'icon.png'}" alt="Thumb">
    <div class="song-row-details">
      <div class="song-row-title">${song.title}</div>
      <div class="song-row-artist">${song.artist}</div>
    </div>
    <div class="song-row-album">${song.album || 'Single'}</div>
    <button class="song-row-like-btn" title="Like"><span class="material-icons-round">${isLiked ? 'favorite' : 'favorite_border'}</span></button>
    <div class="song-row-duration">${formatTime(song.duration)}</div>
  `;

  // Play song on click
  row.addEventListener('click', () => {
    playQueue = [...queueContext];
    currentQueueIndex = idx;
    loadAndPlaySong(song);
  });

  // Like button handling
  const likeBtn = row.querySelector('.song-row-like-btn');
  likeBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    toggleLikeSong(song);
    likeBtn.querySelector('span').textContent = likedSongs.some(s => s.id === song.id) ? 'favorite' : 'favorite_border';
  });

  return row;
}

function renderRecentlyPlayed() {
  const list = document.getElementById('recently-played-list');
  list.innerHTML = '';

  const recent = recentlyPlayed.length > 0 ? recentlyPlayed : catalogSongs.slice(0, 5);

  recent.forEach((song, idx) => {
    const row = createSongRow(song, idx, recent);
    list.appendChild(row);
  });
}

function renderLikedSongs() {
  const list = document.getElementById('liked-songs-list');
  list.innerHTML = '';

  if (likedSongs.length === 0) {
    list.innerHTML = `<div class="empty-list-label" style="text-align:center; padding: 40px; color: var(--text-secondary);">No liked songs yet. Tap favorite on any track!</div>`;
    return;
  }

  likedSongs.forEach((song, idx) => {
    const row = createSongRow(song, idx, likedSongs);
    list.appendChild(row);
  });
}

function renderCustomPlaylists() {
  // Sidebar Playlists list
  const sidebarList = document.getElementById('sidebar-playlists-list');
  sidebarList.innerHTML = '';

  customPlaylists.forEach(playlist => {
    const item = document.createElement('div');
    item.className = 'sidebar-playlist-item';
    item.textContent = playlist.name;
    item.addEventListener('click', () => showPlaylistDetail(playlist, true));
    sidebarList.appendChild(item);
  });
}

// 8. Liked Songs & Custom Playlists Logic
function toggleLikeSong(song) {
  const index = likedSongs.findIndex(s => s.id === song.id);
  if (index === -1) {
    likedSongs.push(song);
  } else {
    likedSongs.splice(index, 1);
  }
  localStorage.setItem('liked_songs', JSON.stringify(likedSongs));
  renderLikedSongs();
  
  // Sync bottom bar state
  if (currentSong && currentSong.id === song.id) {
    const likeBtn = document.getElementById('btn-player-like');
    likeBtn.querySelector('span').textContent = likedSongs.some(s => s.id === song.id) ? 'favorite' : 'favorite_border';
    if (likedSongs.some(s => s.id === song.id)) {
      likeBtn.classList.add('liked');
    } else {
      likeBtn.classList.remove('liked');
    }
  }
}

function createCustomPlaylist(name, desc) {
  const newPlaylist = {
    id: 'playlist_' + Date.now(),
    name: name,
    desc: desc,
    art: 'https://images.unsplash.com/photo-1494232410401-ad00d5433cfa?w=500&auto=format&fit=crop&q=60',
    songs: []
  };

  customPlaylists.push(newPlaylist);
  localStorage.setItem('custom_playlists', JSON.stringify(customPlaylists));
  renderCustomPlaylists();
}

function addSongToPlaylist(playlistId, song) {
  const playlist = customPlaylists.find(p => p.id === playlistId);
  if (playlist && !playlist.songs.some(s => s.id === song.id)) {
    playlist.songs.push(song);
    localStorage.setItem('custom_playlists', JSON.stringify(customPlaylists));
    renderCustomPlaylists();
  }
}

function removeSongFromPlaylist(playlistId, songId) {
  const playlist = customPlaylists.find(p => p.id === playlistId);
  if (playlist) {
    playlist.songs = playlist.songs.filter(s => s.id !== songId);
    localStorage.setItem('custom_playlists', JSON.stringify(customPlaylists));
    renderCustomPlaylists();
  }
}

// 9. YouTube IFrame API audio-only player integration

// Executed by YouTube script loader
window.onYouTubeIframeAPIReady = () => {
  ytPlayer = new YT.Player('yt-iframe-placeholder', {
    height: '1',
    width: '1',
    videoId: currentSong.id,
    playerVars: {
      'autoplay': 0,
      'controls': 0,
      'disablekb': 1,
      'fs': 0,
      'rel': 0,
      'showinfo': 0,
      'iv_load_policy': 3
    },
    events: {
      'onReady': onPlayerReady,
      'onStateChange': onPlayerStateChange
    }
  });
};

function onPlayerReady(event) {
  playerReady = true;
  ytPlayer.setVolume(volume);
  updatePlaybackUI();
}

function onPlayerStateChange(event) {
  // YT.PlayerState.PLAYING = 1, PAUSED = 2, ENDED = 0, BUFFERING = 3
  if (event.data === YT.PlayerState.PLAYING) {
    isPlaying = true;
    startProgressTimer();
    document.getElementById('btn-play-pause').querySelector('span').textContent = 'pause';
    document.getElementById('full-screen-visualizer').classList.add('playing-wave');
  } else {
    isPlaying = false;
    stopProgressTimer();
    document.getElementById('btn-play-pause').querySelector('span').textContent = 'play_arrow';
    document.getElementById('full-screen-visualizer').classList.remove('playing-wave');
    
    if (event.data === YT.PlayerState.ENDED) {
      handleSongEnded();
    }
  }
}

// 10. Playback Operations
function loadAndPlaySong(song) {
  currentSong = song;
  isPlaying = true;

  // Add to Recently Played
  recentlyPlayed = [song, ...recentlyPlayed.filter(s => s.id !== song.id)].slice(0, 10);
  localStorage.setItem('recently_played', JSON.stringify(recentlyPlayed));
  renderRecentlyPlayed();

  updatePlaybackUI();

  if (playerReady && ytPlayer) {
    ytPlayer.loadVideoById(song.id);
    ytPlayer.setPlaybackRate(playbackSpeed);
  }
}

function togglePlayPause() {
  if (!playerReady || !ytPlayer) return;

  if (isPlaying) {
    ytPlayer.pauseVideo();
  } else {
    ytPlayer.playVideo();
  }
}

function skipNext() {
  if (playQueue.length === 0) return;
  
  if (isShuffle) {
    currentQueueIndex = Math.floor(Math.random() * playQueue.length);
  } else {
    currentQueueIndex = (currentQueueIndex + 1) % playQueue.length;
  }
  loadAndPlaySong(playQueue[currentQueueIndex]);
}

function skipPrevious() {
  if (playQueue.length === 0) return;

  currentQueueIndex = currentQueueIndex - 1;
  if (currentQueueIndex < 0) {
    currentQueueIndex = playQueue.length - 1;
  }
  loadAndPlaySong(playQueue[currentQueueIndex]);
}

function handleSongEnded() {
  if (isRepeat) {
    // If repeat is on, loop queue
    skipNext();
  } else {
    // If not repeat, check if it's the last song in queue
    if (currentQueueIndex < playQueue.length - 1) {
      skipNext();
    } else {
      isPlaying = false;
      document.getElementById('btn-play-pause').querySelector('span').textContent = 'play_arrow';
    }
  }
}

// 11. UI Playback State sync
function updatePlaybackUI() {
  if (!currentSong) return;

  // Bottom player bar sync
  document.getElementById('player-track-art').src = currentSong.artwork || 'icon.png';
  document.getElementById('player-track-name').textContent = currentSong.title;
  document.getElementById('player-track-artist').textContent = currentSong.artist;
  document.getElementById('total-duration').textContent = formatTime(currentSong.duration);

  // Like button state
  const isLiked = likedSongs.some(s => s.id === currentSong.id);
  const likeBtn = document.getElementById('btn-player-like');
  likeBtn.querySelector('span').textContent = isLiked ? 'favorite' : 'favorite_border';
  if (isLiked) {
    likeBtn.classList.add('liked');
  } else {
    likeBtn.classList.remove('liked');
  }

  // Full Screen visualizer panel sync
  document.getElementById('vis-track-art').src = currentSong.artwork || 'icon.png';
  document.getElementById('vis-track-name').textContent = currentSong.title;
  document.getElementById('vis-track-artist').textContent = currentSong.artist;
  document.getElementById('vis-track-album').textContent = currentSong.album || 'Single';

  // Highlight active song row in lists
  document.querySelectorAll('.song-row').forEach(row => {
    row.classList.remove('playing');
  });
  // Search details matching row ID
  document.querySelectorAll('.song-row').forEach(row => {
    // Find the title element text to verify
    const titleEl = row.querySelector('.song-row-title');
    if (titleEl && titleEl.textContent === currentSong.title) {
      row.classList.add('playing');
    }
  });
}

function setupPlayerUI() {
  // Play/pause btn
  document.getElementById('btn-play-pause').addEventListener('click', togglePlayPause);
  document.getElementById('btn-next').addEventListener('click', skipNext);
  document.getElementById('btn-prev').addEventListener('click', skipPrevious);

  // Shuffle & Repeat toggles
  const shuffleBtn = document.getElementById('btn-shuffle');
  shuffleBtn.addEventListener('click', () => {
    isShuffle = !isShuffle;
    shuffleBtn.classList.toggle('active', isShuffle);
  });

  const repeatBtn = document.getElementById('btn-repeat');
  repeatBtn.addEventListener('click', () => {
    isRepeat = !isRepeat;
    repeatBtn.classList.toggle('active', isRepeat);
  });

  // Seek bar implementation
  const seekHitbox = document.getElementById('progress-bar-hitbox');
  seekHitbox.addEventListener('click', (e) => {
    if (!playerReady || !ytPlayer || !currentSong) return;

    const rect = seekHitbox.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const width = rect.width;
    const clickPercentage = clickX / width;
    
    const newTime = clickPercentage * currentSong.duration;
    ytPlayer.seekTo(newTime, true);
    updateProgressFill(newTime);
  });

  // Volume control bar
  const volumeHitbox = document.getElementById('volume-slider-hitbox');
  volumeHitbox.addEventListener('click', (e) => {
    const rect = volumeHitbox.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const width = rect.width;
    const percentage = Math.max(0, Math.min(100, Math.round((clickX / width) * 100)));

    volume = percentage;
    localStorage.setItem('player_volume', volume.toString());
    
    document.getElementById('player-volume-fill').style.width = volume + '%';
    
    if (playerReady && ytPlayer) {
      ytPlayer.setVolume(volume);
      if (volume > 0) {
        isMuted = false;
        ytPlayer.unMute();
      }
    }
    updateVolumeIcon();
  });

  // Mute button
  document.getElementById('btn-mute').addEventListener('click', () => {
    if (!playerReady || !ytPlayer) return;

    isMuted = !isMuted;
    if (isMuted) {
      ytPlayer.mute();
      document.getElementById('player-volume-fill').style.width = '0%';
    } else {
      ytPlayer.unMute();
      document.getElementById('player-volume-fill').style.width = volume + '%';
    }
    updateVolumeIcon();
  });

  // Playback speed selector
  const speedSelect = document.getElementById('playback-speed-select');
  speedSelect.addEventListener('change', (e) => {
    playbackSpeed = parseFloat(e.target.value);
    if (playerReady && ytPlayer) {
      ytPlayer.setPlaybackRate(playbackSpeed);
    }
  });

  // Visualizer toggle
  document.getElementById('btn-expand-visualizer').addEventListener('click', () => {
    document.getElementById('full-screen-visualizer').classList.add('open');
  });

  document.getElementById('btn-close-visualizer').addEventListener('click', () => {
    document.getElementById('full-screen-visualizer').classList.remove('open');
  });

  // Bottom bar like button
  document.getElementById('btn-player-like').addEventListener('click', () => {
    if (currentSong) {
      toggleLikeSong(currentSong);
    }
  });
}

function startProgressTimer() {
  stopProgressTimer();
  updateProgressInterval = setInterval(() => {
    if (playerReady && ytPlayer && isPlaying) {
      const currentTime = ytPlayer.getCurrentTime();
      updateProgressFill(currentTime);
    }
  }, 350);
}

function stopProgressTimer() {
  if (updateProgressInterval) {
    clearInterval(updateProgressInterval);
    updateProgressInterval = null;
  }
}

function updateProgressFill(currentTime) {
  if (!currentSong) return;
  
  const percentage = (currentTime / currentSong.duration) * 100;
  document.getElementById('player-progress-fill').style.width = percentage + '%';
  document.getElementById('current-time').textContent = formatTime(currentTime);
}

function updateVolumeIcon() {
  const icon = document.getElementById('volume-icon');
  if (isMuted || volume === 0) {
    icon.textContent = 'volume_off';
  } else if (volume < 40) {
    icon.textContent = 'volume_down';
  } else {
    icon.textContent = 'volume_up';
  }
}

// 12. Search & Fallback API integration
function setupSearch() {
  const searchInput = document.getElementById('search-input');
  
  searchInput.addEventListener('input', debounce((e) => {
    const query = e.target.value.trim();
    triggerSearch(query);
  }, 600));

  // Voice Search (Speech Recognition simulation)
  document.getElementById('btn-voice-search').addEventListener('click', () => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      const recognition = new SpeechRecognition();
      recognition.lang = 'en-US';
      recognition.interimResults = false;
      recognition.maxAlternatives = 1;

      searchInput.placeholder = "Listening...";
      recognition.start();

      recognition.onresult = (event) => {
        const text = event.results[0][0].transcript;
        searchInput.value = text;
        searchInput.placeholder = "Search artists, songs, albums...";
        triggerSearch(text);
      };

      recognition.onerror = () => {
        searchInput.placeholder = "Speech recognition failed...";
        setTimeout(() => {
          searchInput.placeholder = "Search artists, songs, albums...";
        }, 2000);
      };
    } else {
      alert("Speech recognition is not supported on this platform.");
    }
  });

  // History Tags render
  renderSearchHistory();
  document.getElementById('btn-clear-history').addEventListener('click', () => {
    searchHistory = [];
    localStorage.setItem('search_history', JSON.stringify(searchHistory));
    renderSearchHistory();
  });
}

function triggerSearch(query) {
  const historyBox = document.getElementById('search-history-box');
  const suggestionsBox = document.getElementById('search-suggestions-box');
  const resultsBox = document.getElementById('search-results-section');
  const resultsList = document.getElementById('search-results-list');

  if (!query) {
    resultsBox.style.display = 'none';
    suggestionsBox.style.display = 'flex';
    historyBox.style.display = searchHistory.length > 0 ? 'block' : 'none';
    return;
  }

  // Hide placeholder screen
  suggestionsBox.style.display = 'none';
  historyBox.style.display = 'none';
  resultsBox.style.display = 'block';
  resultsList.innerHTML = `<div class="empty-list-label" style="text-align:center; padding: 20px;">Searching YouTube...</div>`;

  // Add keyword to history
  if (!searchHistory.includes(query)) {
    searchHistory = [query, ...searchHistory].slice(0, 8);
    localStorage.setItem('search_history', JSON.stringify(searchHistory));
    renderSearchHistory();
  }

  // Network Query
  if (settings.offline) {
    // Offline / Local catalog search
    setTimeout(() => {
      const filtered = catalogSongs.filter(s => 
        s.title.toLowerCase().includes(query.toLowerCase()) || 
        s.artist.toLowerCase().includes(query.toLowerCase())
      );
      renderSearchResults(filtered);
    }, 200);
  } else {
    // Web / YouTube Search integration
    searchYouTube(query);
  }
}

function searchYouTube(query) {
  const resultsList = document.getElementById('search-results-list');
  const apiKey = settings.apiKey;

  if (!apiKey) {
    // Fallback to local catalog filter if no key is entered
    setTimeout(() => {
      const filtered = catalogSongs.filter(s => 
        s.title.toLowerCase().includes(query.toLowerCase()) || 
        s.artist.toLowerCase().includes(query.toLowerCase())
      );
      renderSearchResults(filtered);
    }, 150);
    return;
  }

  const url = `https://www.googleapis.com/youtube/v3/search?part=snippet&q=${encodeURIComponent(query)}&type=video&maxResults=12&key=${apiKey}`;

  fetch(url)
    .then(res => res.json())
    .then(data => {
      if (data.error) {
        throw new Error(data.error.message);
      }
      
      const tracks = data.items.map(item => {
        return {
          id: item.id.videoId,
          title: cleanHtmlEntities(item.snippet.title),
          artist: cleanHtmlEntities(item.snippet.channelTitle),
          album: "YouTube Upload",
          artwork: item.snippet.thumbnails?.high?.url || item.snippet.thumbnails?.medium?.url || 'icon.png',
          duration: 300 // Simulated duration for YouTube tracks
        };
      });

      renderSearchResults(tracks);
    })
    .catch(err => {
      console.error("YouTube search error: ", err);
      // Fallback
      const filtered = catalogSongs.filter(s => 
        s.title.toLowerCase().includes(query.toLowerCase()) || 
        s.artist.toLowerCase().includes(query.toLowerCase())
      );
      renderSearchResults(filtered);
    });
}

function renderSearchResults(songs) {
  const resultsList = document.getElementById('search-results-list');
  resultsList.innerHTML = '';

  if (songs.length === 0) {
    resultsList.innerHTML = `<div class="empty-list-label" style="text-align:center; padding: 20px; color: var(--text-secondary);">No songs found. Try another keyword!</div>`;
    return;
  }

  songs.forEach((song, idx) => {
    const row = createSongRow(song, idx, songs);
    
    // Add custom "Add to Playlist" button to search result row
    const addBtn = document.createElement('button');
    addBtn.className = 'song-row-delete-btn';
    addBtn.title = "Add to playlist";
    addBtn.innerHTML = `<span class="material-icons-round">playlist_add</span>`;
    addBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      showAddToPlaylistMenu(song);
    });
    row.insertBefore(addBtn, row.lastElementChild);

    resultsList.appendChild(row);
  });
}

function showAddToPlaylistMenu(song) {
  if (customPlaylists.length === 0) {
    alert("Please create a custom playlist first!");
    return;
  }

  const pNames = customPlaylists.map((p, idx) => `${idx + 1}. ${p.name}`).join('\n');
  const choice = prompt(`Select playlist to add "${song.title}":\n\n${pNames}\n\nEnter playlist index number:`);
  
  if (choice) {
    const idx = parseInt(choice) - 1;
    if (idx >= 0 && idx < customPlaylists.length) {
      addSongToPlaylist(customPlaylists[idx].id, song);
      alert(`Added "${song.title}" to "${customPlaylists[idx].name}".`);
    } else {
      alert("Invalid selection!");
    }
  }
}

function renderSearchHistory() {
  const box = document.getElementById('search-history-box');
  const list = document.getElementById('search-history-tags');
  list.innerHTML = '';

  if (searchHistory.length === 0) {
    box.style.display = 'none';
    return;
  }

  box.style.display = 'block';

  searchHistory.forEach(keyword => {
    const tag = document.createElement('div');
    tag.className = 'history-tag';
    tag.textContent = keyword;
    tag.addEventListener('click', () => {
      document.getElementById('search-input').value = keyword;
      triggerSearch(keyword);
    });
    list.appendChild(tag);
  });
}

// 13. Dialogs, Custom Playlists, & Settings management
function setupDialogs() {
  // Settings Popup
  const settingsOverlay = document.getElementById('settings-dialog');
  document.getElementById('btn-open-settings').addEventListener('click', () => {
    document.getElementById('settings-theme').value = settings.theme;
    document.getElementById('settings-offline-toggle').checked = settings.offline;
    document.getElementById('settings-api-key').value = settings.apiKey;
    settingsOverlay.style.display = 'flex';
  });

  document.getElementById('btn-close-settings').addEventListener('click', () => {
    settingsOverlay.style.display = 'none';
  });

  // Settings Save inside inputs
  document.getElementById('settings-theme').addEventListener('change', (e) => {
    settings.theme = e.target.value;
    localStorage.setItem('settings_theme', settings.theme);
    applySettings();
  });

  document.getElementById('settings-offline-toggle').addEventListener('change', (e) => {
    settings.offline = e.target.checked;
    localStorage.setItem('settings_offline', settings.offline ? 'true' : 'false');
  });

  document.getElementById('settings-api-key').addEventListener('input', (e) => {
    settings.apiKey = e.target.value.trim();
    localStorage.setItem('settings_apikey', settings.apiKey);
  });

  // Create Playlist Popup
  const playlistOverlay = document.getElementById('create-playlist-dialog');
  document.getElementById('btn-create-playlist-sidebar').addEventListener('click', () => {
    document.getElementById('playlist-name-input').value = '';
    document.getElementById('playlist-desc-input').value = '';
    playlistOverlay.style.display = 'flex';
  });

  document.getElementById('btn-close-create-playlist').addEventListener('click', () => {
    playlistOverlay.style.display = 'none';
  });

  document.getElementById('btn-submit-create-playlist').addEventListener('click', () => {
    const name = document.getElementById('playlist-name-input').value.trim();
    const desc = document.getElementById('playlist-desc-input').value.trim();
    if (name) {
      createCustomPlaylist(name, desc);
      playlistOverlay.style.display = 'none';
    } else {
      alert("Please enter a playlist name.");
    }
  });
}

function applySettings() {
  if (settings.theme === 'light') {
    document.body.classList.add('light-theme');
  } else {
    document.body.classList.remove('light-theme');
  }
}

// 14. Utility Methods
function formatTime(secs) {
  if (isNaN(secs) || secs === null) return "0:00";
  const m = Math.floor(secs / 60);
  const s = Math.floor(secs % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}

function shuffleArray(array) {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}

function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

function cleanHtmlEntities(str) {
  const el = document.createElement('div');
  el.innerHTML = str;
  return el.textContent || el.innerText || str;
}
