package com.i2i.cryptopal.e2e;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthPageSeleniumTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private final String appUrl = System.getenv().getOrDefault("APP_URL", "http://localhost:5173");
    private final String gridUrl = System.getenv().getOrDefault("SELENIUM_GRID_URL", "http://localhost:4444/wd/hub");

    @BeforeEach
    void setUp() throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--window-size=1440,1000", "--no-sandbox");
        driver = new RemoteWebDriver(URI.create(gridUrl).toURL(), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() { if (driver != null) driver.quit(); }

    @Test
    void rendersTheAuthenticationPage() {
        driver.get(appUrl);
        assertTrue(driver.getTitle().contains("CryptoPal"));
        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".auth-form"))).isDisplayed());
    }

    @Test
    void switchesToRegistrationForm() {
        driver.get(appUrl);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".auth-tabs button:nth-child(2)"))).click();
        assertEquals(3, driver.findElements(By.cssSelector(".auth-form input")).size());
    }

    @Test
    void togglesPasswordVisibility() {
        driver.get(appUrl);
        WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")));
        driver.findElement(By.cssSelector(".input-icon-action")).click();
        assertEquals("text", password.getAttribute("type"));
    }
}