package com.autopulse.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * ProductDetailPage - Page Object for individual product page.
 * URL: https://automationexercise.com/product_details/{id}
 *
 * This page shows:
 * - Product name, price, category, availability
 * - Quantity input
 * - Add to Cart button
 */
public class ProductDetailPage extends BasePage {

    // ── LOCATORS ──────────────────────────────────────

    private By productName =
            By.xpath("//div[@class='product-information']//h2");

    private By productPrice =
            By.xpath("//div[@class='product-information']//span//span");

    private By productCategory =
            By.xpath("//div[@class='product-information']/p[1]");

    private By productAvailability =
            By.xpath("//div[@class='product-information']/p[2]");

    private By quantityInput =
            By.xpath("//input[@id='quantity']");

    private By addToCartButton =
            By.xpath("//button[@class='btn btn-default cart']");

    // Modal that appears after adding to cart
    private By addedToCartModal =
            By.xpath("//div[@id='cartModal']");

    private By modalContinueButton =
            By.xpath("//button[contains(text(),'Continue Shopping')]");

    private By modalViewCartLink =
            By.xpath("//div[@id='cartModal']//a[contains(@href,'/view_cart')]");

    // ── CONSTRUCTOR ───────────────────────────────────

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    // ── GETTERS — Read product information ────────────

    public String getProductName() {
        return getText(productName);
    }

    public String getProductPrice() {
        return getText(productPrice);
    }

    public String getProductCategory() {
        return getText(productCategory);
    }

    public String getProductAvailability() {
        return getText(productAvailability);
    }

    // ── VERIFICATION ──────────────────────────────────

    /**
     * isProductDetailPageLoaded() - Confirms page loaded.
     * Product name must be visible for page to be "ready".
     */
    public boolean isProductDetailPageLoaded() {
        return isDisplayed(productName);
    }

    // ── ACTIONS ───────────────────────────────────────

    /**
     * setQuantity() - Changes the quantity before adding to cart.
     *
     * Clears the default quantity (usually "1") and types new value.
     * Important for testing quantity validation scenarios.
     */
    public void setQuantity(int quantity) {
        type(quantityInput, String.valueOf(quantity));
    }

    /**
     * addToCart() - Clicks Add to Cart and handles the modal.
     *
     * After clicking Add to Cart — a modal popup appears.
     * The modal has two options:
     * 1. "Continue Shopping" — stay on product page
     * 2. "View Cart" — go to cart page
     *
     * This method returns CartPage because most tests
     * want to verify the cart after adding.
     *
     * @param viewCart - true = go to cart, false = continue shopping
     */
    public CartPage addToCart(boolean viewCart) {
        click(addToCartButton);

        // Wait for modal to appear
        wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions
                        .visibilityOfElementLocated(addedToCartModal)
        );

        System.out.println("🛒 Added to cart. Modal appeared.");

        if (viewCart) {
            click(modalViewCartLink);
        } else {
            click(modalContinueButton);
        }

        waitForPageLoad();
        return new CartPage(driver);
    }

    /**
     * addToCart() - Default: goes to cart after adding.
     * Convenience overload — most tests want to view cart.
     */
    public CartPage addToCart() {
        return addToCart(true);
    }
}