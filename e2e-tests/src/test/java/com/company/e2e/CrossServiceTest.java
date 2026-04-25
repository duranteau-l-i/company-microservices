package com.company.e2e;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class CrossServiceTest extends E2ETestBase {

    @Test
    void getCompanyWithLinkedOfficer_returnsOfficersFromOfficerService() throws InterruptedException {
        String email = randomEmail();
        String ownerId = signUp(email, "Password123!", "Cross", "Owner");
        String ownerToken = signIn(email, "Password123!");
        String companyId = createCompany(ownerToken, "CrossCo " + randomString(), "REG-" + randomString());

        createOfficerForCompany(ownerToken, companyId, ownerId);

        // Feign call from company-service to officer-service is synchronous,
        // but officer-service's read model is populated via Kafka (async).
        // Poll until the officer appears in the company response.
        awaitOfficersInCompany(ownerToken, companyId, 1);
    }

    @Test
    void linkOfficerToNonExistentCompany_returns422() {
        String email = randomEmail();
        String ownerId = signUp(email, "Password123!", "Link", "Tester");
        String ownerToken = signIn(email, "Password123!");
        String realCompanyId = createCompany(ownerToken, "RealCo " + randomString(), "REG-" + randomString());

        String officerId = createOfficerForCompany(ownerToken, realCompanyId, ownerId);

        String nonExistentCompanyId = "00000000-0000-0000-0000-000000000000";

        auth(ownerToken)
                .body("""
                        {
                          "companyId": "%s",
                          "companyOwnerId": "%s",
                          "title": "Director",
                          "appointmentDate": "2024-01-01"
                        }
                        """.formatted(nonExistentCompanyId, ownerId))
                .when()
                .post("/api/officers/" + officerId + "/links")
                .then()
                .statusCode(422);
    }

    @Test
    void deleteCompany_deactivatesOfficerLinks() throws InterruptedException {
        String email = randomEmail();
        String ownerId = signUp(email, "Password123!", "Del", "Owner");
        String ownerToken = signIn(email, "Password123!");
        String companyId = createCompany(ownerToken, "DelCo " + randomString(), "REG-" + randomString());
        String officerId = createOfficerForCompany(ownerToken, companyId, ownerId);

        // Ensure officer has the link before deletion (wait for read model sync)
        awaitOfficersInCompany(ownerToken, companyId, 1);

        // Delete the company
        auth(ownerToken)
                .when()
                .delete("/api/companies/" + companyId)
                .then()
                .statusCode(204);

        // Poll: officer's company links should eventually be empty after Kafka propagation
        awaitNoCompanyLinksForOfficer(adminToken, officerId, 15);
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
    private void awaitOfficersInCompany(String token, String companyId, int minCount)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            List<?> officers = given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/companies/" + companyId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("officers");
            if (officers != null && officers.size() >= minCount) {
                return;
            }
            Thread.sleep(500);
        }
        assertThat(false).as("Timed out waiting for officers to appear in company %s", companyId).isTrue();
    }

    // Polls until the officer has no company links remaining.
    private void awaitNoCompanyLinksForOfficer(String token, String officerId, int timeoutSeconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            List<?> links = given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/officers/" + officerId + "/companies")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("companyLinks");
            if (links == null || links.isEmpty()) {
                return;
            }
            Thread.sleep(500);
        }
        assertThat(false).as("Timed out waiting for officer %s links to be deactivated", officerId).isTrue();
    }
}
