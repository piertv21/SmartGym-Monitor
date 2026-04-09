package com.smartgym.e2e.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MemberFlowSteps {
    private static final String ADMIN_USERNAME = "ADMIN";
    private static final String ADMIN_PASSWORD = "ADMIN";
    private static final String BADGE_ID = "E2E-BADGE-USER-01";
    private static final int MAX_503_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 1000;

    private final Vertx vertx = Vertx.vertx();
    private final WebClient client = WebClient.create(vertx);
    private final String gatewayUrl = "http://localhost:8080";

    private String accessToken;
    private HttpResponse<?> lastResponse;
    private long gymCountBaseline;
    private int areaCountBaseline;

    @Given("A member is authenticated for usage flows")
    public void aMemberIsAuthenticatedForUsageFlows() {
        JsonObject credentials = new JsonObject()
                .put("username", ADMIN_USERNAME)
                .put("password", ADMIN_PASSWORD);

        HttpResponse<?> loginResponse = postJsonAndWait(gatewayUrl + "/auth-service/login", credentials, null);
        assertNotNull(loginResponse, "Missing login response");
        assertEquals(200, loginResponse.statusCode(), "Login failed for usage flows");

        JsonObject payload = unwrapPayload(loginResponse.bodyAsJsonObject());
        assertNotNull(payload, "Login payload is empty");

        accessToken = payload.getString("accessToken");
        assertNotNull(accessToken, "Access token is missing");
        assertFalse(accessToken.isBlank(), "Access token is blank");
        lastResponse = null;
    }

    @And("The baseline gym attendance is stored")
    public void theBaselineGymAttendanceIsStored() {
        gymCountBaseline = getGymCount();
    }

    @When("The member enters the gym")
    public void theMemberEntersTheGym() {
        lastResponse = postJsonAndWait(
                gatewayUrl + "/tracking-service/start-session",
                new JsonObject().put("badgeId", BADGE_ID),
                accessToken
        );
    }

    @Then("The gym attendance increases by 1")
    public void theGymAttendanceIncreasesBy1() {
        assertSuccess(lastResponse, "Gym entry request failed");
        assertEquals(gymCountBaseline + 1, getGymCount(), "Gym count did not increase as expected");
    }

    @When("The member exits the gym")
    public void theMemberExitsTheGym() {
        lastResponse = postJsonAndWait(
                gatewayUrl + "/tracking-service/end-session",
                new JsonObject().put("badgeId", BADGE_ID),
                accessToken
        );
    }

    @Then("The gym attendance returns to baseline")
    public void theGymAttendanceReturnsToBaseline() {
        assertSuccess(lastResponse, "Gym exit request failed");
        assertEquals(gymCountBaseline, getGymCount(), "Gym count did not return to baseline");
    }

    @And("The baseline for area {string} is stored")
    public void theBaselineForAreaIsStored(String areaId) {
        areaCountBaseline = getAreaCount(areaId);
    }

    @When("The member enters area {string}")
    public void theMemberEntersArea(String areaId) {
        lastResponse = postJsonAndWait(
                gatewayUrl + "/area-service/access",
                areaMessage(areaId, "IN"),
                accessToken
        );
    }

    @Then("The area count for {string} increases by 1")
    public void theAreaCountForIncreasesBy1(String areaId) {
        assertSuccess(lastResponse, "Area entry request failed");
        assertEquals(areaCountBaseline + 1, getAreaCount(areaId), "Area count did not increase as expected");
    }

    @When("The member exits area {string}")
    public void theMemberExitsArea(String areaId) {
        lastResponse = postJsonAndWait(
                gatewayUrl + "/area-service/exit",
                areaMessage(areaId, "OUT"),
                accessToken
        );
    }

    @Then("The area count for {string} returns to baseline")
    public void theAreaCountForReturnsToBaseline(String areaId) {
        assertSuccess(lastResponse, "Area exit request failed");
        assertEquals(areaCountBaseline, getAreaCount(areaId), "Area count did not return to baseline");
    }

    @And("Machine {string} is available")
    public void machineIsAvailable(String machineId) {
        JsonObject machine = getMachine(machineId);
        String status = machine.getString("status");
        assertNotNull(status, "Machine status is missing");

        if ("OCCUPIED".equalsIgnoreCase(status)) {
            HttpResponse<?> endResponse = postJsonAndWait(
                    gatewayUrl + "/machine-service/end-session",
                    new JsonObject().put("machineId", machineId),
                    accessToken
            );
            assertSuccess(endResponse, "Unable to release occupied machine before scenario");
            machine = getMachine(machineId);
            status = machine.getString("status");
        }

        assertEquals("FREE", status, "Machine is not free at scenario start");
    }

    @When("The member starts a session on machine {string}")
    public void theMemberStartsASessionOnMachine(String machineId) {
        lastResponse = postJsonAndWait(
                gatewayUrl + "/machine-service/start-session",
                new JsonObject()
                        .put("machineId", machineId)
                        .put("badgeId", BADGE_ID),
                accessToken
        );
    }

    @Then("Machine {string} is occupied")
    public void machineIsOccupied(String machineId) {
        assertSuccess(lastResponse, "Machine start-session request failed");
        JsonObject machine = getMachine(machineId);
        assertEquals("OCCUPIED", machine.getString("status"), "Machine was not marked as occupied");
    }

    @When("The member ends the session on machine {string}")
    public void theMemberEndsTheSessionOnMachine(String machineId) {
        lastResponse = postJsonAndWait(
                gatewayUrl + "/machine-service/end-session",
                new JsonObject().put("machineId", machineId),
                accessToken
        );
    }

    @Then("Machine {string} is free")
    public void machineIsFree(String machineId) {
        assertSuccess(lastResponse, "Machine end-session request failed");
        JsonObject machine = getMachine(machineId);
        assertEquals("FREE", machine.getString("status"), "Machine did not return to FREE");
    }

    @When("The member completes a full workout journey in area {string} using machine {string}")
    public void theMemberCompletesAFullWorkoutJourneyInAreaUsingMachine(String areaId, String machineId) {
        HttpResponse<?> startGym = postJsonAndWait(
                gatewayUrl + "/tracking-service/start-session",
                new JsonObject().put("badgeId", BADGE_ID),
                accessToken
        );
        assertSuccess(startGym, "Gym start failed in complete journey");

        HttpResponse<?> enterArea = postJsonAndWait(
                gatewayUrl + "/area-service/access",
                areaMessage(areaId, "IN"),
                accessToken
        );
        assertSuccess(enterArea, "Area entry failed in complete journey");

        HttpResponse<?> startMachine = postJsonAndWait(
                gatewayUrl + "/machine-service/start-session",
                new JsonObject().put("machineId", machineId).put("badgeId", BADGE_ID),
                accessToken
        );
        assertSuccess(startMachine, "Machine start failed in complete journey");

        HttpResponse<?> endMachine = postJsonAndWait(
                gatewayUrl + "/machine-service/end-session",
                new JsonObject().put("machineId", machineId),
                accessToken
        );
        assertSuccess(endMachine, "Machine end failed in complete journey");

        HttpResponse<?> exitArea = postJsonAndWait(
                gatewayUrl + "/area-service/exit",
                areaMessage(areaId, "OUT"),
                accessToken
        );
        assertSuccess(exitArea, "Area exit failed in complete journey");

        lastResponse = postJsonAndWait(
                gatewayUrl + "/tracking-service/end-session",
                new JsonObject().put("badgeId", BADGE_ID),
                accessToken
        );
    }

    private long getGymCount() {
        HttpResponse<?> response = getJsonAndWait(gatewayUrl + "/tracking-service/count", accessToken);
        assertSuccess(response, "Unable to read gym count");

        JsonObject payload = unwrapPayload(response.bodyAsJsonObject());
        assertNotNull(payload, "Gym count payload is empty");

        Number gymCount = payload.getNumber("gymCount");
        assertNotNull(gymCount, "gymCount field is missing");
        return gymCount.longValue();
    }

    private int getAreaCount(String areaId) {
        HttpResponse<?> response = getAreaById(areaId);
        assertSuccess(response, "Unable to read area status");

        JsonObject payload = unwrapPayload(response.bodyAsJsonObject());
        assertNotNull(payload, "Area payload is empty");

        Integer currentCount = payload.getInteger("currentCount");
        assertNotNull(currentCount, "currentCount field is missing");
        return currentCount;
    }

    private JsonObject getMachine(String machineId) {
        HttpResponse<?> response = getJsonAndWait(gatewayUrl + "/machine-service/" + machineId, accessToken);
        assertSuccess(response, "Unable to read machine status");

        JsonObject payload = unwrapPayload(response.bodyAsJsonObject());
        assertNotNull(payload, "Machine payload is empty");
        return payload;
    }

    private JsonObject areaMessage(String areaId, String direction) {
        return new JsonObject()
                .put("deviceId", "e2e-device")
                .put("timeStamp", Instant.now().toString())
                .put("badgeId", BADGE_ID)
                .put("areaId", areaId)
                .put("direction", direction);
    }

    private HttpResponse<?> getAreaById(String areaId) {
        return getJsonAndWait(gatewayUrl + "/area-service/" + areaId, accessToken);
    }

    private HttpResponse<?> postJsonAndWait(String url, JsonObject body, String bearerToken) {
        return postJsonAndWait(url, body, bearerToken, MAX_503_RETRIES);
    }

    private HttpResponse<?> getJsonAndWait(String url, String bearerToken) {
        return getJsonAndWait(url, bearerToken, MAX_503_RETRIES);
    }

    private HttpResponse<?> postJsonAndWait(String url, JsonObject body, String bearerToken, int retriesLeft) {
        CompletableFuture<HttpResponse<?>> future = new CompletableFuture<>();

        var request = client.postAbs(url)
                .putHeader("Content-Type", "application/json; charset=UTF-8");

        if (bearerToken != null) {
            request.putHeader("Authorization", "Bearer " + bearerToken);
        }

        request.sendJsonObject(body, ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result());
            } else {
                future.completeExceptionally(ar.cause());
            }
        });

        try {
            HttpResponse<?> response = future.get(10, TimeUnit.SECONDS);
            if (response != null && response.statusCode() == 503 && retriesLeft > 0) {
                TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                return postJsonAndWait(url, body, bearerToken, retriesLeft - 1);
            }
            return response;
        } catch (Exception e) {
            fail("Request failed to " + url + " : " + e.getMessage());
            return null;
        }
    }

    private HttpResponse<?> getJsonAndWait(String url, String bearerToken, int retriesLeft) {
        CompletableFuture<HttpResponse<?>> future = new CompletableFuture<>();

        var request = client.getAbs(url)
                .putHeader("Content-Type", "application/json; charset=UTF-8");

        if (bearerToken != null) {
            request.putHeader("Authorization", "Bearer " + bearerToken);
        }

        request.send(ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result());
            } else {
                future.completeExceptionally(ar.cause());
            }
        });

        try {
            HttpResponse<?> response = future.get(10, TimeUnit.SECONDS);
            if (response != null && response.statusCode() == 503 && retriesLeft > 0) {
                TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                return getJsonAndWait(url, bearerToken, retriesLeft - 1);
            }
            return response;
        } catch (Exception e) {
            fail("Request failed to " + url + " : " + e.getMessage());
            return null;
        }
    }

    private JsonObject unwrapPayload(JsonObject responseBody) {
        if (responseBody == null) {
            return null;
        }
        JsonObject wrapped = responseBody.getJsonObject("map");
        return wrapped != null ? wrapped : responseBody;
    }

    private void assertSuccess(HttpResponse<?> response, String message) {
        assertNotNull(response, message + " (missing response)");
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                message + " (status=" + response.statusCode() + ", body=" + response.bodyAsString() + ")");
    }

    @After("@gym or @area or @machine or @journey")
    public void tearDown() {
        vertx.close();
    }
}

