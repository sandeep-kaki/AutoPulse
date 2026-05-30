package com.autopulse.tests.api;

import com.autopulse.api.UserEndpoints;
import com.autopulse.utils.ExtentReportManager;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * UserApiTest — API tests for user-related endpoints.
 *
 * NOTICE: This class does NOT extend BaseTest.
 *
 * WHY?
 * BaseTest opens a browser in @BeforeMethod.
 * API tests don't need a browser — they talk
 * directly to the server. No UI involved.
 *
 * Extending BaseTest would waste time opening
 * Chrome for every API test for no reason.
 *
 * API tests are FASTER than UI tests because
 * they skip the entire browser rendering layer.
 * A UI test takes 5-10 seconds. An API test
 * takes 200-500 milliseconds. Same assertion,
 * 20x faster.
 *
 * This is a key SDET interview point —
 * "Why do you have separate API tests when
 *  your UI tests already cover these flows?"
 * Answer: Speed, reliability, and granularity.
 */
public class UserApiTest {

    private UserEndpoints userEndpoints;

    // Test data — using timestamp for unique email
    // So tests don't clash if run multiple times
    private String testEmail = "autopulse_"
            + System.currentTimeMillis()
            + "@testmail.com";
    private String testPassword = "AutoPulse@123";
    private String testName = "AutoPulse User";

    @BeforeClass
    public void setUp() {
        userEndpoints = new UserEndpoints();
        // Init report for API tests
        ExtentReportManager.initReport();
        System.out.println("🔌 API Test Suite Starting...");
        System.out.println("📧 Test email: " + testEmail);
    }

    // ─────────────────────────────────────────────────
    // TEST 1 — Get all products (simplest API test)
    // ─────────────────────────────────────────────────

    /**
     * This test doesn't use UserEndpoints —
     * it directly calls the products list API.
     *
     * WHY start here?
     * GET /api/productsList needs no authentication,
     * no parameters. The simplest possible API call.
     * Perfect to verify your REST Assured setup works
     * before testing complex authenticated endpoints.
     */
    @Test(priority = 1,
            description = "Verify products list API returns 200")
    public void verifyProductsListApi() {
        System.out.println("\n🧪 API Test: Products List");

        ExtentReportManager.createTest(
                "verifyProductsListApi",
                "Verify GET /api/productsList returns 200"
        );

        // Make the API call directly
        Response response = io.restassured.RestAssured
                .given()
                .spec(com.autopulse.api.ApiClient.getSpec())
                .when()
                .get("/productsList")
                .then()
                .extract().response();

        // Log what came back
        int statusCode = response.getStatusCode();
        System.out.println("   Status: " + statusCode);
        System.out.println("   Time: "
                + response.getTime() + "ms");

        ExtentReportManager.logInfo(
                "Response status: " + statusCode
        );
        ExtentReportManager.logInfo(
                "Response time: " + response.getTime() + "ms"
        );

        // ASSERT — status must be 200
        Assert.assertEquals(
                statusCode, 200,
                "Products API should return 200 but got: "
                        + statusCode
        );

        // ASSERT — response body must contain "products"
        Assert.assertTrue(
                response.getBody().asString().contains("products"),
                "Response body should contain 'products' key"
        );

        ExtentReportManager.logPass(
                "Products API returned 200 with product data ✅"
        );

        System.out.println("✅ Products List API verified");
    }

    // ─────────────────────────────────────────────────
    // TEST 2 — Verify login with valid credentials
    // ─────────────────────────────────────────────────

    @Test(priority = 2,
            description = "Verify login API with valid credentials")
    public void verifyValidLoginApi() {
        System.out.println("\n🧪 API Test: Valid Login");

        ExtentReportManager.createTest(
                "verifyValidLoginApi",
                "Verify POST /api/verifyLogin with valid credentials"
        );

        // Use YOUR registered credentials here
        // Same ones you registered on Day 4
        Response response = userEndpoints.verifyLogin(
                "sandeepkaki2213@gmail.com",  // ← your email
                "Sandeep221355@"          // ← your password
        );

        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        System.out.println("   Status: " + statusCode);
        System.out.println("   Body: " + responseBody);

        ExtentReportManager.logInfo("Response: " + responseBody);

        // automationexercise.com returns 200 inside body
        // even though HTTP status is always 200
        // The REAL status is inside the JSON responseCode field
        // This is called "envelope pattern" — status inside body

        Assert.assertEquals(
                statusCode, 200,
                "HTTP status should be 200"
        );

        Assert.assertTrue(
                responseBody.contains("User exists"),
                "Valid login should return 'User exists' message. " +
                        "Got: " + responseBody
        );

        ExtentReportManager.logPass(
                "Valid login API verified ✅"
        );

        System.out.println("✅ Valid Login API verified");
    }

    // ─────────────────────────────────────────────────
    // TEST 3 — Verify login with invalid credentials
    // ─────────────────────────────────────────────────

    @Test(priority = 3,
            description = "Verify login API with invalid credentials")
    public void verifyInvalidLoginApi() {
        System.out.println("\n🧪 API Test: Invalid Login");

        ExtentReportManager.createTest(
                "verifyInvalidLoginApi",
                "Verify POST /api/verifyLogin with wrong credentials"
        );

        Response response = userEndpoints.verifyLogin(
                "nonexistent@email.com",
                "wrongpassword"
        );

        String responseBody = response.getBody().asString();
        System.out.println("   Body: " + responseBody);

        ExtentReportManager.logInfo("Response: " + responseBody);

        // Server should say user not found
        Assert.assertTrue(
                responseBody.contains("404") ||
                        responseBody.contains("User not found"),
                "Invalid login should return 404 or not found message. " +
                        "Got: " + responseBody
        );

        ExtentReportManager.logPass(
                "Invalid login correctly rejected ✅"
        );

        System.out.println("✅ Invalid Login API verified");
    }

    // ─────────────────────────────────────────────────
    // TEST 4 — Create user, verify, then delete
    // Full lifecycle test
    // ─────────────────────────────────────────────────

    /**
     * WHY is this one test instead of three?
     *
     * Because these three operations are DEPENDENT.
     * You can't verify a user without creating first.
     * You can't delete without creating first.
     *
     * This is called a CRUD lifecycle test —
     * Create → Read → Delete
     * Verifies the entire user lifecycle in one flow.
     *
     * priority = 4 ensures this runs AFTER login tests.
     * TestNG runs tests in priority order.
     */
    @Test(priority = 4,
            description = "Full user lifecycle: create, verify, delete")
    public void verifyUserLifecycle() {
        System.out.println("\n🧪 API Test: User Lifecycle");

        ExtentReportManager.createTest(
                "verifyUserLifecycle",
                "Create user → Get user details → Delete user"
        );

        // ── STEP 1: CREATE USER ──────────────────────

        System.out.println("   Step 1: Creating user...");
        ExtentReportManager.logInfo(
                "Creating user: " + testEmail
        );

        Response createResponse = userEndpoints.createUser(
                testName,
                testEmail,
                testPassword,
                "Mr",
                "15",
                "6",
                "1995"
        );

        String createBody = createResponse.getBody().asString();
        System.out.println("   Create response: " + createBody);

        Assert.assertTrue(
                createBody.contains("201") ||
                        createBody.contains("User created"),
                "User creation failed. Response: " + createBody
        );

        ExtentReportManager.logPass(
                "User created: " + testEmail + " ✅"
        );

        // ── STEP 2: VERIFY USER EXISTS ───────────────

        System.out.println("   Step 2: Verifying user exists...");

        Response getResponse = userEndpoints
                .getUserByEmail(testEmail);

        String getBody = getResponse.getBody().asString();
        System.out.println("   Get response: " + getBody);

        Assert.assertTrue(
                getBody.contains(testEmail) ||
                        getBody.contains(testName),
                "User should exist after creation. Got: " + getBody
        );

        ExtentReportManager.logPass(
                "User verified in system ✅"
        );

        // ── STEP 3: DELETE USER ──────────────────────

        System.out.println("   Step 3: Deleting user...");

        Response deleteResponse = userEndpoints
                .deleteUser(testEmail, testPassword);

        String deleteBody = deleteResponse.getBody().asString();
        System.out.println("   Delete response: " + deleteBody);

        Assert.assertTrue(
                deleteBody.contains("200") ||
                        deleteBody.contains("Account deleted"),
                "User deletion failed. Response: " + deleteBody
        );

        ExtentReportManager.logPass(
                "User deleted successfully ✅"
        );

        System.out.println("✅ Full user lifecycle verified");
    }
}