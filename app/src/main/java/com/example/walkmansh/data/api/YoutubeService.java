package com.example.walkmansh.data.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YoutubeService {
    @GET("youtube/v3/search")
    Call<SearchResponse> searchVideos(
        @Query("part") String part,
        @Query("q") String query,
        @Query("type") String type,
        @Query("maxResults") int maxResults,
        @Query("key") String apiKey
    );
}
