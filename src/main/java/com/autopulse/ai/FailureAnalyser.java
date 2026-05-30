package com.autopulse.ai;

import com.autopulse.config.ConfigReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * FailureAnalyser — AI layer using Google Gemini API.
 *
 * WHY GEMINI?
 * Completely free tier — no credit card, no subscription.
 * 1500 requests/day free. More than enough for test runs.
 *
 * Gemini API endpoint structure is different from Claude:
 * URL contains the model name and API key as query param.
 * Request body uses "contents" instead of "messages".
 */
public class FailureAnalyser {

    // Gemini API endpoint
    // Model and key go in the URL itself
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com" +
                    "/v1beta/models/%s:generateContent?key=%s";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConfigReader config;

    public FailureAnalyser() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.config = ConfigReader.getInstance();
    }

    /**
     * analyse() — Takes failure details, returns AI analysis.
     * Same interface as before — nothing else in AutoPulse
     * needs to change. Only this class knows it's Gemini.
     */
    public String analyse(String testName,
                          String errorMessage,
                          String stackTrace,
                          String pageUrl) {

        if (!config.isAiEnabled()) {
            return "AI analysis disabled in config.";
        }

        try {
            String prompt = buildPrompt(
                    testName, errorMessage, stackTrace, pageUrl
            );
            String requestBody = buildRequestBody(prompt);
            String responseBody = callGeminiApi(requestBody);
            return extractAnalysis(responseBody);

        } catch (Exception e) {
            System.out.println("⚠️ AI analysis failed: "
                    + e.getMessage());
            return "AI analysis unavailable: "
                    + e.getMessage();
        }
    }

    /**
     * buildPrompt() — Lean prompt, under 100 words.
     * Minimizes token usage per call.
     */
    private String buildPrompt(String testName,
                               String errorMessage,
                               String stackTrace,
                               String pageUrl) {
        return String.format(
                "Selenium test failed: %s\n" +
                        "Error: %s\n" +
                        "Stack: %s\n" +
                        "In 3 bullets: root cause, fix, prevention.",
                testName,
                errorMessage,
                truncateStackTrace(stackTrace, 5)
        );
    }

    /**
     * buildRequestBody() — Gemini uses "contents" structure.
     *
     * Gemini request format:
     * {
     *   "contents": [{"parts": [{"text": "your prompt"}]}],
     *   "generationConfig": {"maxOutputTokens": 100}
     * }
     *
     * Different from Claude's "messages" format —
     * but same concept: send text, get text back.
     */
    private String buildRequestBody(String prompt)
            throws Exception {

        ObjectNode root = objectMapper.createObjectNode();

        // Contents array → parts array → text
        ArrayNode contents = objectMapper.createArrayNode();
        ObjectNode content = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();

        part.put("text", prompt);
        parts.add(part);
        content.set("parts", parts);
        contents.add(content);
        root.set("contents", contents);

        // Limit response tokens — minimize usage
        ObjectNode genConfig =
                objectMapper.createObjectNode();
        genConfig.put("maxOutputTokens",
                config.getAiMaxTokens());
        root.set("generationConfig", genConfig);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * callGeminiApi() — HTTP POST to Gemini endpoint.
     *
     * Key difference from Claude:
     * API key goes in the URL as query parameter.
     * No "x-api-key" header needed.
     */
    private String callGeminiApi(String requestBody)
            throws Exception {

        String url = String.format(
                GEMINI_API_URL,
                config.getAiModel(),
                config.getAiApiKey()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers
                        .ofString(requestBody))
                .build();

        System.out.println(
                "🤖 Calling Gemini AI for failure analysis..."
        );

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println(
                "🤖 Gemini API response status: "
                        + response.statusCode()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Gemini API error: "
                            + response.statusCode()
                            + " — " + response.body()
            );
        }

        return response.body();
    }

    /**
     * extractAnalysis() — Pulls text from Gemini response.
     *
     * Gemini response format:
     * {
     *   "candidates": [{
     *     "content": {
     *       "parts": [{"text": "analysis here"}]
     *     }
     *   }]
     * }
     *
     * Navigate: candidates[0] → content → parts[0] → text
     */
    private String extractAnalysis(String responseBody)
            throws Exception {

        JsonNode root =
                objectMapper.readTree(responseBody);

        JsonNode candidates = root.get("candidates");
        if (candidates != null
                && candidates.isArray()
                && candidates.size() > 0) {

            JsonNode content =
                    candidates.get(0).get("content");
            if (content != null) {
                JsonNode parts = content.get("parts");
                if (parts != null
                        && parts.isArray()
                        && parts.size() > 0) {
                    JsonNode text =
                            parts.get(0).get("text");
                    if (text != null) {
                        return text.asText();
                    }
                }
            }
        }

        return "Could not extract analysis from response.";
    }

    private String truncateStackTrace(String stackTrace,
                                      int maxLines) {
        if (stackTrace == null)
            return "No stack trace available";

        String[] lines = stackTrace.split("\n");
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(maxLines, lines.length);

        for (int i = 0; i < limit; i++) {
            sb.append(lines[i]).append("\n");
        }

        if (lines.length > maxLines) {
            sb.append("... (")
                    .append(lines.length - maxLines)
                    .append(" more lines)");
        }

        return sb.toString();
    }
}