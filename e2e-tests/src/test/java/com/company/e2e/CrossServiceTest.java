package com.company.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class CrossServiceTest extends E2ETestBase {

    @Test
    void getCompanyWithLinkedOfficer_returnsOfficersFromOfficerService() {
        String email = randomEmail();
        signUp(email, "Password123!", "Cross", "Owner");
        String ownerToken = signIn(email, "Password123!");
        String companyId = createCompany(ownerToken, "CrossCo " + randomString(), "REG-" + randomString());

        createOfficerForCompany(ownerToken, companyId);

        // Officer events are consumed asynchronously to update the company-service read model.
        // Poll until the officer appears in the company response.
        awaitOfficersInCompany(ownerToken, companyId, 1);
    }

    @Test
    void linkOfficerToNonExistentCompany_returns422() {
        String email = randomEmail();
        signUp(email, "Password123!", "Link", "Tester");
        String ownerToken = signIn(email, "Password123!");
        String realCompanyId = createCompany(ownerToken, "RealCo " + randomString(), "REG-" + randomString());

        String officerId = createOfficerForCompany(ownerToken, realCompanyId);

        String nonExistentCompanyId = "00000000-0000-0000-0000-000000000000";

        auth(ownerToken)
                .body("""
                        {
                          "companyId": "%s",
                          "title": "Director",
                          "appointmentDate": "2024-01-01"
                        }
                        """.formatted(nonExistentCompanyId))
                .when()
                .post("/api/officers/" + officerId + "/links")
                .then()
                .statusCode(422);
    }

    @Test
    void deleteCompany_deactivatesOfficerLinks() {
        String email = randomEmail();
        signUp(email, "Password123!", "Del", "Owner");
        String ownerToken = signIn(email, "Password123!");
        String companyId = createCompany(ownerToken, "DelCo " + randomString(), "REG-" + randomString());
        String officerId = createOfficerForCompany(ownerToken, companyId);

        // Ensure officer has the link before deletion (wait for read model sync)
        awaitOfficersInCompany(ownerToken, companyId, 1);

        // Delete the company
        auth(ownerToken)
                .when()
                .delete("/api/companies/" + companyId)
                .then()
                .statusCode(204);

        // Poll: officer's links to the deleted company should eventually be deactivated (active=false)
        awaitOfficerLinksDeactivated(adminToken, officerId, companyId, 15);
    }

    @Test
    @Disabled("Requires manual intervention: stop officer-service container before running " +
              "(docker stop <officer-service-container>), then restart it after")
    void officerServiceDown_getCompany_returnsWithWarning() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String companyId = createCompany(ownerToken, "DownTest " + randomString(), "REG-" + randomString());

        // With officer-service stopped, company-service circuit breaker fires
        // and returns the company with officers=null and a non-empty warnings list.
        auth(ownerToken)
                .when()
                .get("/api/companies/" + companyId)
                .then()
                .statusCode(200)
                .body("officers", nullValue())
                .body("warnings", not(empty()));
    }

    // Polls until the company response contains at least minCount officers.
    private void awaitOfficersInCompany(String token, String companyId, int minCount) {
        awaitResponse(
                () -> given().header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/companies/" + companyId),
                r -> {
                    if (r.statusCode() != 200) {
                        return false;
                    }
                    List<?> officers = r.then().extract().path("officers");
                    return officers != null && officers.size() >= minCount;
                },
                Duration.ofSeconds(10),
                "officers to appear in company " + companyId);
    }

    // Polls until every officer link to the given company is deactivated (active=false).
    private void awaitOfficerLinksDeactivated(String token, String officerId, String companyId, int timeoutSeconds) {
        awaitResponse(
                () -> given().header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/officers/" + officerId + "/companies"),
                r -> {
                    if (r.statusCode() != 200) {
                        return false;
                    }
                    List<java.util.Map<String, Object>> links = r.then().extract().path("companyLinks");
                    if (links == null) {
                        return false;
                    }
                    boolean hasMatch = links.stream()
                            .anyMatch(l -> companyId.equals(String.valueOf(l.get("companyId"))));
                    boolean allDeactivated = links.stream()
                            .filter(l -> companyId.equals(String.valueOf(l.get("companyId"))))
                            .allMatch(l -> Boolean.FALSE.equals(l.get("active")));
                    return hasMatch && allDeactivated;
                },
                Duration.ofSeconds(timeoutSeconds),
                "officer " + officerId + " links to company " + companyId + " to be deactivated");
    }
}
