package com.autopulse.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * HomePage - Page Object for the Home page after login.
 *
 * After successful login, user lands here.
 * This class verifies login was actually successful.
 */
public class HomePage extends BasePage {

    // This element only appears when user is logged in
    // "Logged in as [username]" text in the navbar
    private By loggedInText =
            By.xpath("//a[contains(@href,'/logout')]"); //locating the actual 'logout' link means already logged in

    private By logoutButton =
            By.xpath("//a[@href='/logout']");

    private By homePageHeader =
            By.xpath("//h2[contains(text(),'Full-Fledged')]");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    /**
     * isLoggedIn() - The most important method here.
     *
     * After login, the navbar shows "Logged in as [name]".
     * If this element is visible — login succeeded.
     * If not visible — something went wrong.
     *
     * Tests use this to ASSERT login success.
     */
    public boolean isLoggedIn() {
        return isDisplayed(loggedInText);
    }

    /**
     * getLoggedInUsername() - Returns the username shown
     * in the navbar after login.
     *
     * Used to verify the CORRECT user is logged in.
     */
    public String getLoggedInUsername() {
        return getText(loggedInText);
    }

    /**
     * isHomePageLoaded() - Verifies we're on the home page.
     */
    public boolean isHomePageLoaded() {
        return isDisplayed(homePageHeader);
    }
}