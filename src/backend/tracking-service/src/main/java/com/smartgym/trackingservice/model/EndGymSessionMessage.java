package com.smartgym.trackingservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndGymSessionMessage {

    @JsonProperty("badgeId")
    private String badgeId;

    public EndGymSessionMessage() {
    }

    public EndGymSessionMessage(String badgeId) {
        this.badgeId = badgeId;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    @Override
    public String toString() {
        return "EndGymSessionMessage{" +
                "badgeId='" + badgeId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        EndGymSessionMessage that = (EndGymSessionMessage) o;
        return badgeId.equals(that.badgeId);
    }

    @Override
    public int hashCode() {
        return badgeId.hashCode();
    }
}