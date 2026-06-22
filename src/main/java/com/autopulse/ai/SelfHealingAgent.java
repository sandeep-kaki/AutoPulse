package com.autopulse.ai;

import com.autopulse.config.ConfigReader;
import com.autopulse.pages.BasePage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SelfHealingAgent — Investigates locator failures using
 * Groq function calling.
 *
 * THIS IS NOT A SINGLE AI CALL. This is a loop where the
 * model decides what to investigate next, requests a tool,
 * WE run the real Java code, feed the result back, and the
 * model keeps reasoning until it reaches a verdict.
 *
 * The model never touches your browser directly. It only
 * ever says "I'd like to run X" — your code does the rest.
 */
public class SelfHealingAgent {

    private static final String GROQ_API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    // Every agent needs a leash — prevents infinite tool-call loops
    private static final int MAX_STEPS = 6;

    // Page source can be huge — truncate to control token usage
    private static final int PAGE_SOURCE_LIMIT = 3000;

    private final WebDriver driver;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConfigReader config;

    public SelfHealingAgent(WebDriver driver) {
        this.driver = driver;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.config = ConfigReader.getInstance();
    }

    /**
     * investigate() — Public entry point. Runs the full agent
     * loop and returns a final verdict string.
     *
     * Returns either:
     *   "HEALED: <new locator> | <reasoning>"
     *   "REAL_BUG: <reasoning>"
     *   "Agent exceeded step limit — escalating to human review."
     */
    public String investigate(String testName,
                              String errorMessage,
                              String stackTrace) {

        // The growing conversation — this IS the agent's memory
        List<ObjectNode> messages = new ArrayList<>();
        messages.add(systemMessage());
        messages.add(userMessage(testName, errorMessage, stackTrace));

        for (int step = 0; step < MAX_STEPS; step++) {

            try {
                String requestBody = buildRequestBody(messages);
                String responseBody = callGroqApi(requestBody);
                JsonNode root = objectMapper.readTree(responseBody);

                JsonNode choice = root.get("choices").get(0);
                JsonNode message = choice.get("message");
                String finishReason =
                        choice.get("finish_reason").asText();

                // Record the model's own message in history —
                // it needs to "remember" what it just asked
                messages.add((ObjectNode) message);

                if ("tool_calls".equals(finishReason)) {

                    JsonNode toolCalls = message.get("tool_calls");

                    for (JsonNode toolCall : toolCalls) {
                        String toolCallId =
                                toolCall.get("id").asText();
                        String toolName = toolCall.get("function")
                                .get("name").asText();
                        String argsJson = toolCall.get("function")
                                .get("arguments").asText();

                        System.out.println(
                                "🔧 Agent requested tool: " + toolName
                        );

                        // WE run the actual code — model never does
                        String result = dispatch(toolName, argsJson);

                        ObjectNode toolResult =
                                objectMapper.createObjectNode();
                        toolResult.put("role", "tool");
                        toolResult.put("tool_call_id", toolCallId);
                        toolResult.put("content", result);
                        messages.add(toolResult);
                    }
                    // Loop continues — model reasons with new info

                } else {
                    // finish_reason == "stop" — final verdict reached
                    System.out.println(
                            "✅ Agent reached verdict after "
                                    + (step + 1) + " step(s)"
                    );
                    return message.get("content").asText();
                }

            } catch (Exception e) {
                return "Agent investigation failed: "
                        + e.getMessage();
            }
        }

        return "Agent exceeded step limit (" + MAX_STEPS
                + ") — escalating to human review.";
    }

    // ─────────────────────────────────────────────────
    // MESSAGE BUILDERS
    // ─────────────────────────────────────────────────

    /**
     * systemMessage() — Sets the agent's persona, goal,
     * and required answer format. This is the "job description"
     * the model reads before doing anything else.
     */
    private ObjectNode systemMessage() {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("role", "system");
        msg.put("content",
                "You are a Self-Healing Test Automation Agent. "
                        + "A Selenium test failed with a locator-related error. "
                        + "Investigate using the tools provided: check the broken "
                        + "locator, examine the live page source, and validate "
                        + "candidate replacement locators before concluding. "
                        + "Decide if this is a STALE LOCATOR (page changed, "
                        + "element moved or renamed) or a REAL BUG (element "
                        + "is genuinely absent from the page). "
                        + "When confident, respond with EXACTLY one of these "
                        + "formats and nothing else: "
                        + "'HEALED: <new locator> | <one line reasoning>' or "
                        + "'REAL_BUG: <one line reasoning>'. "
                        + "Always use the tools to verify — never guess blindly."
        );
        return msg;
    }

    private ObjectNode userMessage(String testName,
                                   String errorMessage,
                                   String stackTrace) {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", String.format(
                "Test: %s\nError: %s\nStack: %s\n\n"
                        + "Investigate this locator failure.",
                testName, errorMessage, truncate(stackTrace, 5)
        ));
        return msg;
    }

    // ─────────────────────────────────────────────────
    // REQUEST BUILDING — messages + tools + tool_choice
    // ─────────────────────────────────────────────────

    private String buildRequestBody(List<ObjectNode> messages)
            throws Exception {

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", config.getAiModel());
        root.put("max_tokens", 300);

        ArrayNode messagesArray = objectMapper.createArrayNode();
        for (ObjectNode m : messages) {
            messagesArray.add(m);
        }
        root.set("messages", messagesArray);

        // "auto" lets the model decide — call a tool, or answer
        root.set("tools", buildToolsDefinition());
        root.put("tool_choice", "auto");

        return objectMapper.writeValueAsString(root);
    }

    /**
     * buildToolsDefinition() — Declares the three tools the
     * agent is allowed to request. This is literally a menu —
     * the model can only ask for what's listed here.
     */
    private ArrayNode buildToolsDefinition() {
        ArrayNode tools = objectMapper.createArrayNode();

        tools.add(buildTool(
                "getBrokenLocator",
                "Returns the exact locator string that failed during "
                        + "the test, as originally written in the Page Object.",
                null
        ));

        tools.add(buildTool(
                "getPageSource",
                "Returns the current live HTML page source from the "
                        + "browser right now. Use this to see what elements "
                        + "actually exist.",
                null
        ));

        ObjectNode validateParams = objectMapper.createObjectNode();
        validateParams.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode locatorProp = objectMapper.createObjectNode();
        locatorProp.put("type", "string");
        locatorProp.put("description",
                "An XPath (e.g. //button[@id='login']) or CSS "
                        + "selector to test against the live page.");
        props.set("locator", locatorProp);
        validateParams.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("locator");
        validateParams.set("required", required);

        tools.add(buildTool(
                "validateLocator",
                "Tests whether a candidate locator finds an element "
                        + "on the current live page. Returns FOUND with "
                        + "element details, or NOT_FOUND.",
                validateParams
        ));

        return tools;
    }

    private ObjectNode buildTool(String name, String description,
                                 ObjectNode params) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");

        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", name);
        function.put("description", description);

        if (params == null) {
            ObjectNode emptyParams =
                    objectMapper.createObjectNode();
            emptyParams.put("type", "object");
            emptyParams.set("properties",
                    objectMapper.createObjectNode());
            function.set("parameters", emptyParams);
        } else {
            function.set("parameters", params);
        }

        tool.set("function", function);
        return tool;
    }

    // ─────────────────────────────────────────────────
    // TOOL DISPATCH — where requests become real actions
    // ─────────────────────────────────────────────────

    /**
     * dispatch() — The bridge between "AI wants X" and
     * "Java actually does X". This is THE most important
     * method to understand in this entire class.
     *
     * The model NEVER executes Java code. It only ever
     * sends a tool name + arguments as text. This method
     * reads that text and runs the real corresponding method.
     */
    private String dispatch(String toolName, String argsJson) {
        try {
            switch (toolName) {
                case "getBrokenLocator":
                    return getBrokenLocator();

                case "getPageSource":
                    return getPageSourceTruncated();

                case "validateLocator":
                    JsonNode args = objectMapper.readTree(argsJson);
                    String locator = args.get("locator").asText();
                    return validateLocator(locator);

                default:
                    return "ERROR: Unknown tool " + toolName;
            }
        } catch (Exception e) {
            return "ERROR: Tool execution failed - "
                    + e.getMessage();
        }
    }

    private String getBrokenLocator() {
        By locator = BasePage.getLastAttemptedLocator();
        if (locator == null) {
            return "No locator was recorded for this failure.";
        }
        return locator.toString();
    }

    private String getPageSourceTruncated() {
        String source = driver.getPageSource();
        if (source.length() > PAGE_SOURCE_LIMIT) {
            return source.substring(0, PAGE_SOURCE_LIMIT)
                    + "... (truncated, " + source.length()
                    + " total characters)";
        }
        return source;
    }

    /**
     * validateLocator() — The agent's "test my hypothesis" tool.
     * Actually queries the live DOM through Selenium — this is
     * real verification, not the model guessing confidently.
     */
    private String validateLocator(String locatorStr) {
        try {
            By by = parseLocator(locatorStr);
            List<WebElement> elements = driver.findElements(by);

            if (elements.isEmpty()) {
                return "NOT_FOUND: No elements matched '"
                        + locatorStr + "'";
            }

            WebElement first = elements.get(0);
            String tag = first.getTagName();
            String text = first.getText();
            if (text.length() > 50) {
                text = text.substring(0, 50) + "...";
            }

            return "FOUND: " + elements.size()
                    + " element(s) | tag=" + tag
                    + " | text=" + text;

        } catch (Exception e) {
            return "ERROR: Invalid locator syntax - "
                    + e.getMessage();
        }
    }

    /**
     * parseLocator() — Converts a plain string the AI sent
     * into a real Selenium By object.
     *
     * Simple heuristic: starts with "/" or "(" → XPath.
     * Otherwise → CSS selector. Covers the vast majority
     * of real-world locator styles.
     */
    private By parseLocator(String locatorStr) {
        String trimmed = locatorStr.trim();
        if (trimmed.startsWith("/") || trimmed.startsWith("(")) {
            return By.xpath(trimmed);
        }
        return By.cssSelector(trimmed);
    }

    // ─────────────────────────────────────────────────
    // HTTP + HELPERS
    // ─────────────────────────────────────────────────

    private String callGroqApi(String requestBody)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization",
                        "Bearer " + config.getAiApiKey())
                .POST(HttpRequest.BodyPublishers
                        .ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Groq API error: " + response.statusCode()
                            + " — " + response.body()
            );
        }

        return response.body();
    }

    private String truncate(String text, int maxLines) {
        if (text == null) return "No stack trace available";
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(maxLines, lines.length);
        for (int i = 0; i < limit; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }
}