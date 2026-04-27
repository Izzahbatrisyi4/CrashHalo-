package com.example.crashhaloapp;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class AppiumTest {

    private AndroidDriver driver;

    @Before
    public void setUp() throws MalformedURLException {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setDeviceName("emulator-5554") // Change to your device name from 'adb devices'
                .setAppPackage("com.example.crashhaloapp")
                .setAppActivity(".MainActivity")
                .setAutomationName("UiAutomator2")
                .setNoReset(false);

        // Point this to your local Appium Server (usually port 4723)
        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @Test
    public void testAppLaunchAndNavigation() {
        // 1. Verify we are on the Home Screen
        WebElement header = driver.findElement(By.xpath("//android.widget.TextView[@text='🛡️ CRASHHALO']"));
        assert(header.isDisplayed());

        // 2. Click the Settings button (btn_settings)
        WebElement btnSettings = driver.findElement(By.id("com.example.crashhaloapp:id/btn_settings"));
        btnSettings.click();

        // 3. Verify Settings page opened
        WebElement settingsTitle = driver.findElement(By.xpath("//android.widget.TextView[@text='SETTINGS']"));
        assert(settingsTitle.isDisplayed());
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
