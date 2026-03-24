package com.autopulse.utils;

import com.autopulse.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;

import java.time.Duration;

/**
 * DriverManager - Manages WebDriver lifecycle using ThreadLocal.
 *
 * ThreadLocal ensures each parallel test thread gets its OWN
 * browser instance. No sharing, no interference.
 *
 * This class has only STATIC methods — you never create a
 * DriverManager object. You just call DriverManager.getDriver()
 * from anywhere in the framework.
 */
public class DriverManager {

    /**
     * ThreadLocal<WebDriver> — the hotel key system.
     * Each thread gets its own WebDriver stored here.
     * Completely isolated from other threads.
     */
    private static ThreadLocal<WebDriver> driverThread =
            new ThreadLocal<>();

    // Private constructor — this class should never be instantiated
    private DriverManager() {}

    /**
     * initDriver() - Creates a new browser based on config.
     * Called at the START of each test (in BaseTest.setUp)
     */
    public static void initDriver() {
        String browser = ConfigReader.getInstance().getBrowser()
                .toLowerCase();

        WebDriver driver;

        switch (browser) {
            case "chrome":
                // WebDriverManager downloads the right
                // ChromeDriver version automatically.
                // No more manual driver downloads ever.
                WebDriverManager.chromedriver().setup();

                ChromeOptions chromeOptions = new ChromeOptions();

                // Add useful Chrome settings
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--disable-notifications");
                chromeOptions.addArguments(
                        "--disable-blink-features=AutomationControlled");

                driver = new ChromeDriver(chromeOptions);
                break;

            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;

            default:
                throw new RuntimeException(
                        "Browser '" + browser + "' not supported. " +
                                "Use: chrome, firefox, or edge"
                );
        }

        // Set timeouts from config
        int implicitWait = ConfigReader.getInstance()
                .getImplicitWait();
        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(30));

        // Store in ThreadLocal — this thread's browser
        driverThread.set(driver);

        System.out.println("✅ Browser started: " + browser);
    }

    /**
     * getDriver() - Get the current thread's WebDriver.
     * Called everywhere that needs to interact with the browser.
     *
     * Usage from anywhere: DriverManager.getDriver().findElement(...)
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThread.get();

        if (driver == null) {
            throw new RuntimeException(
                    "WebDriver not initialised. " +
                            "Make sure initDriver() was called in @BeforeMethod"
            );
        }
        return driver;
    }

    /**
     * quitDriver() - Closes browser and cleans up.
     * Called at the END of each test (in BaseTest.tearDown)
     *
     * IMPORTANT: remove() clears the ThreadLocal.
     * Without this, memory leaks happen in long test runs.
     */
    public static void quitDriver() {
        WebDriver driver = driverThread.get();

        if (driver != null) {
            driver.quit();
            driverThread.remove(); // Critical — prevent memory leak
            System.out.println("✅ Browser closed");
        }
    }
}