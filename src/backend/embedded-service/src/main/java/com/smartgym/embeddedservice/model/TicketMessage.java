package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketMessage {

    private String id;
    private String type;
    private String associatedWithPlate;
    private boolean exitDetected;
    private String status;
    private String createdAt;   // String → per non avere problemi di parsing
    private long timestamp;

    public TicketMessage() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAssociatedWithPlate() {
        return associatedWithPlate;
    }

    public void setAssociatedWithPlate(String associatedWithPlate) {
        this.associatedWithPlate = associatedWithPlate;
    }

    public boolean isExitDetected() {
        return exitDetected;
    }

    public void setExitDetected(boolean exitDetected) {
        this.exitDetected = exitDetected;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TicketMessage{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", associatedWithPlate='" + associatedWithPlate + '\'' +
                ", exitDetected=" + exitDetected +
                ", status='" + status + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
