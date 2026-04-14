package com.loyalty.mediation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Component-level tests for the mediation service enrolment API.
 *
 * The vendor API is stubbed with WireMock — no vendor-mock service needs to be running.
 * Solace is disabled via the "test" profile (subscription.enabled=false).
 * The SolaceConfig bean is mocked so no Solace connection is attempted.
 *
 * Run: mvn test -pl mediation-service
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EnrollmentApiTest {

    // Disable the real Solace session creation
    @MockBean
    com.solacesystems.jcsmp.JCSMPSession jcsmpSession;

    @LocalServerPort
    private int port;

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void overrideVendorUrl(DynamicPropertyRegistry registry) {
        registry.add("vendor.base-url", () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void setupRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
        wireMock.resetAll();
    }

    // ── Happy Path ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /enroll — 200 when vendor accepts the request")
    void enroll_success() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/vendor/enroll"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Request accepted\"}")));

        given()
            .contentType(ContentType.JSON)
            .body(validEnrollmentPayload())
        .when()
            .post("/enroll")
        .then()
            .statusCode(200)
            .body("message", equalTo("Enrollment request accepted"))
            .body("correlationId", notNullValue());
    }

    // ── Validation Errors ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /enroll — 400 when firstName is blank")
    void enroll_validationFails_missingFirstName() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"firstName\":\"\",\"lastName\":\"Smith\",\"dateOfBirth\":\"1990-01-15\",\"country\":\"AU\"}")
        .when()
            .post("/enroll")
        .then()
            .statusCode(400)
            .body("message", notNullValue());
    }

    @Test
    @DisplayName("POST /enroll — 400 when dateOfBirth is missing")
    void enroll_validationFails_missingDob() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"firstName\":\"Jane\",\"lastName\":\"Smith\",\"country\":\"AU\"}")
        .when()
            .post("/enroll")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /enroll — 400 when country code is not 2 characters")
    void enroll_validationFails_invalidCountry() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"firstName\":\"Jane\",\"lastName\":\"Smith\",\"dateOfBirth\":\"1990-01-15\",\"country\":\"AUSTRALIA\"}")
        .when()
            .post("/enroll")
        .then()
            .statusCode(400);
    }

    // ── Vendor Error Scenarios ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /enroll — 400 when vendor returns 400 bad request")
    void enroll_vendorReturns400() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/vendor/enroll"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Invalid date of birth format\"}")));

        given()
            .contentType(ContentType.JSON)
            .body(validEnrollmentPayload())
        .when()
            .post("/enroll")
        .then()
            .statusCode(400)
            .body("message", notNullValue());
    }

    @Test
    @DisplayName("POST /enroll — 422 when vendor returns 422 unprocessable entity")
    void enroll_vendorReturns422() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/vendor/enroll"))
                .willReturn(aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Member already exists\"}")));

        given()
            .contentType(ContentType.JSON)
            .body(validEnrollmentPayload())
        .when()
            .post("/enroll")
        .then()
            .statusCode(422)
            .body("message", notNullValue());
    }

    @Test
    @DisplayName("POST /enroll — 502 when vendor returns 500 server error")
    void enroll_vendorReturns500() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/vendor/enroll"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Internal server error\"}")));

        given()
            .contentType(ContentType.JSON)
            .body(validEnrollmentPayload())
        .when()
            .post("/enroll")
        .then()
            .statusCode(502)
            .body("message", notNullValue());
    }

    @Test
    @DisplayName("POST /enroll — 502 when vendor is unreachable")
    void enroll_vendorUnreachable() {
        // No stub — WireMock will return a connection refused on an unregistered path
        wireMock.stubFor(post(urlEqualTo("/api/v1/vendor/enroll"))
                .willReturn(aResponse().withFault(
                        com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        given()
            .contentType(ContentType.JSON)
            .body(validEnrollmentPayload())
        .when()
            .post("/enroll")
        .then()
            .statusCode(502);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String validEnrollmentPayload() {
        return """
                {
                  "firstName": "Jane",
                  "lastName": "Smith",
                  "dateOfBirth": "1990-06-15",
                  "country": "AU"
                }
                """;
    }
}
