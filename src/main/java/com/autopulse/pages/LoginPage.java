package com.autopulse.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * LoginPage - Page Object for the Login page.
 * URL: https://automationexercise.com/login
 *
 * This class owns:
 * 1. All locators on the login page
 * 2. All actions that can happen on the login page
 *
 * Tests NEVER write locators directly.
 * Tests ALWAYS use methods from this class.
 */
public class LoginPage extends BasePage {

    // =============================================
    // LOCATORS — What exists on this page
    // By.xpath / By.cssSelector / By.id etc.
    //
    // WHY private?
    // Locators are internal implementation details.
    // Tests shouldn't know or care how we find elements.
    // Tests just call the action methods below.
    // =============================================

    private By emailField =
            By.xpath("//input[@data-qa='login-email']");

    private By passwordField =
            By.xpath("//input[@data-qa='login-password']");

    private By loginButton =
            By.xpath("//button[@data-qa='login-button']");

    private By errorMessage =
            By.xpath("//p[contains(text(),'Your email or password')]");

    private By loggedInUsername =
            By.xpath("//a[contains(@href,'/profile')]/b");

    // =============================================
    // CONSTRUCTOR
    // Passes driver up to BasePage.
    // BasePage sets up driver and wait for us.
    // =============================================

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // =============================================
    // ACTIONS — What can happen on this page
    // =============================================

    /**
     * enterEmail() - Types into the email field.
     * Uses BasePage.type() which handles clearing + typing.
     */
    public void enterEmail(String email) {
        type(emailField, email);
    }

    /**
     * enterPassword() - Types into the password field.
     */
    public void enterPassword(String password) {
        type(passwordField, password);
    }

    /**
     * clickLoginButton() - Clicks the Login button.
     * Uses BasePage.click() which waits until clickable.
     */
    public void clickLoginButton() {
        click(loginButton);
    }

    /**
     * loginAs() - Complete login workflow in one method.
     *
     * This is a HIGH-LEVEL action method.
     * Combines multiple steps into one readable call.
     *
     * Usage in test:
     * loginPage.loginAs("user@email.com", "password")
     *
     * Returns HomePage because after successful login
     * the application navigates to the home page.
     * This is called METHOD CHAINING — one page returns
     * the next page, guiding the test through the flow.
     */
    public HomePage loginAs(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();
        return new HomePage(driver);
    }

    /**
     * getErrorMessage() - Gets the error text shown
     * when login fails with wrong credentials.
     */
    public String getErrorMessage() {
        return getText(errorMessage);
    }

    /**
     * isErrorMessageDisplayed() - Checks if error is visible.
     * Used to assert that invalid login shows error.
     */
    public boolean isErrorMessageDisplayed() {
        return isDisplayed(errorMessage);
    }

    /**
     * navigateToLoginPage() - Goes directly to login URL.
     * Some tests need to start directly on login page.
     */
    public void navigateToLoginPage() {
        driver.get("https://automationexercise.com/login");
        waitForPageLoad();
    }
}