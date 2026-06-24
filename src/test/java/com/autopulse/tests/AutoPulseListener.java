package com.autopulse.tests;

import com.autopulse.ai.FailureAnalyser;
import com.autopulse.ai.SelfHealingAgent;
import com.autopulse.utils.ExtentReportManager;
import com.autopulse.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * AutoPulseListener — TestNG Listener that hooks into
 * every test result across the entire suite.
 *
 * IMPLEMENTS ITestListener — TestNG's interface for
 * listening to test lifecycle events.
 *
 * HOW IT WORKS:
 * TestNG calls these methods automatically.
 * We don't call them anywhere in our code.
 * We just implement the interface and register
 * the listener — TestNG does the rest.
 *
 * REGISTERED IN: testng.xml (we'll add it there)
 *
 * WHY NOT PUT THIS IN BaseTest?
 * BaseTest is a parent class — it runs INSIDE tests.
 * A Listener runs OUTSIDE tests — it observes them.
 * Cleaner separation. Listener can watch ALL test
 * classes, not just those extending BaseTest.
 */
public class AutoPulseListener implements ITestListener {

    private final FailureAnalyser failureAnalyser =
            new FailureAnalyser();

    /**
     * onTestFailure() — Called automatically by TestNG
     * the moment ANY test fails anywhere in the suite.
     *
     * ITestResult contains EVERYTHING about the failure:
     * - The test method name
     * - The exception that was thrown
     * - The test class instance
     * - Start/end timestamps
     */
    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("🔴 Listener caught failure: "
                + result.getName());

        String testName = result.getName();
        Throwable throwable = result.getThrowable();
        String errorMessage = throwable != null
                ? throwable.getMessage()
                : "No error message";
        String stackTrace = throwable != null
                ? getStackTraceAsString(throwable)
                : "No stack trace";

        WebDriver driver = getDriver(result);
        String pageUrl = getCurrentUrl(result);

        String screenshotPath = null;
        if (driver != null) {
            screenshotPath = ScreenshotUtil.capture(driver, testName);
        }

        // ── Existing quick AI analysis — runs for EVERY failure ──
        System.out.println("🤖 Requesting AI analysis...");
        String aiAnalysis = failureAnalyser.analyse(
                testName, errorMessage, stackTrace, pageUrl
        );

        com.aventstack.extentreports.ExtentTest test =
                ExtentReportManager.getTest();

        if (test != null) {
            test.fail("❌ Test FAILED: " + errorMessage);

            if (screenshotPath != null) {
                try {
                    test.addScreenCaptureFromPath(
                            screenshotPath, "Failure Screenshot"
                    );
                } catch (Exception e) {
                    System.out.println("Screenshot attach failed: "
                            + e.getMessage());
                }
            }

            test.info("<details><summary>🤖 AI Analysis"
                    + " — click to expand</summary><br>"
                    + aiAnalysis + "</details>");
        }

        // ── NEW — Self-Healing Agent, ONLY for locator failures ──
        if (isLocatorFailure(throwable) && driver != null) {
            System.out.println(
                    "🩺 Locator failure detected — waking up "
                            + "Self-Healing Agent..."
            );

            SelfHealingAgent agent = new SelfHealingAgent(driver);
            String verdict = agent.investigate(
                    testName, errorMessage, stackTrace
            );

            System.out.println("🩺 Agent verdict: " + verdict);

            ExtentReportManager.attachHealingVerdict(verdict);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // Log pass in report
        ExtentReportManager.logPass(
                "Test PASSED ✅"
        );
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentReportManager.logWarning(
                "Test SKIPPED ⚠️"
        );
    }

    // These are required by ITestListener interface
    // We don't need them — leave empty
    @Override
    public void onTestStart(ITestResult result) {}

    @Override
    public void onStart(ITestContext context) {}

    @Override
    public void onFinish(ITestContext context) {}

    // ── Helper Methods ────────────────────────────────

    /**
     * getDriver() — Gets WebDriver from the test class.
     *
     * result.getInstance() returns the test class object
     * as Object. We try to cast it and access driver field.
     *
     * This works because BaseTest has protected WebDriver driver
     * — accessible from this package.
     */
    private WebDriver getDriver(ITestResult result) {
        try {
            Object testInstance = result.getInstance();
            if (testInstance instanceof
                    com.autopulse.tests.BaseTest) {
                return ((com.autopulse.tests.BaseTest)
                        testInstance).driver;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get driver: "
                    + e.getMessage());
        }
        return null;
    }

    /**
     * getCurrentUrl() — Gets URL from browser at failure.
     */
    private String getCurrentUrl(ITestResult result) {
        try {
            WebDriver driver = getDriver(result);
            if (driver != null) {
                return driver.getCurrentUrl();
            }
        } catch (Exception e) {
            // Silent fail — URL is optional context
        }
        return "URL unavailable";
    }

    /**
     * getStackTraceAsString() — Converts exception stack
     * trace to a readable string.
     *
     * Throwable.getStackTrace() returns StackTraceElement[]
     * We join them into one string for the AI prompt.
     */
    private String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element :
                throwable.getStackTrace()) {
            sb.append("\tat ")
                    .append(element.toString())
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * isLocatorFailure() — Decides if this failure is worth
     * waking up the Self-Healing Agent for.
     *
     * WHY CHECK EXCEPTION CLASS NAME?
     * NoSuchElementException, StaleElementReferenceException, and
     * TimeoutException (when waiting for an element) are Selenium's
     * specific signals that "something about finding/using an
     * element went wrong" — as opposed to AssertionError (business
     * logic) or network exceptions (environment problems).
     *
     * The agent is expensive (multiple API calls). This filter
     * keeps it focused on the ONE problem class it's actually
     * designed to solve.
     */
    private boolean isLocatorFailure(Throwable t) {
        if (t == null) return false;

        String exceptionName = t.getClass().getSimpleName();
        String message = t.getMessage();

        return exceptionName.contains("NoSuchElement")
                || exceptionName.contains("StaleElement")
                || (exceptionName.contains("Timeout")
                && message != null
                && (message.contains("visibility")
                || message.contains("element")
                || message.contains("clickable")));
    }
}