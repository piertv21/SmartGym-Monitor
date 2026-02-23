package com.smartgym.authservice;

import com.mongodb.client.*;
import com.smartgym.authservice.model.LoginMessage;
import com.smartgym.authservice.model.LogoutMessage;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JUnitAuthServiceTest {

    private final String baseUrl = "http://localhost:8081";
    private final RestTemplate rest = new RestTemplate();

    private static final String mongoUrl = "mongodb://localhost:27017";
    private static final String databaseName = "authservicedb";

    private static final String logsCollection = "logs";
    private static final String usersCollection = "users";

    private static final String USERNAME = "ADMIN";
    private static final String PASSWORD = "ADMIN";

    @BeforeAll
    static void cleanMongoOnce() {
        try (MongoClient client = MongoClients.create(mongoUrl)) {
            MongoDatabase db = client.getDatabase(databaseName);

            db.getCollection(logsCollection).deleteMany(new Document());
            db.getCollection(usersCollection).deleteMany(new Document());

            db.getCollection(usersCollection).insertOne(
                    new Document("username", USERNAME)
                            .append("password", PASSWORD)
            );

            System.out.println("🔥 AuthService Mongo cleaned + ADMIN user recreated");
        }
    }

    @Test
    @Order(1)
    void testLoginSuccess() {
        LoginMessage msg = new LoginMessage(USERNAME, PASSWORD);

        ResponseEntity<String> resp = rest.postForEntity(
                baseUrl + "/login",
                msg,
                String.class
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains(USERNAME));

        System.out.println("✅ Login riuscito");
    }

    @Test
    @Order(2)
    void testVerifyUser() {
        ResponseEntity<String> resp =
                rest.getForEntity(baseUrl + "/login/" + USERNAME, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("\"exists\":true"));

        System.out.println("✅ User exists verificato");
    }

    @Test
    @Order(3)
    void testLogout() {
        LogoutMessage logout = new LogoutMessage(USERNAME, PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LogoutMessage> entity = new HttpEntity<>(logout, headers);

        ResponseEntity<String> resp = rest.exchange(
                baseUrl + "/logout",
                HttpMethod.POST,   // POST, NON GET
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("Logout effettuato"));

        System.out.println("✅ Logout OK");
    }

    @Test
    @Order(4)
    void testLoginWrongPassword() {
        LoginMessage msg = new LoginMessage(USERNAME, "Wrong password");

        try {
            rest.postForEntity(baseUrl + "/login", msg, String.class);
            fail("Expected 403 UNAUTHORIZED");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            System.out.println("✅ Login con password errata restituisce 403 correttamente");
        }
    }
}
