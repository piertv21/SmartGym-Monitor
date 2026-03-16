package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CmdCommandMessage {

    @JsonProperty("command")
    private String command;

}