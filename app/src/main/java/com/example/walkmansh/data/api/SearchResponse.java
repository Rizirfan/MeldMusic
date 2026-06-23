package com.example.walkmansh.data.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResponse {
    @SerializedName("items")
    private List<SearchItem> items;

    public List<SearchItem> getItems() {
        return items;
    }

    public void setItems(List<SearchItem> items) {
        this.items = items;
    }

    public static class SearchItem {
        @SerializedName("id")
        private ResourceId id;

        @SerializedName("snippet")
        private Snippet snippet;

        public ResourceId getId() {
            return id;
        }

        public void setId(ResourceId id) {
            this.id = id;
        }

        public Snippet getSnippet() {
            return snippet;
        }

        public void setSnippet(Snippet snippet) {
            this.snippet = snippet;
        }
    }

    public static class ResourceId {
        @SerializedName("kind")
        private String kind;

        @SerializedName("videoId")
        private String videoId;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getVideoId() {
            return videoId;
        }

        public void setVideoId(String videoId) {
            this.videoId = videoId;
        }
    }

    public static class Snippet {
        @SerializedName("title")
        private String title;

        @SerializedName("description")
        private String description;

        @SerializedName("channelTitle")
        private String channelTitle;

        @SerializedName("thumbnails")
        private Thumbnails thumbnails;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getChannelTitle() {
            return channelTitle;
        }

        public void setChannelTitle(String channelTitle) {
            this.channelTitle = channelTitle;
        }

        public Thumbnails getThumbnails() {
            return thumbnails;
        }

        public void setThumbnails(Thumbnails thumbnails) {
            this.thumbnails = thumbnails;
        }
    }

    public static class Thumbnails {
        @SerializedName("default")
        private ThumbnailDetails defaultThumbnail;

        @SerializedName("medium")
        private ThumbnailDetails mediumThumbnail;

        @SerializedName("high")
        private ThumbnailDetails highThumbnail;

        public ThumbnailDetails getDefaultThumbnail() {
            return defaultThumbnail;
        }

        public void setDefaultThumbnail(ThumbnailDetails defaultThumbnail) {
            this.defaultThumbnail = defaultThumbnail;
        }

        public ThumbnailDetails getMediumThumbnail() {
            return mediumThumbnail;
        }

        public void setMediumThumbnail(ThumbnailDetails mediumThumbnail) {
            this.mediumThumbnail = mediumThumbnail;
        }

        public ThumbnailDetails getHighThumbnail() {
            return highThumbnail;
        }

        public void setHighThumbnail(ThumbnailDetails highThumbnail) {
            this.highThumbnail = highThumbnail;
        }
    }

    public static class ThumbnailDetails {
        @SerializedName("url")
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
