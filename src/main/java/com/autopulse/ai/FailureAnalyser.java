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
 * FailureAnalyser — AI layer using Groq API (Llama 3.3 70B).
 *
 * WHY GROQ?
 * Completely free tier — 14,400 requests/day, no credit card.
 * Runs on custom LPU hardware — fastest inference available.
 * Speaks OpenAI-compatible format — the universal AI API standard.
 * Llama 3.3 70B supports function calling — needed for
 * the Self-Healing Agent we build next.
 *
 * WHY OPENAI FORMAT MATTERS:
 * OpenAI's message format (role + content) is the most widely
 * adopted AI API standard on the planet. Learn it once, use it
 * with Groq, OpenAI, Mistral, Together AI, and dozens more.
 * Gemini had its own dialect — OpenAI format is the lingua franca.
 *
 * REQUEST FORMAT:
 * {
 *   "model": "llama-3.3-70b-versatile",
 *   "messages": [
 *     { "role": "system", "content": "persona/constraints" },
 *     { "role": "user",   "content": "your prompt"         }
 *   ],
 *   "max_tokens": 300
 * }
 *
 * RESPONSE FORMAT:
 * {
 *   "choices": [{
 *     "message": {
 *       "role": "assistant",
 *       "content": "analysis here"
 *     }
 *   }]
 * }
 */
public class FailureAnalyser {

    // Groq endpoint — OpenAI-compatible chat completions
    // Same path as OpenAI: /v1/chat/completions
    // Only the base domain differs
    private static final String GROQ_API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

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
     * analyse() — Public interface. Takes failure details,
     * returns AI analysis as a string.
     *
     * NOTHING OUTSIDE THIS CLASS CHANGED.
     * AutoPulseListener still calls analyse() the same way.
     * ExtentReportManager still renders what this returns.
     * This is Separation of Concerns working as designed —
     * swap the AI engine, the rest of the framework is unaware.
     */
    public String analyse(String testName,
                          String errorMessage,
                          String stackTrace,
                          String pageUrl) {

        if (!config.isAiEnabled()) {
            return "AI analysis disabled in config.";
        }

        // Validate key early — clear error beats cryptic 401
        String apiKey = config.getAiApiKey();
        if (apiKey == null || apiKey.isBlank()
                || apiKey.equals("placeholder")) {
            return "AI analysis unavailable: "
                    + "GROQ_API_KEY is not configured. "
                    + "Set ai.api.key in config.properties "
                    + "or the GROQ_API_KEY environment variable.";
        }

        try {
            String requestBody = buildRequestBody(
                    testName, errorMessage, stackTrace, pageUrl
            );
            String responseBody = callGroqApi(requestBody);
            return extractAnalysis(responseBody);

        } catch (Exception e) {
            System.out.println("⚠️ AI analysis failed: "
                    + e.getMessage());
            return "AI analysis unavailable: "
                    + e.getMessage();
        }
    }

    /**
     * buildRequestBody() — Builds the OpenAI-format request.
     *
     * TWO MESSAGES — this is the key learning here:
     *
     * SYSTEM message: Sets the AI's persona before anything else.
     * "You are a test automation expert" makes every response
     * sharper and more specific than a raw prompt alone.
     * The model reads this as its job description.
     *
     * USER message: The actual failure context and question.
     * Kept under 100 words to minimise token usage.
     *
     * WHY SEPARATE SYSTEM AND USER?
     * System sets WHO the AI is. User sets WHAT you're asking.
     * This separation is what makes AI responses consistent
     * across different kinds of questions.
     */
    private String buildRequestBody(String testName,
                                    String errorMessage,
                                    String stackTrace,
                                    String pageUrl)
            throws Exception {

        ObjectNode root = objectMapper.createObjectNode();

        // Model name comes from config.properties
        // ai.model=llama-3.3-70b-versatile
        root.put("model", config.getAiModel());

        // max_tokens — OpenAI format uses this key
        // (Gemini used "maxOutputTokens" inside generationConfig)
        root.put("max_tokens", config.getAiMaxTokens());

        // Messages array — the core of OpenAI format
        ArrayNode messages = objectMapper.createArrayNode();

        // Message 1: SYSTEM — persona and constraints
        // This sharpens every response without adding user tokens
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "You are a test automation expert. "
                        + "Analyse Selenium test failures. "
                        + "Be concise. Respond in exactly 3 bullets."
        );
        messages.add(systemMessage);

        // Message 2: USER — the actual failure details
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", buildPrompt(
                testName, errorMessage, stackTrace
        ));
        messages.add(userMessage);

        root.set("messages", messages);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * buildPrompt() — Lean prompt, under 80 words.
     * Goes inside the user message content.
     * Kept short to minimise token usage per call.
     */
    private String buildPrompt(String testName,
                               String errorMessage,
                               String stackTrace) {
        return String.format(
                "Selenium test failed: %s\n"
                        + "Error: %s\n"
                        + "Stack: %s\n"
                        + "3 bullets: root cause, fix, prevention.",
                testName,
                errorMessage,
                truncateStackTrace(stackTrace, 5)
        );
    }

    /**
     * callGroqApi() — HTTP POST to Groq endpoint.
     *
     * KEY DIFFERENCE FROM GEMINI:
     * Gemini: API key in URL as ?key=YOUR_KEY
     * Groq:   API key in Authorization header as Bearer token
     *
     * "Authorization: Bearer <key>" is the universal standard
     * for REST API authentication. Used by OpenAI, Anthropic,
     * Groq, Stripe, GitHub, and virtually every modern API.
     * Gemini's ?key= approach was the odd one out.
     *
     * WHY BEARER IN HEADER IS BETTER THAN KEY IN URL:
     * URL parameters appear in server logs, browser history,
     * and proxy tool outputs. Headers do not.
     * Never put secrets in URLs — that's a security principle
     * worth remembering for your SDET career.
     */
    private String callGroqApi(String requestBody)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                // Bearer token — the universal auth standard
                .header("Authorization",
                        "Bearer " + config.getAiApiKey())
                .POST(HttpRequest.BodyPublishers
                        .ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Groq API error: "
                            + response.statusCode()
                            + " — " + response.body()
            );
        }

        return response.body();
    }

    /**
     * extractAnalysis() — Pulls text from Groq response.
     *
     * GROQ/OPENAI RESPONSE FORMAT:
     * {
     *   "choices": [{
     *     "message": {
     *       "role": "assistant",
     *       "content": "analysis here"
     *     }
     *   }]
     * }
     *
     * Path: choices[0] → message → content
     *
     * Compare to Gemini's path:
     * candidates[0] → content → parts[0] → text
     *
     * OpenAI format is one level shallower — cleaner to navigate.
     */
    private String extractAnalysis(String responseBody)
            throws Exception {

        JsonNode root =
                objectMapper.readTree(responseBody);

        // Navigate: choices → first choice → message → content
        JsonNode choices = root.get("choices");
        if (choices != null
                && choices.isArray()
                && choices.size() > 0) {

            JsonNode message =
                    choices.get(0).get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    return content.asText();
                }
            }
        }

        return "Could not extract analysis from response.";
    }

    /**
     * truncateStackTrace() — Limits stack trace to N lines.
     * Keeps the AI prompt short and focused.
     * Full stack traces have 30-50 lines — 5 is enough context.
     */
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