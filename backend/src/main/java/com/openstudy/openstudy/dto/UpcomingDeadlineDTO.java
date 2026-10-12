package com.openstudy.openstudy.dto;

import java.time.LocalDateTime;

public class UpcomingDeadlineDTO {

    private final String itemId;
    private final String itemType;
    private final String title;
    private final LocalDateTime dueAt;
    private final String dueAtDisplay;
    private final String courseLabel;

    public UpcomingDeadlineDTO(
            String itemId,
            String itemType,
            String title,
            LocalDateTime dueAt,
            String dueAtDisplay,
            String courseLabel) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.title = title;
        this.dueAt = dueAt;
        this.dueAtDisplay = dueAtDisplay;
        this.courseLabel = courseLabel;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public String getDueAtDisplay() {
        return dueAtDisplay;
    }

    public String getCourseLabel() {
        return courseLabel;
    }
}
