package com.company.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class AuthFlowTest extends E2ETestBase {

    @Test
    void signUp_returnsUserWithId() {
        String email = randomEmail();
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "%s", "password": "Password123!", "firstName": "John", "lastName": "Doe"}
                        """.formatted(email))
                .when()
                .post("/api/users/signup")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("email", equalTo(email))
                .body("role", equalTo("USER"))
                .body("active", equalTo(true));
    }

    @Test
    void signIn_returnsTokenPair() {
        String email = randomEmail();
        signUp(email, "Password123!", "Jane", "Doe");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "%s", "password": "Password123!"}
                        """.formatted(email))
                .when()
                .post("/api/users/signin")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("expiresIn", greaterThan(0));
    }

    @Test
    void accessProtected_withoutToken_returns401() {
        given()
                .when()
                .get("/api/users")
                .then()
                .statusCode(401);
    }

    @Test
    void accessProtected_withInvalidToken_returns401() {
        given()
                .header("Authorization", "Bearer not.a.valid.jwt.token")
                .when()
                .get("/api/users")
                .then()
                .statusCode(401);
    }

    @Test
    void refresh_returnsNewTokenPair() {
        String email = randomEmail();
        signUp(email, "Password123!", "Alice", "Brown");

        String refreshToken = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "%s", "password": "Password123!"}
                        """.formatted(email))
                .when()
                .post("/api/users/signin")
                .then()
                .statusCode(200)
                .extract()
                .path("refreshToken");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"refreshToken": "%s"}
                        """.formatted(refreshToken))
                .when()
                .post("/api/users/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }
}
