package com.openstudy.openstudy.dto;

public class QuickActionDTO {

    private final String id;
    private final String label;
    private final String description;
    private final String href;
    private final String owner;
    private final boolean placeholder;

    public QuickActionDTO(
            String id,
            String label,
            String description,
            String href,
            String owner,
            boolean placeholder) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.href = href;
        this.owner = owner;
        this.placeholder = placeholder;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getHref() {
        return href;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }
}
