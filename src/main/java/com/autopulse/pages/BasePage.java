package com.autopulse.pages;

import com.autopulse.utils.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * BasePage - Parent of ALL Page Object classes.
 *
 * Contains common interactions that every page needs.
 * Child pages extend this and inherit all these methods.
 *
 * WHY?
 * Instead of writing wait logic in LoginPage, HomePage,
 * ProductPage separately — write it once here.
 * Every page gets it for free by extending BasePage.
 */
public class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;

    // Every page object receives the driver when created
    public BasePage(WebDriver driver) {
        this.driver = driver;
        // 15 second explicit wait — used for all interactions
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /**
     * click() - Waits for element to be clickable, then clicks.
     *
     * WHY NOT just driver.findElement().click()?
     * Because pages load dynamically. The element might exist
     * in the HTML but not be ready for interaction yet.
     * waitUntilClickable ensures the element is truly ready.
     */
    protected void click(By locator) {
        WebElement element = wait.until(
                ExpectedConditions.elementToBeClickable(locator)
        );
        element.click();
    }

    /**
     * type() - Clears any existing text, then types new text.
     *
     * WHY clear first?
     * If a field already has text (from a previous test or
     * autofill), just typing appends to it. Clearing first
     * ensures clean input every time.
     */
    protected void type(By locator, String text) {
        WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(locator)
        );
        element.clear();
        element.sendKeys(text);
    }

    /**
     * getText() - Waits for element to be visible, returns text.
     */
    protected String getText(By locator) {
        WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(locator)
        );
        return element.getText().trim();
    }

    /**
     * isDisplayed() - Checks if element is visible on page.
     * Returns true/false — doesn't throw exception if not found.
     *
     * WHY try-catch?
     * If element doesn't exist, Selenium throws an exception.
     * For a simple "is this visible?" check, exception is too
     * aggressive. We catch it and return false cleanly.
     */
    protected boolean isDisplayed(By locator) {
        try {
            return wait.until(
                    ExpectedConditions.visibilityOfElementLocated(locator)
            ).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * scrollIntoView() - Scrolls the page to bring element
     * into the visible area of the screen.
     *
     * Some elements are below the fold (not visible without
     * scrolling). Selenium can't interact with them until
     * they're in the viewport. This fixes that.
     */
    protected void scrollIntoView(By locator) {
        WebElement element = driver.findElement(locator);
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView(true);",
                        element);
    }

    /**
     * getPageTitle() - Returns current page title.
     * Useful for assertions in tests.
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * getCurrentUrl() - Returns current URL.
     * Useful for verifying navigation happened correctly.
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * waitForPageLoad() - Waits until page is fully loaded.
     *
     * JavaScript's document.readyState becomes "complete"
     * when everything on the page has finished loading.
     * We wait for that signal before proceeding.
     */
    protected void waitForPageLoad() {
        wait.until(webDriver ->
                ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState")
                        .equals("complete")
        );
    }

    /**
     * closeAdPopupIfPresent() - Closes the ad overlay
     * on automationexercise.com if it appears.
     *
     * WHY two approaches?
     * The popup has a close button we try first.
     * If that fails — JavaScript forcefully removes
     * the overlay from the page DOM entirely.
     * Belt AND suspenders approach.
     */
    protected void closeAdPopupIfPresent() {
        try {
            // The ad popup close button on this site
            By adCloseButton = By.xpath(
                    "//div[@id='ad_position_box']//button" +
                            " | //ins[@class='adsbygoogle']//button" +
                            " | //div[contains(@id,'ad')]//button[@class='close']"
            );

            // Short wait — don't waste time if no popup
            org.openqa.selenium.support.ui.WebDriverWait shortWait =
                    new org.openqa.selenium.support.ui.WebDriverWait(
                            driver, java.time.Duration.ofSeconds(3)
                    );

            try {
                org.openqa.selenium.WebElement closeBtn = shortWait.until(
                        org.openqa.selenium.support.ui.ExpectedConditions
                                .elementToBeClickable(adCloseButton)
                );
                closeBtn.click();
                System.out.println("🚫 Ad popup closed");

            } catch (Exception e) {
                // No popup found — that's fine, continue
            }

            // Nuclear option — remove ALL ad elements via JavaScript
            // This runs regardless, cleans any invisible overlays
            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript(
                            "var ads = document.querySelectorAll(" +
                                    "'#ad_position_box, .adsbygoogle, " +
                                    "[id*=\"google_ads\"], [id*=\"aswift\"]');" +
                                    "ads.forEach(function(ad) { ad.remove(); });"
                    );

        } catch (Exception e) {
            // Silent fail — never let popup handling break the test
            System.out.println("⚠️ Ad handling skipped: "
                    + e.getMessage());
        }
    }
}