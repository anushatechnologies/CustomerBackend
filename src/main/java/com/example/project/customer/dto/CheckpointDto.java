package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckpointDto {
    private String status;
    private String title;
    private String location;
    private LocalDateTime timestamp;
    private String description;

    public CheckpointDto() {
    }

    public CheckpointDto(String status, String title, String location, LocalDateTime timestamp, String description) {
        this.status = status;
        this.title = title;
        this.location = location;
        this.timestamp = timestamp;
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
