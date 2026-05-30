package com.autopulse.api;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * UserEndpoints — API calls related to User operations.
 *
 * RESPONSIBILITY:
 * Makes HTTP calls to user-related API endpoints.
 * Returns raw Response objects to the test classes.
 *
 * WHAT IS Response?
 * REST Assured's Response object holds everything
 * that came back from the server:
 * - Status code (200, 400, 404 etc.)
 * - Response body (JSON data)
 * - Headers
 * - Response time
 *
 * WHY return Response instead of asserting here?
 * Endpoints class does ONE job — make the call.
 * Assertions belong in the Test class.
 * This separation means the same endpoint method
 * can be reused in many different test scenarios.
 *
 * ANALOGY:
 * A telephone operator connects your call.
 * They don't listen to your conversation or
 * judge what you say. They just connect.
 * That's what endpoint methods do — they connect
 * and return what came back. Tests do the judging.
 */
public class UserEndpoints {

    /**
     * getVerifyLogin() — Verifies user login credentials.
     *
     * ENDPOINT: POST /api/verifyLogin
     * TYPE: Form Data (not JSON)
     * SUCCESS: { "responseCode": 200, "message": "User exists!" }
     * FAILURE: { "responseCode": 404, "message": "User not found!" }
     *
     * WHY POST for a "verify" operation?
     * Because we're sending credentials — sensitive data.
     * GET requests put data in the URL (visible, logged).
     * POST sends data in the body (hidden, secure).
     * Even though we're not creating anything, POST is
     * the right choice when sending sensitive information.
     */
    public Response verifyLogin(String email, String password) {
        return given()
                .spec(ApiClient.getSpecWithFormData())
                // formParam = one field of form data
                // equivalent to typing in a form field
                .formParam("email", email)
                .formParam("password", password)
                .when()
                // POST to this endpoint path
                // base URL comes from ApiClient spec
                .post("/verifyLogin")
                .then()
                // extract() converts the response to
                // a Response object we can work with
                .extract()
                .response();
    }

    /**
     * createUser() — Registers a new user account.
     *
     * ENDPOINT: POST /api/createAccount
     * TYPE: Form Data
     * SUCCESS: { "responseCode": 201, "message": "User created!" }
     *
     * WHY so many parameters?
     * The API requires all these fields to create an account.
     * We pass them all so tests have full control over
     * what user data gets created.
     */
    public Response createUser(String name,
                               String email,
                               String password,
                               String title,
                               String birthDay,
                               String birthMonth,
                               String birthYear) {
        return given()
                .spec(ApiClient.getSpecWithFormData())
                .formParam("name", name)
                .formParam("email", email)
                .formParam("password", password)
                .formParam("title", title)
                .formParam("birth_date", birthDay)
                .formParam("birth_month", birthMonth)
                .formParam("birth_year", birthYear)
                // Required address fields
                .formParam("firstname", name)
                .formParam("lastname", "AutoPulse")
                .formParam("address1", "123 Test Street")
                .formParam("country", "India")
                .formParam("zipcode", "500001")
                .formParam("state", "Telangana")
                .formParam("city", "Hyderabad")
                .formParam("mobile_number", "9999999999")
                .when()
                .post("/createAccount")
                .then()
                .extract()
                .response();
    }

    /**
     * getUserByEmail() — Fetches user details by email.
     *
     * ENDPOINT: GET /api/getUserDetailByEmail
     * TYPE: Query Parameter (not body)
     * SUCCESS: Returns user object with all details
     *
     * WHAT IS A QUERY PARAMETER?
     * Unlike form data (in the body), query params
     * are appended to the URL directly:
     * /api/getUserDetailByEmail?email=test@gmail.com
     *
     * Used for GET requests where you're fetching
     * data based on a filter/identifier.
     */
    public Response getUserByEmail(String email) {
        return given()
                .spec(ApiClient.getSpec())
                // queryParam adds ?email=xxx to the URL
                .queryParam("email", email)
                .when()
                .get("/getUserDetailByEmail")
                .then()
                .extract()
                .response();
    }

    /**
     * deleteUser() — Deletes a user account.
     *
     * ENDPOINT: DELETE /api/deleteAccount
     * TYPE: Form Data
     * SUCCESS: { "responseCode": 200, "message": "Account deleted!" }
     *
     * WHY DELETE method?
     * REST convention — DELETE method = remove a resource.
     * The HTTP method tells the server WHAT you intend to do,
     * not just the URL. Same URL with different methods
     * can mean completely different operations.
     */
    public Response deleteUser(String email, String password) {
        return given()
                .spec(ApiClient.getSpecWithFormData())
                .formParam("email", email)
                .formParam("password", password)
                .when()
                .delete("/deleteAccount")
                .then()
                .extract()
                .response();
    }
}