package com.autopulse.tests.ui;

import com.autopulse.pages.CartPage;
import com.autopulse.pages.ProductDetailPage;
import com.autopulse.pages.ProductsPage;
import com.autopulse.tests.BaseTest;
import com.autopulse.utils.ExtentReportManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * ProductTest - Tests for product browsing and cart functionality.
 *
 * These tests cover the most business-critical flow:
 * User finds a product → views details → adds to cart.
 *
 * If ANY of these fail in production — the company
 * loses money directly. That's why they're important.
 */
public class ProductTest extends BaseTest {

    private ProductsPage productsPage;

    @BeforeMethod
    public void setUpProductsPage() {
        productsPage = new ProductsPage(driver);
        productsPage.navigateToProducts();
    }

    // ─────────────────────────────────────────────────
    // TEST 1 — Products page loads correctly
    // ─────────────────────────────────────────────────

    @Test(description = "Verify products page loads with items")
    public void verifyProductsPageLoads() {
        System.out.println("🧪 Test: Products page loads");

        ExtentReportManager.logInfo(
                "Navigated to products page"
        );

        // Verify page header is visible
        Assert.assertTrue(
                productsPage.isProductsPageLoaded(),
                "Products page header not found"
        );

        ExtentReportManager.logPass("Products page loaded");

        // Verify products are actually showing
        int count = productsPage.getProductCount();
        Assert.assertTrue(
                count > 0,
                "No products found on products page"
        );

        ExtentReportManager.logPass(
                "Products visible on page: " + count
        );

        System.out.println("✅ Products page loaded with "
                + count + " products");
    }

    // ─────────────────────────────────────────────────
    // TEST 2 — Search functionality works
    // ─────────────────────────────────────────────────

    @Test(description = "Verify product search returns results")
    public void verifyProductSearch() {
        System.out.println("🧪 Test: Product search");

        String searchKeyword = "top";
        ExtentReportManager.logInfo(
                "Searching for: " + searchKeyword
        );

        // ACT — search for a product
        productsPage.searchProduct(searchKeyword);

        // ASSERT 1 — search results header appears
        Assert.assertTrue(
                productsPage.isSearchResultsDisplayed(),
                "Search results header not displayed after search"
        );

        ExtentReportManager.logPass("Search results section appeared");

        // ASSERT 2 — results are actually found
        int resultCount = productsPage.getSearchResultCount();
        Assert.assertTrue(
                resultCount > 0,
                "No results found for search: " + searchKeyword
        );

        ExtentReportManager.logPass(
                "Search returned " + resultCount + " results"
        );

        System.out.println("✅ Search found " + resultCount
                + " results for '" + searchKeyword + "'");
    }

    // ─────────────────────────────────────────────────
    // TEST 3 — Product detail page loads correctly
    // ─────────────────────────────────────────────────

    @Test(description = "Verify product detail page shows correct info")
    public void verifyProductDetailPage() {
        System.out.println("🧪 Test: Product detail page");

        // ACT — click first product
        ProductDetailPage detailPage =
                productsPage.viewProduct(0);

        ExtentReportManager.logInfo(
                "Clicked View Product on first item"
        );

        // ASSERT — detail page loaded
        Assert.assertTrue(
                detailPage.isProductDetailPageLoaded(),
                "Product detail page did not load"
        );

        // ASSERT — product has a name
        String productName = detailPage.getProductName();
        Assert.assertFalse(
                productName.isEmpty(),
                "Product name should not be empty"
        );

        // ASSERT — product has a price
        String productPrice = detailPage.getProductPrice();
        Assert.assertFalse(
                productPrice.isEmpty(),
                "Product price should not be empty"
        );

        ExtentReportManager.logPass(
                "Product loaded — Name: " + productName
                        + " | Price: " + productPrice
        );

        System.out.println("✅ Product detail verified");
        System.out.println("   Name: " + productName);
        System.out.println("   Price: " + productPrice);
    }

    // ─────────────────────────────────────────────────
    // TEST 4 — Add product to cart (THE most important test)
    // ─────────────────────────────────────────────────

    @Test(description = "Verify product can be added to cart")
    public void verifyAddProductToCart() {
        System.out.println("🧪 Test: Add product to cart");

        // Step 1 — View first product
        ProductDetailPage detailPage =
                productsPage.viewProduct(0);

        String productName = detailPage.getProductName();
        ExtentReportManager.logInfo(
                "Viewing product: " + productName
        );

        // Step 2 — Add to cart and go to cart page
        CartPage cartPage = detailPage.addToCart();

        ExtentReportManager.logInfo(
                "Clicked Add to Cart. Navigated to cart."
        );

        // ASSERT 1 — we're on cart page
        Assert.assertTrue(
                cartPage.isCartPageLoaded(),
                "Cart page did not load after adding product"
        );

        ExtentReportManager.logPass("Cart page loaded");

        // ASSERT 2 — cart is not empty
        Assert.assertFalse(
                cartPage.isCartEmpty(),
                "Cart should not be empty after adding a product"
        );

        // ASSERT 3 — cart has exactly 1 item
        int cartCount = cartPage.getCartItemCount();
        Assert.assertEquals(
                cartCount, 1,
                "Cart should have 1 item but found: " + cartCount
        );

        ExtentReportManager.logPass(
                "Cart has " + cartCount + " item as expected"
        );

        System.out.println("✅ Product added to cart successfully");
        System.out.println("   Cart items: " + cartCount);
    }

    // ─────────────────────────────────────────────────
    // TEST 5 — End-to-end: Search → View → Add to Cart
    // ─────────────────────────────────────────────────

    @Test(description = "End-to-end: search product and add to cart")
    public void verifySearchAndAddToCart() {
        System.out.println("🧪 Test: Search and add to cart E2E");

        String searchKeyword = "top";

        // Step 1 — Search
        productsPage.searchProduct(searchKeyword);
        ExtentReportManager.logInfo("Searched for: " + searchKeyword);

        Assert.assertTrue(
                productsPage.getSearchResultCount() > 0,
                "No results for search: " + searchKeyword
        );

        // Step 2 — View first result
        String expectedProductName =
                productsPage.getProductNameAt(0);
        ProductDetailPage detailPage =
                productsPage.viewProduct(0);

        ExtentReportManager.logInfo(
                "Viewing product: " + expectedProductName
        );

        // Step 3 — Add to cart
        CartPage cartPage = detailPage.addToCart();
        ExtentReportManager.logInfo("Added to cart");

        // Step 4 — Verify correct product is in cart
        Assert.assertTrue(
                cartPage.getCartItemCount() > 0,
                "Cart is empty after adding product"
        );

        // Verify the right product is in cart
        // (partial match — product name contains search keyword)
        boolean productFound =
                cartPage.isProductInCart(searchKeyword);

        // Log what's actually in cart for debugging
        System.out.println("   Cart contains: "
                + cartPage.getCartProductNames());

        Assert.assertTrue(
                productFound,
                "Searched product not found in cart. Cart has: "
                        + cartPage.getCartProductNames()
        );

        ExtentReportManager.logPass(
                "Correct product in cart: " + expectedProductName
        );

        System.out.println("✅ E2E test passed — searched, "
                + "found, and added to cart");
    }
}