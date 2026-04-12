package com.autopulse.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

/**
 * CartPage - Page Object for the Shopping Cart page.
 * URL: https://automationexercise.com/view_cart
 *
 * This page shows all items added to cart with:
 * - Product name
 * - Price per unit
 * - Quantity
 * - Total price
 * - Remove button
 *
 * KEY LEARNING — Table-based web pages:
 * Cart items are in an HTML table. Each row = one product.
 * We use XPath to target specific columns within rows.
 */
public class CartPage extends BasePage {

    // ── LOCATORS ──────────────────────────────────────

    private By cartPageHeader =
            By.xpath("//li[@class='active' and contains(text(),'Shopping Cart')]");

    // All product rows in the cart table
    private By cartRows =
            By.xpath("//tbody/tr");

    // Product names within cart rows
    private By cartProductNames =
            By.xpath("//td[@class='cart_description']//h4/a");

    // Product prices in cart
    private By cartProductPrices =
            By.xpath("//td[@class='cart_price']/p");

    // Quantities in cart
    private By cartQuantities =
            By.xpath("//td[@class='cart_quantity']/button");

    // Total prices per item
    private By cartTotalPrices =
            By.xpath("//td[@class='cart_total']/p");

    // Delete/remove buttons
    private By removeButtons =
            By.xpath("//td[@class='cart_delete']//a");

    private By emptyCartMessage =
            By.xpath("//b[contains(text(),'Cart is empty')]");

    private By proceedToCheckoutButton =
            By.xpath("//a[contains(text(),'Proceed To Checkout')]");

    // ── CONSTRUCTOR ───────────────────────────────────

    public CartPage(WebDriver driver) {
        super(driver);
    }

    // ── NAVIGATION ────────────────────────────────────

    public void navigateToCart() {
        driver.get("https://automationexercise.com/view_cart");
        waitForPageLoad();
    }

    // ── VERIFICATION ──────────────────────────────────

    /**
     * isCartPageLoaded() - Verifies we're on cart page.
     */
    public boolean isCartPageLoaded() {
        return getCurrentUrl().contains("view_cart");
    }

    /**
     * getCartItemCount() - Returns number of items in cart.
     *
     * Each row in the cart table = one product.
     * Count the rows = count the items.
     */
    public int getCartItemCount() {
        List<WebElement> rows = driver.findElements(cartRows);
        return rows.size();
    }

    /**
     * isCartEmpty() - Checks if cart has no items.
     */
    public boolean isCartEmpty() {
        return isDisplayed(emptyCartMessage);
    }

    /**
     * isProductInCart() - Checks if specific product is in cart.
     *
     * @param productNameKeyword - partial name to search for
     *
     * WHY partial match?
     * Full product names are long. Tests shouldn't hardcode
     * the entire name — brittle if name changes slightly.
     * Partial match like "top" finds "Blue Top", "Summer Top" etc.
     */
    public boolean isProductInCart(String productNameKeyword) {
        List<WebElement> names =
                driver.findElements(cartProductNames);

        for (WebElement nameElement : names) {
            if (nameElement.getText().toLowerCase()
                    .contains(productNameKeyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * getCartProductNames() - Returns list of all product names.
     * Useful for detailed assertions.
     */
    public List<String> getCartProductNames() {
        List<WebElement> nameElements =
                driver.findElements(cartProductNames);
        List<String> names = new java.util.ArrayList<>();
        for (WebElement el : nameElements) {
            names.add(el.getText().trim());
        }
        return names;
    }

    /**
     * getProductQuantityAt() - Gets quantity of item at index.
     */
    public String getProductQuantityAt(int index) {
        List<WebElement> quantities =
                driver.findElements(cartQuantities);
        return quantities.get(index).getText().trim();
    }

    /**
     * getTotalPriceAt() - Gets total price of item at index.
     */
    public String getTotalPriceAt(int index) {
        List<WebElement> totals =
                driver.findElements(cartTotalPrices);
        return totals.get(index).getText().trim();
    }
}