package com.smartgym.gateway;

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

class JUnitGatewayAuthTest {

    @Test
    void testSum() {
        int a = 2 + 3;
        assertEquals(5, a);
    }

    /*private final String baseUrl = "http://localhost:8080";
    private final RestTemplate rest = new RestTemplate();

    private static String generatedToken;

    @Test @Order(1)
    void testGenerateTokenSuccess() {

        String url = baseUrl + "/auth/generate?clientId=smartgym-client&clientSecret=Smart-Parking";

        ResponseEntity<Map> resp = rest.postForEntity(url, null, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());

        generatedToken = (String) resp.getBody().get("token");

        System.out.println("🔑 TOKEN GENERATO: " + generatedToken);
        assertNotNull(generatedToken);
    }

    @Test @Order(2)
    void testGenerateTokenUnauthorized() {

        String url = baseUrl + "/auth/generate?clientId=wrong&clientSecret=wrong";

        try {
            rest.postForEntity(url, null, String.class);
            fail("Expected 401");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            System.out.println("✅ 401 correctly returned for invalid credentials");
        }
    }

    @Test @Order(3)
    void testProtectedRouteWithoutToken() {

        try {
            rest.getForEntity(baseUrl + "/auth-service/login/ADMIN", String.class);
            fail("Expected 401");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            System.out.println("✅ 401 whithout token correctly returned");
        }
    }

    @Test @Order(4)
    void testProtectedRouteWithValidToken() {

        String url = baseUrl + "/auth/generate?clientId=smartgym-client&clientSecret=Smart-Parking";

        ResponseEntity<Map> resp = rest.postForEntity(url, null, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());

        generatedToken = (String) resp.getBody().get("token");

        System.out.println("🔑 TOKEN GENERATED: " + generatedToken);
        assertNotNull(generatedToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", generatedToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url1 = baseUrl + "/auth-service/login/ADMIN";

        try {
            ResponseEntity<String> resp1 = rest.exchange(url1, HttpMethod.GET, entity, String.class);
            assertEquals(HttpStatus.OK, resp1.getStatusCode());
            System.out.println("✅ Access with valid token granted");
        } catch (HttpClientErrorException ex) {
            fail("Valid token -> shouldn't returns " + ex.getStatusCode());
        }
    }

    @Test @Order(5)
    void testAuthRouteBypassesFilter() {

        String url = baseUrl + "/auth/generate?clientId=smartgym-client&clientSecret=Smart-Parking";

        ResponseEntity<Map> resp = rest.postForEntity(url, null, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        System.out.println("✅ /auth bypassa correttamente il filtro");
    }*/
}

