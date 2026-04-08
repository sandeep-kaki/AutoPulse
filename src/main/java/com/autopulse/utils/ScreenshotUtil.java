package com.autopulse.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ScreenshotUtil - Takes and saves screenshots.
 *
 * Called automatically when any test fails.
 * Screenshot is saved to disk and path returned
 * so ExtentReportManager can attach it to the report.
 */
public class ScreenshotUtil {

    private static final String SCREENSHOT_DIR =
            "reports/screenshots/";

    /**
     * capture() - Takes a screenshot and saves it.
     *
     * @param driver    - current WebDriver instance
     * @param testName  - used in the filename
     * @return          - file path of saved screenshot
     *
     * HOW SELENIUM TAKES SCREENSHOTS:
     * WebDriver implements TakesScreenshot interface.
     * We cast driver to TakesScreenshot to access
     * getScreenshotAs() method which returns the image
     * as a File, byte array, or Base64 string.
     * We use File — simplest for saving to disk.
     */
    public static String capture(WebDriver driver, String testName) {

        // Create screenshots directory if it doesn't exist
        createDirectoryIfNeeded(SCREENSHOT_DIR);

        // Generate unique filename with timestamp
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = testName + "_" + timestamp + ".png";
        String filePath = SCREENSHOT_DIR + fileName;

        try {
            // Cast driver to TakesScreenshot
            TakesScreenshot ts = (TakesScreenshot) driver;

            // Get screenshot as a temporary File
            File screenshotFile = ts.getScreenshotAs(OutputType.FILE);

            // Copy from temp location to our screenshots folder
            Files.copy(
                    screenshotFile.toPath(),
                    Paths.get(filePath)
            );

            System.out.println("📸 Screenshot saved: " + filePath);
            return filePath;

        } catch (IOException e) {
            System.out.println("❌ Screenshot failed: "
                    + e.getMessage());
            return null;
        }
    }

    private static void createDirectoryIfNeeded(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
            // mkdirs() creates parent directories too
            // mkdir() only creates the final directory
            // Always use mkdirs() for nested paths
        }
    }
}