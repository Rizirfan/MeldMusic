package com.example.walkmansh.data.model;

import java.io.Serializable;

public class Song implements Serializable {
    private String id;
    private String title;
    private String artist;
    private String album;
    private String thumbnailUrl;
    private int durationSeconds;

    public Song() {}

    public Song(String id, String title, String artist, String album, String thumbnailUrl, int durationSeconds) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDurationString() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
