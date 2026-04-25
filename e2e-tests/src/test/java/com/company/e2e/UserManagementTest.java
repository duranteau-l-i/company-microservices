package com.company.e2e;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class UserManagementTest extends E2ETestBase {

    // Admin can create MANAGER and USER; MANAGER can create USER only; USER cannot create anyone.

    @Test
    void adminCreatesManager_returns201() {
        String email = randomEmail();
        adminAuth()
                .body("""
                        {"email": "%s", "password": "Password123!", "firstName": "Mgr", "lastName": "User", "role": "MANAGER"}
                        """.formatted(email))
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .body("role", equalTo("MANAGER"));
    }

    @Test
    void managerCreatesUser_returns201() {
        String mgrEmail = randomEmail();
        String managerToken = createManagerToken(mgrEmail);

        String userEmail = randomEmail();
        auth(managerToken)
                .body("""
                        {"email": "%s", "password": "Password123!", "firstName": "New", "lastName": "User", "role": "USER"}
                        """.formatted(userEmail))
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .body("role", equalTo("USER"));
    }

    @Test
    void managerCreatesManager_returns403() {
        String mgrEmail = randomEmail();
        String managerToken = createManagerToken(mgrEmail);

        auth(managerToken)
                .body("""
                        {"email": "%s", "password": "Password123!", "firstName": "Another", "lastName": "Mgr", "role": "MANAGER"}
                        """.formatted(randomEmail()))
                .when()
                .post("/api/users")
                .then()
                .statusCode(403);
    }

    @Test
    void userCreatesUser_returns403() {
        String userToken = signUpAndSignIn(randomEmail(), "Password123!");

        auth(userToken)
                .body("""
                        {"email": "%s", "password": "Password123!", "firstName": "New", "lastName": "User", "role": "USER"}
                        """.formatted(randomEmail()))
                .when()
                .post("/api/users")
                .then()
                .statusCode(403);
    }

    @Test
    void adminDeletesUser_returns204() {
        String email = randomEmail();
        String userId = signUp(email, "Password123!", "To", "Delete");

        adminAuth()
                .when()
                .delete("/api/users/" + userId)
                .then()
                .statusCode(204);
    }

    @Test
    void nonAdminDeletesUser_returns403() {
        String target = signUp(randomEmail(), "Password123!", "Target", "User");
        String otherUserToken = signUpAndSignIn(randomEmail(), "Password123!");

        auth(otherUserToken)
                .when()
                .delete("/api/users/" + target)
                .then()
                .statusCode(403);
    }

    @Test
    void userGetsOwnProfile_returns200() {
        String email = randomEmail();
        String userId = signUp(email, "Password123!", "Self", "Reader");
        String token = signIn(email, "Password123!");

        auth(token)
                .when()
                .get("/api/users/" + userId)
                .then()
                .statusCode(200)
                .body("id", equalTo(userId))
                .body("email", equalTo(email));
    }

    @Test
    void adminListsAllUsers_returns200WithList() {
        adminAuth()
                .when()
                .get("/api/users")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void userListsUsers_returns403() {
        String userToken = signUpAndSignIn(randomEmail(), "Password123!");

        auth(userToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(403);
    }

    private String createManagerToken(String email) {
        adminAuth()
                .body("""
                        {"email": "%s", "password": "Password123!", "firstName": "Mgr", "lastName": "One", "role": "MANAGER"}
                        """.formatted(email))
                .when()
                .post("/api/users")
                .then()
                .statusCode(201);
        return signIn(email, "Password123!");
    }
}
