package com.autopulse.tests;

import com.autopulse.config.ConfigReader;
import com.autopulse.utils.DriverManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * BaseTest - Parent class for ALL test classes.
 *
 * Every test class extends this.
 * This means every test automatically gets:
 * - Browser setup before it runs
 * - Browser cleanup after it runs
 * - Access to driver and config
 *
 * Tests focus ONLY on what to test.
 * BaseTest handles HOW to set up and tear down.
 */
public class BaseTest {

    // protected — child test classes can access this directly
    protected WebDriver driver;
    protected ConfigReader config;

    /**
     * @BeforeMethod — TestNG runs this BEFORE every single test.
     * Every test gets a fresh browser. Clean state.
     */
    @BeforeMethod
    public void setUp() {
        System.out.println("\n========== TEST STARTING ==========");

        // Start browser based on config
        DriverManager.initDriver();

        // Store reference for use in test classes
        driver = DriverManager.getDriver();
        config = ConfigReader.getInstance();

        // Navigate to the application
        driver.get(config.getBaseUrl());

        System.out.println("✅ Navigated to: " + config.getBaseUrl());
    }

    /**
     * @AfterMethod — TestNG runs this AFTER every single test.
     * Cleans up no matter if test passed or failed.
     */
    @AfterMethod
    public void tearDown() {
        DriverManager.quitDriver();
        System.out.println("========== TEST FINISHED ==========\n");
    }
}