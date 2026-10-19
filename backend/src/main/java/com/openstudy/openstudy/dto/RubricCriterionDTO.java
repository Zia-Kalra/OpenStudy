package com.openstudy.openstudy.dto;

public class RubricCriterionDTO {

    private String name;
    private String description;
    private double maxMarks;

    public RubricCriterionDTO() {
    }

    public RubricCriterionDTO(String name, String description, double maxMarks) {
        this.name = name;
        this.description = description;
        this.maxMarks = maxMarks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }
}
