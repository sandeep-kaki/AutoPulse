package com.autopulse.api;

import com.autopulse.config.ConfigReader;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * ApiClient — The foundation of AutoPulse's API layer.
 *
 * RESPONSIBILITY:
 * Builds and provides a pre-configured RequestSpecification.
 * Every API call in the framework starts from this spec.
 *
 * WHAT IS RequestSpecification?
 * Think of it as a template for API requests.
 * Instead of configuring base URL, headers, content type
 * in every single test — you configure once here.
 * Every test inherits this configuration automatically.
 *
 * ANALOGY:
 * Like a pre-stamped envelope. The return address,
 * the stamp, the standard format — all pre-filled.
 * You just write the specific destination and content.
 */
public class ApiClient {

    // The shared request specification
    // static — belongs to the class, not any instance
    // All API tests share this one specification
    private static RequestSpecification requestSpec;

    // Private constructor — this class should never
    // be instantiated. All methods are static.
    // You call ApiClient.getSpec() — not new ApiClient()
    private ApiClient() {}

    /**
     * getSpec() — Returns the pre-configured request spec.
     *
     * Uses lazy initialization — spec is built only when
     * first requested, not when class loads.
     *
     * WHY LAZY INITIALIZATION?
     * ConfigReader needs to be ready before ApiClient builds
     * the spec. Lazy init ensures ConfigReader always
     * initializes first. No timing issues.
     */
    public static RequestSpecification getSpec() {
        if (requestSpec == null) {
            requestSpec = buildSpec();
        }
        return requestSpec;
    }

    /**
     * buildSpec() — Constructs the RequestSpecification.
     *
     * RequestSpecBuilder is REST Assured's builder pattern.
     * We chain configuration calls and build() at the end.
     *
     * WHAT EACH CONFIGURATION DOES:
     */
    private static RequestSpecification buildSpec() {
        return new RequestSpecBuilder()

                // Base URI — all requests start from here
                // "/api/productsList" becomes
                // "https://automationexercise.com/api/productsList"
                .setBaseUri(ConfigReader.getInstance().getApiBaseUrl())

                // Content-Type header — tells server we're
                // sending JSON data. Server knows how to parse it.
                .setContentType(ContentType.JSON)

                // Accept header — tells server we want JSON back.
                // Server knows what format to respond in.
                .setAccept(ContentType.JSON)

                // Logging — prints request details to console
                // when a test FAILS. Helps debug what was sent.
                // LOG ALL = log URL, headers, body everything.
                .log(LogDetail.ALL)

                // build() creates the final immutable spec
                .build();
    }

    /**
     * getSpecWithFormData() — Spec for form-encoded requests.
     *
     * Not all APIs accept JSON. Some (like automationexercise.com)
     * expect HTML form data format — the same format browsers
     * use when submitting forms.
     *
     * Content-Type: application/x-www-form-urlencoded
     * Body looks like: name=Sandeep&email=s@gmail.com
     *
     * WHY DOES THIS EXIST?
     * automationexercise.com's API uses form data for POST
     * requests like createAccount and deleteAccount.
     * This spec handles those specific endpoints.
     */
    public static RequestSpecification getSpecWithFormData() {
        return new RequestSpecBuilder()
                .setBaseUri(ConfigReader.getInstance().getApiBaseUrl())
                .setContentType(ContentType.URLENC) // form data
                .log(LogDetail.ALL)
                .build();
    }
}