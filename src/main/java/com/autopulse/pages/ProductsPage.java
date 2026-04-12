package com.autopulse.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

/**
 * ProductsPage - Page Object for the Products listing page.
 * URL: https://automationexercise.com/products
 *
 * This page has:
 * - A list of all products
 * - A search box to filter products
 * - "View Product" buttons for each product
 *
 * IMPORTANT LEARNING — Dynamic lists in Selenium:
 * The products list is dynamic — different number of items
 * based on search. We use List<WebElement> to handle
 * any number of results, not just a fixed count.
 */
public class ProductsPage extends BasePage {

    // ── LOCATORS ──────────────────────────────────────

    private By productsPageHeader =
            By.xpath("//h2[contains(text(),'All Products')]");

    private By searchBox =
            By.xpath("//input[@id='search_product']");

    private By searchButton =
            By.xpath("//button[@id='submit_search']");

    private By searchResultsHeader =
            By.xpath("//h2[contains(text(),'Searched Products')]");

    // This locator matches ALL product cards on the page
    // We'll use it to count products and interact with specific ones
    private By allProductCards =
            By.xpath("//div[@class='productinfo text-center']");

    // "View Product" links — one per product card
    private By viewProductLinks =
            By.xpath("//div[@class='choose']//a[contains(@href,'/product_details/')]");

    // Product names shown on the listing cards
    private By productNames =
            By.xpath("//div[@class='productinfo text-center']/p");

    // Navbar cart icon — shows item count
    private By cartCountBadge =
            By.xpath("//span[contains(@class,'cart_quantity_count')]");

    // ── CONSTRUCTOR ───────────────────────────────────

    public ProductsPage(WebDriver driver) {
        super(driver);
    }

    // ── NAVIGATION ────────────────────────────────────

    /**
     * navigateToProducts() - Goes directly to products page.
     *
     * WHY navigate directly instead of clicking navbar?
     * Direct navigation is faster and more reliable.
     * Navbar clicks can fail due to overlays or animations.
     * In automation, direct URL is preferred for setup steps.
     * Only click UI elements when TESTING the click itself.
     */
    public void navigateToProducts() {
        driver.get("https://automationexercise.com/products");
        waitForPageLoad();
        closeAdPopupIfPresent();
    }

    // ── VERIFICATION ──────────────────────────────────

    /**
     * isProductsPageLoaded() - Confirms we're on products page.
     * Checks for the "All Products" header.
     */
    public boolean isProductsPageLoaded() {
        return isDisplayed(productsPageHeader);
    }

    /**
     * getProductCount() - Returns number of products visible.
     *
     * driver.findElements() (plural) returns a List.
     * Never throws exception if nothing found — returns
     * empty list instead. Safe for count checks.
     */
    public int getProductCount() {
        List<WebElement> products =
                driver.findElements(allProductCards);
        return products.size();
    }

    // ── SEARCH ────────────────────────────────────────

    /**
     * searchProduct() - Types in search box and submits.
     *
     * @param keyword - what to search for
     */
    public void searchProduct(String keyword) {
        type(searchBox, keyword);
        click(searchButton);
        waitForPageLoad();
        System.out.println("🔍 Searched for: " + keyword);
    }

    /**
     * isSearchResultsDisplayed() - Confirms search happened.
     * "Searched Products" header appears after search.
     */
    public boolean isSearchResultsDisplayed() {
        return isDisplayed(searchResultsHeader);
    }

    /**
     * getSearchResultCount() - How many products found.
     */
    public int getSearchResultCount() {
        return driver.findElements(allProductCards).size();
    }

    // ── PRODUCT INTERACTION ───────────────────────────

    /**
     * viewProduct() - Clicks "View Product" for a specific item.
     *
     * @param index - 0 = first product, 1 = second, etc.
     * @return ProductDetailPage - the page we land on
     *
     * WHY return ProductDetailPage?
     * After clicking View Product, browser goes to detail page.
     * Returning the new page object guides the test naturally.
     * Test doesn't need to manually create ProductDetailPage.
     */
    public ProductDetailPage viewProduct(int index) {
        List<WebElement> links =
                driver.findElements(viewProductLinks);

        if (index >= links.size()) {
            throw new RuntimeException(
                    "Product index " + index + " not found. " +
                            "Only " + links.size() + " products visible."
            );
        }

        // Scroll to the product first — it might be below viewport
        scrollIntoView(viewProductLinks);
        links.get(index).click();
        waitForPageLoad();

        System.out.println("👁️ Viewing product at index: " + index);
        return new ProductDetailPage(driver);
    }

    /**
     * getProductNameAt() - Gets name of product at given index.
     * Useful for assertions — check correct product was found.
     */
    public String getProductNameAt(int index) {
        List<WebElement> names =
                driver.findElements(productNames);
        return names.get(index).getText().trim();
    }
}