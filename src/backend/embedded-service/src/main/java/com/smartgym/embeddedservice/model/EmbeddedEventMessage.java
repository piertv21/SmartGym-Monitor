package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddedEventMessage {

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("payload")
    private Object payload;

    public EmbeddedEventMessage() {
    }

    public EmbeddedEventMessage(String eventType, Object payload) {
        this.eventType = eventType;
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public Object getPayload() {
        return payload;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "EmbeddedEventMessage{" +
                "eventType='" + eventType + '\'' +
                ", payload=" + payload +
                '}';
    }
}