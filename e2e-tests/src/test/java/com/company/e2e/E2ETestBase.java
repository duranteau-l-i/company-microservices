package com.company.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

@Tag("e2e")
public abstract class E2ETestBase {

    protected static final String BASE_URL = "http://localhost:8080";
    protected static final String ADMIN_EMAIL = "admin@company.com";
    protected static final String ADMIN_PASSWORD = "admin123";

    protected static String adminToken;

    @BeforeAll
    static void ensureStackIsRunning() {
        RestAssured.baseURI = BASE_URL;
        try {
            given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Docker Compose stack is not running. Start it with: docker compose up -d\n" +
                "Original error: " + e.getMessage(), e);
        }
    }

    @BeforeAll
    static void signInAsAdmin() {
        RestAssured.baseURI = BASE_URL;
        adminToken = signIn(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected static String signIn(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password))
                .when()
                .post("/api/users/signin")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    protected static String signUp(String email, String password, String firstName, String lastName) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "%s", "password": "%s", "firstName": "%s", "lastName": "%s"}
                        """.formatted(email, password, firstName, lastName))
                .when()
                .post("/api/users/signup")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    protected static String signUpAndSignIn(String email, String password) {
        signUp(email, password, "Test", "User");
        return signIn(email, password);
    }

    protected RequestSpecification auth(String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }

    protected RequestSpecification adminAuth() {
        return auth(adminToken);
    }

    protected String randomEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    protected String randomString() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * Polls {@code request} every 500ms until {@code ready} accepts the response or
     * {@code timeout} elapses. Used to bridge the async Kafka-driven read-model sync
     * (see cqrs-implementation.md) between a write and a dependent read in these tests.
     */
    protected static Response awaitResponse(Supplier<Response> request, Predicate<Response> ready,
                                             Duration timeout, String description) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        Response last;
        do {
            last = request.get();
            if (ready.test(last)) {
                return last;
            }
            sleepQuietly(Duration.ofMillis(500));
        } while (System.currentTimeMillis() < deadline);
        throw new AssertionError("Timed out waiting for: " + description
                + ". Last response: " + last.statusCode() + " " + last.body().asString());
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected String createCompany(String token, String name, String regNumber) {
        return auth(token)
                .body("""
                        {
                          "name": "%s",
                          "registrationNumber": "%s",
                          "street": "123 Main Street",
                          "city": "San Francisco",
                          "postalCode": "94102",
                          "country": "USA",
                          "ownerDisplayName": "Test Owner"
                        }
                        """.formatted(name, regNumber))
                .when()
                .post("/api/companies")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    protected String createOfficerForCompany(String token, String companyId) {
        String body = """
                {
                  "companyId": "%s",
                  "firstName": "Alice",
                  "lastName": "Smith",
                  "dateOfBirth": "1990-01-15",
                  "nationality": "French",
                  "street": "1 Rue de la Paix",
                  "city": "Paris",
                  "postalCode": "75001",
                  "country": "France",
                  "email": "%s",
                  "phone": "+33 1 23 45 67 89",
                  "title": "Director",
                  "appointmentDate": "2024-01-01"
                }
                """.formatted(companyId, randomEmail());

        Response response = awaitResponse(
                () -> auth(token).body(body).when().post("/api/officers"),
                r -> r.statusCode() != 422,
                Duration.ofSeconds(10),
                "officer creation for company " + companyId
                        + " (waiting for officer-service's known_companies projection to sync)");

        return response.then()
                .statusCode(201)
                .extract()
                .path("id")
                .toString();
    }

    protected ValidatableResponse linkOfficer(String token, String officerId, String companyId) {
        String body = """
                {
                  "companyId": "%s",
                  "title": "Secretary",
                  "appointmentDate": "2024-06-01"
                }
                """.formatted(companyId);

        Response response = awaitResponse(
                () -> auth(token).body(body).when().post("/api/officers/" + officerId + "/links"),
                r -> r.statusCode() != 422,
                Duration.ofSeconds(10),
                "officer link to company " + companyId
                        + " (waiting for officer-service's known_companies projection to sync)");

        return response.then();
    }
}
