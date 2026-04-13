package com.smartgym.analyticsservice.application.ports;

import com.smartgym.analyticsservice.ddd.Service;
import com.smartgym.analyticsservice.model.AttendanceSeriesResponse;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.GymSessionDurationStat;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsServiceAPI extends Service {

    CompletableFuture<Void> ingestEvent(JsonObject event);

    CompletableFuture<List<AttendanceSnapshot>> getAllAttendanceStats();

    CompletableFuture<AttendanceSeriesResponse> getAttendanceSeries(
            String from, String to, String granularity, String areaId);

    CompletableFuture<GymSessionDurationStat> getGymSessionDurationByDate(String date);
}
