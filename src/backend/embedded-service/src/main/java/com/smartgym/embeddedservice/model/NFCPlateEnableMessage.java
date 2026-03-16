package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NFCPlateEnableMessage {

    @JsonProperty("plate")
    private String plate;

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    @Override
    public String toString() {
        return "NFCPlateEnableMessage{" +
                "plate='" + plate + '\'' +
                '}';
    }
}
