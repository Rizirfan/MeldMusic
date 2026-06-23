package com.example.walkmansh.data.api;

import com.example.walkmansh.data.model.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class YoutubeClient {
    private static YoutubeClient instance;
    private final YoutubeService service;
    private String apiKey = "";

    private YoutubeClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.service = retrofit.create(YoutubeService.class);
    }

    public static synchronized YoutubeClient getInstance() {
        if (instance == null) {
            instance = new YoutubeClient();
        }
        return instance;
    }

    public void setApiKey(String key) {
        this.apiKey = key != null ? key.trim() : "";
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public List<Song> search(String query, int maxResults) throws IOException {
        if (!hasApiKey()) {
            throw new IllegalStateException("API key is not configured");
        }

        Response<SearchResponse> response = service.searchVideos(
            "snippet",
            query,
            "video",
            maxResults,
            apiKey
        ).execute();

        if (response.isSuccessful() && response.body() != null) {
            List<Song> songs = new ArrayList<>();
            SearchResponse searchResponse = response.body();
            if (searchResponse.getItems() != null) {
                for (SearchResponse.SearchItem item : searchResponse.getItems()) {
                    if (item.getId() != null && item.getId().getVideoId() != null) {
                        String videoId = item.getId().getVideoId();
                        String title = "";
                        String artist = "Unknown Artist";
                        String thumbnailUrl = "";

                        if (item.getSnippet() != null) {
                            title = item.getSnippet().getTitle();
                            // YouTube titles are HTML escaped sometimes, let's keep it simple
                            title = title.replaceAll("&quot;", "\"")
                                         .replaceAll("&amp;", "&")
                                         .replaceAll("&#39;", "'")
                                         .replaceAll("&lt;", "<")
                                         .replaceAll("&gt;", ">");
                            
                            artist = item.getSnippet().getChannelTitle();
                            if (item.getSnippet().getThumbnails() != null) {
                                if (item.getSnippet().getThumbnails().getHighThumbnail() != null) {
                                    thumbnailUrl = item.getSnippet().getThumbnails().getHighThumbnail().getUrl();
                                } else if (item.getSnippet().getThumbnails().getMediumThumbnail() != null) {
                                    thumbnailUrl = item.getSnippet().getThumbnails().getMediumThumbnail().getUrl();
                                } else if (item.getSnippet().getThumbnails().getDefaultThumbnail() != null) {
                                    thumbnailUrl = item.getSnippet().getThumbnails().getDefaultThumbnail().getUrl();
                                }
                            }
                        }

                        // Create song with 0 default duration, will be fetched from YouTube Player when playing
                        Song song = new Song(videoId, title, artist, "YouTube Single", thumbnailUrl, 240);
                        songs.add(song);
                    }
                }
            }
            return songs;
        } else {
            String errorMsg = "YouTube API Error: " + response.code();
            if (response.errorBody() != null) {
                errorMsg += " - " + response.errorBody().string();
            }
            throw new IOException(errorMsg);
        }
    }
}
