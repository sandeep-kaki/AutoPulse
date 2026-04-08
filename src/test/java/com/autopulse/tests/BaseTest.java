package com.autopulse.tests;

import com.autopulse.config.ConfigReader;
import com.autopulse.utils.DriverManager;
import com.autopulse.utils.ExtentReportManager;
import com.autopulse.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.lang.reflect.Method;

/**
 * BaseTest — Updated with reporting and screenshots.
 *
 * @BeforeSuite  → runs ONCE before all tests → init report
 * @BeforeMethod → runs before EACH test → open browser
 * @AfterMethod  → runs after EACH test → screenshot if fail,
 *                 log result, close browser
 * @AfterSuite   → runs ONCE after all tests → save report
 */
public class BaseTest {

    protected WebDriver driver;
    protected ConfigReader config;

    /**
     * @BeforeSuite — runs ONCE before the entire test run.
     * Perfect place to initialise the report.
     */
    @BeforeSuite
    public void initSuite() {
        ExtentReportManager.initReport();
        System.out.println("🚀 AutoPulse Test Suite Starting...");
    }

    /**
     * @BeforeMethod — runs before EACH test method.
     * Method parameter gives us the test name automatically.
     */
    @BeforeMethod
    public void setUp(Method method) {
        System.out.println("\n========== TEST STARTING ==========");
        System.out.println("Test: " + method.getName());

        // Create test entry in report
        // method.getName() = test method name
        // method.getAnnotation grabs the description from @Test
        String description = "";
        org.testng.annotations.Test testAnnotation =
                method.getAnnotation(org.testng.annotations.Test.class);
        if (testAnnotation != null) {
            description = testAnnotation.description();
        }

        ExtentReportManager.createTest(method.getName(), description);

        // Start browser
        DriverManager.initDriver();
        driver = DriverManager.getDriver();
        config = ConfigReader.getInstance();

        // Navigate to application
        driver.get(config.getBaseUrl());
        ExtentReportManager.logInfo(
                "Browser opened. Navigated to: " + config.getBaseUrl()
        );
    }

    /**
     * @AfterMethod — runs after EACH test method.
     * ITestResult tells us if the test passed or failed.
     */
    @AfterMethod
    public void tearDown(ITestResult result) {

        // ITestResult.SUCCESS = 1, FAILURE = 2, SKIP = 3
        if (result.getStatus() == ITestResult.FAILURE) {

            // Take screenshot
            String screenshotPath = ScreenshotUtil.capture(
                    driver,
                    result.getName()
            );

            // Attach screenshot to report
            if (screenshotPath != null) {
                ExtentReportManager.attachScreenshot(screenshotPath);
            }

            // Log failure with error message
            ExtentReportManager.logFail(
                    "Test FAILED: " + result.getThrowable().getMessage()
            );

        } else if (result.getStatus() == ITestResult.SUCCESS) {
            ExtentReportManager.logPass("Test PASSED");

        } else if (result.getStatus() == ITestResult.SKIP) {
            ExtentReportManager.logWarning("Test SKIPPED");
        }

        // Close browser
        DriverManager.quitDriver();
        System.out.println("========== TEST FINISHED: "
                + result.getName() + " — "
                + getStatusText(result.getStatus())
                + " ==========\n");
    }

    /**
     * @AfterSuite — runs ONCE after everything finishes.
     * Flushes (saves) the report to disk.
     * WITHOUT this — the HTML file is empty.
     */
    @AfterSuite
    public void tearDownSuite() {
        ExtentReportManager.flushReport();
        System.out.println("✅ AutoPulse Test Suite Complete.");
    }

    private String getStatusText(int status) {
        switch (status) {
            case ITestResult.SUCCESS: return "PASSED ✅";
            case ITestResult.FAILURE: return "FAILED ❌";
            case ITestResult.SKIP:    return "SKIPPED ⚠️";
            default: return "UNKNOWN";
        }
    }
}