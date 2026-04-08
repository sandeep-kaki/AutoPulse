package com.autopulse.tests.ui;

import com.autopulse.pages.HomePage;
import com.autopulse.pages.LoginPage;
import com.autopulse.tests.BaseTest;
import com.autopulse.utils.ExcelReader;
import com.autopulse.utils.ExtentReportManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * LoginTest - Tests for login functionality.
 *
 * Extends BaseTest — so browser setup/teardown is automatic.
 *
 * Notice how clean each test is:
 * - No driver setup
 * - No waits written manually
 * - No locators visible
 * - Reads like plain English
 *
 * That's the power of Page Object Model + BaseTest.
 */
public class LoginTest extends BaseTest {

    private LoginPage loginPage;

    /**
     * @BeforeMethod runs before EACH test in this class.
     * (BaseTest's @BeforeMethod already opened the browser
     *  and navigated to base URL — this adds login page nav)
     */
    @BeforeMethod
    public void setUpLoginPage() {
        loginPage = new LoginPage(driver);
        loginPage.navigateToLoginPage();
    }

    /**
     * Test 1 — Valid login should succeed.
     *
     * These are real credentials for automationexercise.com.
     * The site allows you to register and use your account.
     * Replace with credentials you register with.
     */
    @Test(description = "Verify login with valid credentials succeeds")
    public void verifyValidLogin() {
        System.out.println("🧪 Test: Valid login");

        // ACT — perform the login
        HomePage homePage = loginPage.loginAs(
                "sandeepkaki2213@gmail.com",  // ← Replace with your registered email
                "Sandeep221355@"          // ← Replace with your password
        );

        // ASSERT — verify login actually worked
        Assert.assertTrue(
                homePage.isLoggedIn(),
                "User should be logged in after valid credentials"
        );

        System.out.println("✅ Valid login verified. User is logged in.");
    }

    /**
     * Test 2 — Wrong password should show error.
     */
    @Test(description = "Verify wrong password shows error message")
    public void verifyInvalidPasswordShowsError() {
        System.out.println("🧪 Test: Invalid password");

        // ACT — login with wrong password
        loginPage.loginAs(
                "wrong@email.com",
                "wrongpassword123"
        );

        // ASSERT — error message should appear
        Assert.assertTrue(
                loginPage.isErrorMessageDisplayed(),
                "Error message should be displayed for wrong credentials"
        );

        // ASSERT — verify the exact error text
        String errorText = loginPage.getErrorMessage();
        Assert.assertTrue(
                errorText.contains("Your email or password is incorrect"),
                "Error message text is wrong. Actual: " + errorText
        );

        System.out.println("✅ Invalid login shows correct error: "
                + errorText);
    }

    /**
     * Test 3 — Empty credentials should show error.
     */
    @Test(description = "Verify empty credentials shows error")
    public void verifyEmptyCredentialsShowsError() {
        System.out.println("🧪 Test: Empty credentials");

        // ACT — click login without entering anything
        loginPage.clickLoginButton();

        // ASSERT — should not navigate away from login page
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(
                currentUrl.contains("/login"),
                "Should stay on login page with empty credentials. " +
                        "URL: " + currentUrl
        );

        System.out.println("✅ Empty credentials kept user on login page");
    }

    // Add this import at the top of the file:
    // import com.autopulse.utils.ExcelReader;
    // import java.util.Map;

    private static final String LOGIN_DATA_PATH =
            "src/test/resources/testdata/loginData.xlsx";

    /**
     * @DataProvider — supplies test data to the test method.
     *
     * This method returns Object[][] — a 2D array.
     * Each inner array = one test execution.
     * TestNG calls the test once for EACH row.
     *
     * ExcelReader reads your loginData.xlsx and converts
     * it to the Object[][] format TestNG needs.
     */
    @org.testng.annotations.DataProvider(name = "loginDataProvider")
    public Object[][] loginDataProvider() {
        return ExcelReader.getTestDataAs2DArray(
                LOGIN_DATA_PATH, "Sheet1"
        );
    }

    /**
     * Data-driven login test — runs once per Excel row.
     *
     * @param testData - one row from Excel as a Map
     *                   keys = column headers
     *                   values = cell values
     *
     * Same test method. Different data each time.
     * One method covers: valid login, wrong password,
     * invalid email, unregistered user — all from Excel.
     */
    @Test(
            dataProvider = "loginDataProvider",
            description = "Data-driven login test from Excel"
    )
    public void verifyLoginDataDriven(Map<String, String> testData) {

        String email          = testData.get("email");
        String password       = testData.get("password");
        String expectedResult = testData.get("expectedResult");
        String description    = testData.get("testDescription");

        System.out.println("🧪 Running: " + description);
        System.out.println("   Email: " + email);
        System.out.println("   Expected: " + expectedResult);

        ExtentReportManager.logInfo("Scenario: " + description);
        ExtentReportManager.logInfo("Email: " + email);

        // ACT
        loginPage.loginAs(email, password);

        // ASSERT based on expected result column
        if (expectedResult.equals("success")) {

            HomePage homePage = new HomePage(driver);
            Assert.assertTrue(
                    homePage.isLoggedIn(),
                    "Expected successful login but user is not logged in"
            );
            ExtentReportManager.logPass(
                    "Login succeeded as expected for: " + email
            );

        } else {

            Assert.assertTrue(
                    loginPage.isErrorMessageDisplayed(),
                    "Expected error message but none appeared"
            );
            ExtentReportManager.logPass(
                    "Login correctly rejected with error for: " + email
            );
        }
    }


}