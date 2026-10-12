package com.openstudy.openstudy.dto;

import java.time.LocalDateTime;

public class RecentActivityDTO {

    private final String activityId;
    private final String type;
    private final String title;
    private final String description;
    private final LocalDateTime occurredAt;
    private final String occurredAtDisplay;

    public RecentActivityDTO(
            String activityId,
            String type,
            String title,
            String description,
            LocalDateTime occurredAt,
            String occurredAtDisplay) {
        this.activityId = activityId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.occurredAt = occurredAt;
        this.occurredAtDisplay = occurredAtDisplay;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getOccurredAtDisplay() {
        return occurredAtDisplay;
    }
}
