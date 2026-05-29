package com.company.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class OfficerCrudTest extends E2ETestBase {

    private String ownerToken;
    private String companyId;

    @BeforeEach
    void setupUserAndCompany() {
        String email = randomEmail();
        signUp(email, "Password123!", "Officer", "Owner");
        ownerToken = signIn(email, "Password123!");
        companyId = createCompany(ownerToken, "OfficerCo " + randomString(), "REG-" + randomString());
    }

    @Test
    void createOfficer_returns201WithId() {
        String officerId = createOfficerForCompany(ownerToken, companyId);
        assertThat(officerId).isNotNull();
    }

    @Test
    void searchByName_returnsMatches() {
        createOfficerForCompany(ownerToken, companyId);

        auth(ownerToken)
                .queryParam("firstName", "Alice")
                .queryParam("lastName", "Smith")
                .when()
                .get("/api/officers/search")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].firstName", equalTo("Alice"));
    }

    @Test
    void searchByNameAndDob_returnsMatches() {
        createOfficerForCompany(ownerToken, companyId);

        auth(ownerToken)
                .queryParam("firstName", "Alice")
                .queryParam("dateOfBirth", "1990-01-15")
                .when()
                .get("/api/officers/search")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void linkToSecondCompany_returns200() {
        String officerId = createOfficerForCompany(ownerToken, companyId);
        String secondCompanyId = createCompany(ownerToken, "Second Co " + randomString(), "REG-" + randomString());

        linkOfficer(ownerToken, officerId, secondCompanyId)
                .statusCode(200)
                .body("companyLinks", hasSize(greaterThan(1)));
    }

    @Test
    void linkDuplicate_returns409() {
        String officerId = createOfficerForCompany(ownerToken, companyId);

        // Link to the same company again — expect 409 Conflict
        linkOfficer(ownerToken, officerId, companyId)
                .statusCode(409);
    }

    @Test
    void adminGetOfficer_returnsFullView() {
        String officerId = createOfficerForCompany(ownerToken, companyId);

        adminAuth()
                .when()
                .get("/api/officers/" + officerId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("firstName", equalTo("Alice"))
                .body("companyLinks", notNullValue());
    }

    @Test
    void userGetOfficer_returnsRestrictedView() {
        String officerId = createOfficerForCompany(ownerToken, companyId);
        String otherUserToken = signUpAndSignIn(randomEmail(), "Password123!");

        // USER role gets restricted view — has companyLinks but no email, phone, dateOfBirth
        auth(otherUserToken)
                .when()
                .get("/api/officers/" + officerId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("email", nullValue())
                .body("phone", nullValue());
    }

    @Test
    void unlinkFromCompany_returns200WithInactiveLink() {
        String officerId = createOfficerForCompany(ownerToken, companyId);

        // Unlink marks the link inactive (resignationDate set) rather than removing it
        auth(ownerToken)
                .when()
                .delete("/api/officers/" + officerId + "/links/" + companyId)
                .then()
                .statusCode(200)
                .body("companyLinks", hasSize(1))
                .body("companyLinks[0].active", equalTo(false));
    }

    @Test
    void listOfficersByCompany_returnsOfficersForCompany() {
        createOfficerForCompany(ownerToken, companyId);

        auth(ownerToken)
                .when()
                .get("/api/officers/by-company/" + companyId)
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].firstName", equalTo("Alice"));
    }

    @Test
    void listCompaniesByOfficer_returnsCompanyLinks() {
        String officerId = createOfficerForCompany(ownerToken, companyId);

        auth(ownerToken)
                .when()
                .get("/api/officers/" + officerId + "/companies")
                .then()
                .statusCode(200)
                .body("companyLinks", not(empty()));
    }
}
