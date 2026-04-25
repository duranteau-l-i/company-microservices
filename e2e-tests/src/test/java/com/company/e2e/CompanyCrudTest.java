package com.company.e2e;

import org.junit.jupiter.api.Test;

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

        auth(token)
                .when()
                .get("/api/companies/" + companyId)
                .then()
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

        auth(otherToken)
                .when()
                .get("/api/companies/" + companyId)
                .then()
                .statusCode(200)
                .body("id", equalTo(companyId))
                .body("ownerId", nullValue());
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
                        {"email": "%s", "password": "Password123!", "firstName": "Mgr", "lastName": "Up", "role": "MANAGER"}
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

        auth(token)
                .when()
                .get("/api/companies")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void searchCompanies_returnsMatchByName() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String uniqueName = "SearchTarget-" + randomString();
        createCompany(ownerToken, uniqueName, "REG-" + randomString());

        String otherToken = signUpAndSignIn(randomEmail(), "Password123!");

        auth(otherToken)
                .queryParam("term", uniqueName)
                .when()
                .get("/api/companies/search")
                .then()
                .statusCode(200)
                .body("[0].name", equalTo(uniqueName));
    }
}
