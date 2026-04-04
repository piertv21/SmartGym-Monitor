package com.smartgym.e2e.steps;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AdminFlowSteps {
    private final Vertx vertx = Vertx.vertx();
    private final WebClient client = WebClient.create(vertx);
    private static final String ADMIN_USERNAME = "ADMIN";
    private static final String ADMIN_PASSWORD = "ADMIN";

    private String accessToken;
    private HttpResponse<?> lastResponse;
    private final String gatewayUrl = "http://localhost:8080";
    private final String authMongoUri = "mongodb://localhost:27017";
    private static final int MAX_503_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 1000;
    private long loginCountBeforeScenario;
    private long logoutCountBeforeScenario;

    @Given("The admin credentials are valid")
    public void theAdminCredentialsAreValid() {
        accessToken = null;
        lastResponse = null;
        loginCountBeforeScenario = countAuthLogsByAction(ADMIN_USERNAME, "LOGIN");
        logoutCountBeforeScenario = countAuthLogsByAction(ADMIN_USERNAME, "LOGOUT");
    }

    @When("The admin sends valid credentials to login")
    public void theAdminSendsValidCredentialsToLogin() {
        JsonObject credentials = new JsonObject()
                .put("username", ADMIN_USERNAME)
                .put("password", ADMIN_PASSWORD);

        lastResponse = postJsonAndWait(gatewayUrl + "/auth-service/login", credentials, null);
    }

    @Then("The login is accepted and tracked")
    public void theLoginIsAcceptedAndTracked() {
        assertNotNull(lastResponse, "Missing login response");
        assertEquals(200, lastResponse.statusCode(), "Login failed");

        JsonObject body = unwrapPayload(lastResponse.bodyAsJsonObject());
        assertNotNull(body, "Login response body is empty");
        assertEquals("Bearer", body.getString("tokenType"), "Unexpected token type");

        accessToken = body.getString("accessToken");
        assertNotNull(accessToken, "Access token is missing");
        assertFalse(accessToken.isBlank(), "Access token is blank");

        Integer expiresIn = body.getInteger("expiresIn");
        assertNotNull(expiresIn, "Token expiration is missing");
        assertTrue(expiresIn > 0, "Token expiration must be positive");

        long loginCountAfter = countAuthLogsByAction(ADMIN_USERNAME, "LOGIN");
        assertTrue(
                loginCountAfter >= loginCountBeforeScenario + 1,
                "Login event was not tracked in MongoDB logs"
        );
    }

    @When("The admin sends a logout request with the access token")
    public void theAdminSendsALogoutRequestWithTheAccessToken() {
        assertNotNull(accessToken, "Access token is required before logout");
        lastResponse = postJsonAndWait(gatewayUrl + "/auth-service/logout", new JsonObject(), accessToken);
    }

    @Then("The logout is accepted")
    public void theLogoutIsAccepted() {
        assertNotNull(lastResponse, "Missing logout response");
        assertEquals(200, lastResponse.statusCode(), "Logout failed");

        JsonObject body = unwrapPayload(lastResponse.bodyAsJsonObject());
        assertNotNull(body, "Logout response body is empty");
        assertEquals("Logout effettuato", body.getString("message"));
        String timestamp = body.getString("timestamp");
        assertNotNull(timestamp, "Logout timestamp is missing");
        assertFalse(timestamp.isBlank(), "Logout timestamp is blank");

        long logoutCountAfter = countAuthLogsByAction(ADMIN_USERNAME, "LOGOUT");
        assertTrue(
                logoutCountAfter >= logoutCountBeforeScenario + 1,
                "Logout event was not tracked in MongoDB logs"
        );
    }

    private long countAuthLogsByAction(String username, String action) {
        try (MongoClient mongoClient = MongoClients.create(authMongoUri)) {
            MongoDatabase database = mongoClient.getDatabase("authservicedb");
            MongoCollection<Document> logs = database.getCollection("logs");
            return logs.countDocuments(
                    Filters.and(
                            Filters.eq("username", username),
                            Filters.eq("action", action)
                    )
            );
        } catch (Exception e) {
            fail("Unable to query auth tracking logs: " + e.getMessage());
            return -1;
        }
    }

    private HttpResponse<?> postJsonAndWait(String url, JsonObject body, String bearerToken) {
        return postJsonAndWait(url, body, bearerToken, MAX_503_RETRIES);
    }

    private JsonObject unwrapPayload(JsonObject responseBody) {
        if (responseBody == null) {
            return null;
        }
        JsonObject wrapped = responseBody.getJsonObject("map");
        return wrapped != null ? wrapped : responseBody;
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

    @After("@auth")
    public void tearDown() {
        vertx.close();
    }
}



