package com.autopulse.tests.ui;

import com.autopulse.tests.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * SmokeTest - First test of AutoPulse.
 * Just verifies the site opens and has the right title.
 *
 * "Smoke test" = the most basic check.
 * Like turning on a machine and checking it doesn't
 * produce smoke before running full tests.
 */
public class SmokeTest extends BaseTest {

    @Test
    public void verifyHomepageTitleAndLaunch() {
        System.out.println("🚀 AutoPulse smoke test running...");

        // Get the page title
        String actualTitle = driver.getTitle();
        System.out.println("📄 Page title: " + actualTitle);

        // Verify the URL is correct
        String currentUrl = driver.getCurrentUrl();
        System.out.println("🌐 Current URL: " + currentUrl);

        // Assert title contains expected text
        Assert.assertTrue(
                actualTitle.contains("Automation"),
                "Homepage title should contain 'Automation' but was: "
                        + actualTitle
        );

        System.out.println("✅ AutoPulse smoke test PASSED!");
    }
}