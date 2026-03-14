package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NfcReaderMessage {

    @JsonProperty("timeStamp")
    private String timeStamp;

    @JsonProperty("date")
    private String date;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("nfcData")
    private String nfcData;

    public NfcReaderMessage() {}

    public NfcReaderMessage(String timeStamp, String date, String deviceId, String tagId) {
        this.timeStamp = timeStamp;
        this.date = date;
        this.deviceId = deviceId;
        this.nfcData = tagId;
    }

    public String getTimeStamp() { return timeStamp; }
    public void setTimeStamp(String timeStamp) { this.timeStamp = timeStamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getNfcData() { return nfcData; }
    public void setNfcData(String tagId) { this.nfcData = tagId; }

    @Override
    public String toString() {
        return "NfcMessage{" +
                "timeStamp='" + timeStamp + '\'' +
                ", date='" + date + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", tagId='" + nfcData + '\'' +
                '}';
    }
}

