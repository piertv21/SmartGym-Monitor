package com.smartgym.e2e.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AdminFlowSteps {
    private final Vertx vertx = Vertx.vertx();
    private final WebClient client = WebClient.create(vertx);
    private String token;
    private HttpResponse<?> lastResponse;
    private final String gatewayUrl = "http://localhost:8080";

    @Given("The admin has a valid gateway token")
    public void theAdminHasAValidGatewayToken() {
        JsonObject authRequest = new JsonObject()
                .put("clientId", "smartgym-client")
                .put("clientSecret", "smartgym-secret");
        HttpResponse<?> response = postJsonAndWait(gatewayUrl + "/auth/generate", authRequest, false);
        assertNotNull(response, "Missing response from /auth/generate");
        assertEquals(200, response.statusCode(), "Token generation failed");

        JsonObject body = response.bodyAsJsonObject();
        assertNotNull(body, "Token response body is empty");
        token = body.getString("token");
        assertNotNull(token, "Token not found in /auth/generate response");
    }

    @When("The admin sends valid credentials to login")
    public void theAdminSendsValidCredentialsToLogin() {
        assertNotNull(token, "Gateway token is required before login");

        JsonObject credentials = new JsonObject()
                .put("username", "ADMIN")
                .put("password", "ADMIN");

        lastResponse = postJsonAndWait(gatewayUrl + "/auth-service/login", credentials, true);
    }

    @Then("The login is accepted and tracked")
    public void theLoginIsAcceptedAndTracked() {
        assertNotNull(lastResponse, "Missing login response");
        assertEquals(200, lastResponse.statusCode(), "Login failed");

        JsonObject body = unwrapPayload(lastResponse.bodyAsJsonObject());
        assertNotNull(body, "Login response body is empty");
        assertEquals("ADMIN", body.getString("username"));
        assertEquals("login", body.getString("action"));
        assertNotNull(body.getLong("timestamp"), "Login timestamp is missing");
    }

    @When("The admin sends a logout request")
    public void theAdminSendsALogoutRequest() {
        assertNotNull(token, "Gateway token is required before logout");

        JsonObject payload = new JsonObject().put("username", "ADMIN");
        lastResponse = postJsonAndWait(gatewayUrl + "/auth-service/logout", payload, true);
    }

    @Then("The logout is accepted")
    public void theLogoutIsAccepted() {
        assertNotNull(lastResponse, "Missing logout response");
        assertEquals(200, lastResponse.statusCode(), "Logout failed");

        JsonObject body = unwrapPayload(lastResponse.bodyAsJsonObject());
        assertNotNull(body, "Logout response body is empty");
        assertEquals("Logout effettuato", body.getString("message"));
        assertNotNull(body.getLong("timestamp"), "Logout timestamp is missing");
    }

    private JsonObject unwrapPayload(JsonObject responseBody) {
        if (responseBody == null) {
            return null;
        }

        JsonObject wrapped = responseBody.getJsonObject("map");
        return wrapped != null ? wrapped : responseBody;
    }

    private HttpResponse<?> postJsonAndWait(String url, JsonObject body, boolean withAuth) {
        CompletableFuture<HttpResponse<?>> future = new CompletableFuture<>();

        var request = client.postAbs(url)
                .putHeader("Content-Type", "application/json; charset=UTF-8");

        if (withAuth) {
            request.putHeader("X-Auth-Token", token);
        }

        request.sendJsonObject(body, ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result());
            } else {
                future.completeExceptionally(ar.cause());
            }
        });

        try {
            return future.get(10, TimeUnit.SECONDS);
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



