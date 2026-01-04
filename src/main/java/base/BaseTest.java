package base;

import utilities.AndroidElementInspector;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

/**
 * BaseTest - Base class for all test classes
 *
 * Provides:
 * - AndroidDriver setup and teardown
 * - Element finding methods with automatic Inspector integration
 * - Helper methods for common operations
 */
public class BaseTest {

    protected AndroidDriver driver;
    protected WebDriverWait wait;

    private static final String APPIUM_SERVER_URL = "http://127.0.0.1:4723/wd/hub";

    @BeforeMethod
    public void setUp() throws MalformedURLException {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setAutomationName("UiAutomator2")
                .setAppPackage("io.appium.android.apis")
                .setAppActivity("io.appium.android.apis.ApiDemos")
                .setNoReset(true)
                .setNewCommandTimeout(Duration.ofSeconds(300));

        driver = new AndroidDriver(new URL(APPIUM_SERVER_URL), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        System.out.println("\n[INFO] Driver started - ApiDemos app launched");
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("[INFO] Driver closed\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ELEMENT FINDING - NoSuchElementException triggers Inspector
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Find element by locator. If not found, Inspector is triggered automatically.
     */
    protected WebElement findElement(By locator) {
        try {
            return driver.findElement(locator);
        } catch (NoSuchElementException e) {
            AndroidElementInspector.inspect(driver, locator.toString(), e);
            throw e;
        }
    }

    /**
     * Find multiple elements by locator. If empty list, Inspector is triggered.
     */
    protected List<WebElement> findElements(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        if (elements.isEmpty()) {
            AndroidElementInspector.inspect(driver, locator.toString(),
                new NoSuchElementException("Element list is empty: " + locator));
        }
        return elements;
    }

    /**
     * Wait for element and find. If timeout, Inspector is triggered.
     */
    protected WebElement waitAndFind(By locator) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (org.openqa.selenium.TimeoutException e) {
            AndroidElementInspector.inspect(driver, locator.toString(),
                new NoSuchElementException("Timeout waiting for: " + locator));
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Find element by text attribute
     */
    protected WebElement findByText(String text) {
        return findElement(By.xpath("//*[@text='" + text + "']"));
    }

    /**
     * Click element by text attribute
     */
    protected void clickByText(String text) {
        findByText(text).click();
    }

    /**
     * Find element by resource-id
     */
    protected WebElement findById(String resourceId) {
        return findElement(By.id(resourceId));
    }

    /**
     * Click element by resource-id
     */
    protected void clickById(String resourceId) {
        findById(resourceId).click();
    }

    /**
     * Navigate back
     */
    protected void goBack() {
        driver.navigate().back();
    }

    /**
     * Sleep for specified milliseconds
     */
    protected void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
