package com.company.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class CompanyCrudTest extends E2ETestBase {

    @Test
    void createCompany_returns201WithId() {
        String token = signUpAndSignIn(randomEmail(), "Password123!");

        String companyId = auth(token)
                .body("""
                        {
                          "name": "Acme Corp",
                          "registrationNumber": "REG-%s",
                          "street": "1 Main St",
                          "city": "London",
                          "postalCode": "EC1A 1BB",
                          "country": "UK",
                          "ownerDisplayName": "Test Owner"
                        }
                        """.formatted(randomString()))
                .when()
                .post("/api/companies")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .path("id");

        assertThat(companyId).isNotNull();
    }

    @Test
    void getOwnCompany_returnsFullView() {
        String email = randomEmail();
        String ownerId = signUp(email, "Password123!", "Owner", "A");
        String token = signIn(email, "Password123!");
        String companyId = createCompany(token, "MyCompany " + randomString(), "REG-" + randomString());

        Response response = awaitResponse(
                () -> auth(token).when().get("/api/companies/" + companyId),
                r -> r.statusCode() == 200,
                Duration.ofSeconds(10),
                "company " + companyId + " to appear in the read model");

        response.then()
                .statusCode(200)
                .body("id", equalTo(companyId))
                .body("ownerId", equalTo(ownerId))
                .body("registrationNumber", notNullValue());
    }

    @Test
    void getOtherCompany_returnsRestrictedView() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String companyId = createCompany(ownerToken, "Their Co " + randomString(), "REG-" + randomString());

        String otherToken = signUpAndSignIn(randomEmail(), "Password123!");

        Response response = awaitResponse(
                () -> auth(otherToken).when().get("/api/companies/" + companyId),
                r -> r.statusCode() == 200,
                Duration.ofSeconds(10),
                "company " + companyId + " to appear in the read model");

        // Restricted view includes id, name, registrationNumber, ownerId, ownerDisplayName, status
        // but NOT officers, address detail, or timestamps
        response.then()
                .statusCode(200)
                .body("id", equalTo(companyId))
                .body("address", nullValue())
                .body("officers", nullValue());
    }

    @Test
    void ownerUpdatesCompany_returns200() {
        String token = signUpAndSignIn(randomEmail(), "Password123!");
        String companyId = createCompany(token, "Before " + randomString(), "REG-" + randomString());

        auth(token)
                .body("""
                        {
                          "name": "After Update",
                          "registrationNumber": "UPD-%s",
                          "street": "99 New St",
                          "city": "Paris",
                          "postalCode": "75001",
                          "country": "France"
                        }
                        """.formatted(randomString()))
                .when()
                .put("/api/companies/" + companyId)
                .then()
                .statusCode(200)
                .body("name", equalTo("After Update"));
    }

    @Test
    void nonOwnerUpdatesCompany_returns403() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String companyId = createCompany(ownerToken, "Protected " + randomString(), "REG-" + randomString());

        String otherToken = signUpAndSignIn(randomEmail(), "Password123!");

        auth(otherToken)
                .body("""
                        {
                          "name": "Hacked",
                          "registrationNumber": "HACK-001",
                          "street": "1 St",
                          "city": "City",
                          "postalCode": "00000",
                          "country": "Anywhere"
                        }
                        """)
                .when()
                .put("/api/companies/" + companyId)
                .then()
                .statusCode(403);
    }

    @Test
    void managerUpdatesAnyCompany_returns200() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String companyId = createCompany(ownerToken, "MgrTest " + randomString(), "REG-" + randomString());

        String mgrEmail = randomEmail();
        adminAuth()
                .body("""
                        {"email": "%s", "password": "Password123!", "firstName": "Mgr", "lastName": "Update", "role": "MANAGER"}
                        """.formatted(mgrEmail))
                .when()
                .post("/api/users")
                .then()
                .statusCode(201);
        String mgrToken = signIn(mgrEmail, "Password123!");

        auth(mgrToken)
                .body("""
                        {
                          "name": "Manager Updated",
                          "registrationNumber": "MGR-%s",
                          "street": "1 Mgr St",
                          "city": "Lyon",
                          "postalCode": "69000",
                          "country": "France"
                        }
                        """.formatted(randomString()))
                .when()
                .put("/api/companies/" + companyId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Manager Updated"));
    }

    @Test
    void ownerDeletesCompany_returns204() {
        String token = signUpAndSignIn(randomEmail(), "Password123!");
        String companyId = createCompany(token, "ToDelete " + randomString(), "REG-" + randomString());

        auth(token)
                .when()
                .delete("/api/companies/" + companyId)
                .then()
                .statusCode(204);
    }

    @Test
    void managerDeletesCompany_returns403() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String companyId = createCompany(ownerToken, "NoDelMgr " + randomString(), "REG-" + randomString());

        String mgrEmail = randomEmail();
        adminAuth()
                .body("""
                        {"email": "%s", "password": "Password123!", "firstName": "Mgr", "lastName": "Del", "role": "MANAGER"}
                        """.formatted(mgrEmail))
                .when()
                .post("/api/users")
                .then()
                .statusCode(201);
        String mgrToken = signIn(mgrEmail, "Password123!");

        auth(mgrToken)
                .when()
                .delete("/api/companies/" + companyId)
                .then()
                .statusCode(403);
    }

    @Test
    void adminDeletesCompany_returns204() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String companyId = createCompany(ownerToken, "AdminDel " + randomString(), "REG-" + randomString());

        adminAuth()
                .when()
                .delete("/api/companies/" + companyId)
                .then()
                .statusCode(204);
    }

    @Test
    void listCompanies_userSeesOwnCompanies() {
        String email = randomEmail();
        signUp(email, "Password123!", "Lister", "User");
        String token = signIn(email, "Password123!");
        createCompany(token, "Listed " + randomString(), "REG-" + randomString());

        Response response = awaitResponse(
                () -> auth(token).when().get("/api/companies"),
                r -> {
                    if (r.statusCode() != 200) {
                        return false;
                    }
                    List<?> companies = r.then().extract().path("$");
                    return companies != null && !companies.isEmpty();
                },
                Duration.ofSeconds(10),
                "user's company list to include the newly created company");

        response.then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void searchCompanies_returnsMatchByName() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String uniqueName = "SearchTarget-" + randomString();
        createCompany(ownerToken, uniqueName, "REG-" + randomString());

        String otherToken = signUpAndSignIn(randomEmail(), "Password123!");

        Response response = awaitResponse(
                () -> auth(otherToken).queryParam("term", uniqueName).when().get("/api/companies/search"),
                r -> {
                    if (r.statusCode() != 200) {
                        return false;
                    }
                    List<?> matches = r.then().extract().path("$");
                    return matches != null && !matches.isEmpty();
                },
                Duration.ofSeconds(10),
                "search results for company " + uniqueName + " to appear");

        response.then()
                .statusCode(200)
                .body("[0].name", equalTo(uniqueName));
    }
}
