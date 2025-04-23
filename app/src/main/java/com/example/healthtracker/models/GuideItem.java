package com.example.healthtracker.models;

public class GuideItem {
    private int iconResource;
    private String title;
    private String description;

    public GuideItem(int iconResource, String title, String description) {
        this.iconResource = iconResource;
        this.title = title;
        this.description = description;
    }

    public int getIconResource() {
        return iconResource;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}