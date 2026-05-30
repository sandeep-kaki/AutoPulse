package com.autopulse.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ExtentReportManager - Manages the HTML test report lifecycle.
 *
 * ExtentReports = the report document
 * ExtentTest = one test entry inside that document
 * ExtentSparkReporter = the HTML renderer (makes it look beautiful)
 *
 * Uses ThreadLocal for ExtentTest — same reason as DriverManager.
 * Each parallel test thread writes to its OWN test entry.
 * No mixing of results between parallel tests.
 */
public class ExtentReportManager {

    private static ExtentReports extent;

    // Each thread gets its own test entry in the report
    private static ThreadLocal<ExtentTest> testThread =
            new ThreadLocal<>();

    // Report file path — timestamp makes each run unique
    private static String reportPath;

    /**
     * initReport() - Creates the report document.
     * Called ONCE before all tests start.
     * (We'll call this from BaseTest @BeforeSuite)
     */
    public static void initReport() {
        // Generate unique report name with timestamp
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        reportPath = "reports/AutoPulse_Report_" + timestamp + ".html";

        // SparkReporter = the HTML file writer
        ExtentSparkReporter sparkReporter =
                new ExtentSparkReporter(reportPath);

        // Configure the report appearance
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle("AutoPulse Test Report");
        sparkReporter.config().setReportName("AutoPulse — Automation Results");
        sparkReporter.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        // Create the main report object
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        // System info shown in report header
        extent.setSystemInfo("Project", "AutoPulse");
        extent.setSystemInfo("Tester", "Sandeep");
        extent.setSystemInfo("Environment", "QA");
        extent.setSystemInfo("Browser", "Chrome");
        extent.setSystemInfo("Application", "automationexercise.com");

        System.out.println("📊 Report will be saved at: " + reportPath);
    }

    /**
     * createTest() - Creates a new test entry in the report.
     * Called at the START of each test method.
     */
    public static void createTest(String testName, String description) {
        ExtentTest test = extent.createTest(testName, description);
        testThread.set(test);
    }

    /**
     * getTest() - Gets the current thread's test entry.
     * Used throughout the test to log steps and results.
     */
    public static ExtentTest getTest() {
        return testThread.get();
    }

    /**
     * Logging methods — add steps to the current test entry.
     * These show up as step-by-step entries in the HTML report.
     */
    public static void logPass(String message) {
        getTest().pass("✅ " + message);
    }

    public static void logFail(String message) {
        getTest().fail("❌ " + message);
    }

    public static void logInfo(String message) {
        getTest().info("ℹ️ " + message);
    }

    public static void logWarning(String message) {
        getTest().warning("⚠️ " + message);
    }

    /**
     * attachScreenshot() - Adds a screenshot image to
     * the current test entry in the report.
     */
    public static void attachScreenshot(String screenshotPath) {
        try {
            getTest().addScreenCaptureFromPath(
                    screenshotPath,
                    "Screenshot at failure"
            );
        } catch (Exception e) {
            logWarning("Could not attach screenshot: "
                    + e.getMessage());
        }
    }

    /**
     * attachAiAnalysis() - Adds AI failure analysis to report.
     * This is the showstopper feature we'll wire up later.
     */
    public static void attachAiAnalysis(String analysis) {
        getTest().info("<details><summary>🤖 AI Failure Analysis"
                + " (click to expand)</summary><br>"
                + analysis + "</details>");
    }

    /**
     * flushReport() - Saves and closes the report file.
     * MUST be called after all tests finish.
     * Without this — the HTML file is incomplete/empty.
     */
    public static void flushReport() {
        if (extent != null) {
            extent.flush();
            System.out.println("📊 Report saved: " + reportPath);
        }
    }
}