package tests;

import base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ApiDemosTest - Test class for ApiDemos application
 *
 * Contains:
 * - 2 successful navigation tests
 * - 3 NoSuchElementException tests (triggers Inspector automatically)
 */
public class ApiDemosTest extends BaseTest {

    // ═══════════════════════════════════════════════════════════════════════════════
    // SUCCESSFUL TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Navigate to Accessibility category")
    public void testAccessibilityNavigation() {
        System.out.println("\n[TEST] Navigating to Accessibility...");

        clickByText("Accessibility");
        sleep(500);

        Assert.assertTrue(driver.getPageSource().contains("Accessibility"));
        System.out.println("[TEST] Success!");

        goBack();
    }

    @Test(priority = 2, description = "Navigate to Views > Buttons")
    public void testViewsButtonsNavigation() {
        System.out.println("\n[TEST] Navigating to Views > Buttons...");

        clickByText("Views");
        sleep(500);
        clickByText("Buttons");
        sleep(500);

        Assert.assertTrue(driver.getPageSource().contains("Button"));
        System.out.println("[TEST] Success!");

        goBack();
        goBack();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INSPECTOR TESTS - NoSuchElementException (3 Tests)
    // Inspector is triggered automatically when element is not found
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test(priority = 10, description = "NoSuchElementException: Wrong ID search")
    public void testInspector_NoSuchElement_1() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println(" TEST-1: NoSuchElementException - Wrong ID");
        System.out.println(" Inspector will be triggered automatically!");
        System.out.println("═".repeat(70));

        clickByText("Views");
        sleep(500);

        System.out.println("\n[SEARCHING] io.appium.android.apis:id/button_wrong");

        try {
            findById("io.appium.android.apis:id/button_wrong");
        } catch (Exception e) {
            System.out.println("\n[OK] Inspector output above!");
        }

        goBack();
    }

    @Test(priority = 11, description = "NoSuchElementException: Wrong Text search")
    public void testInspector_NoSuchElement_2() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println(" TEST-2: NoSuchElementException - Wrong Text");
        System.out.println(" Inspector will be triggered automatically!");
        System.out.println("═".repeat(70));

        System.out.println("\n[SEARCHING] 'Aksesibiliti' (typo)");

        try {
            findByText("Aksesibiliti");
        } catch (Exception e) {
            System.out.println("\n[OK] Inspector output above!");
        }
    }

    @Test(priority = 12, description = "NoSuchElementException: Wrong XPath search")
    public void testInspector_NoSuchElement_3() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println(" TEST-3: NoSuchElementException - Wrong XPath");
        System.out.println(" Inspector will be triggered automatically!");
        System.out.println("═".repeat(70));

        clickByText("App");
        sleep(500);

        System.out.println("\n[SEARCHING] [@text='Activity123']");

        try {
            findElement(By.xpath("//*[@text='Activity123']"));
        } catch (Exception e) {
            System.out.println("\n[OK] Inspector output above!");
        }

        goBack();
    }
}
