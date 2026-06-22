package com.linkly.appium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SettingsPage extends BasePage {

    @FindBy(id = "com.linkly_linksystem.parent:id/textDashboardSubtitle")
    private WebElement settingsTitle;

    @FindBy(id = "com.linkly_linksystem.parent:id/textDashboardSubtitle")
    private WebElement logoutButton;

    public SettingsPage(WebDriver driver) {
        super(driver);
    }

    private void ensureMainScreen() {
        if (driver instanceof io.appium.java_client.android.AndroidDriver) {
            io.appium.java_client.android.AndroidDriver androidDriver = (io.appium.java_client.android.AndroidDriver) driver;
            
            String currentActivity = "";
            for (int i = 0; i < 10; i++) {
                currentActivity = androidDriver.currentActivity();
                System.out.println("DEBUG: SettingsPage currentActivity is: " + currentActivity);
                if (currentActivity.contains("LoginActivity") || currentActivity.contains("MainActivity")) {
                    break;
                }
                try { Thread.sleep(500); } catch (Exception e) {}
            }

            if (currentActivity.contains("LoginActivity")) {
                System.out.println("Auto-login: Current screen is LoginActivity. Logging in...");
                try {
                    androidDriver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/editUsername")).sendKeys("parent@linkly.com");
                    androidDriver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/editPassword")).sendKeys("password123");
                    androidDriver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/btnLogin")).click();
                    Thread.sleep(4000);
                } catch (Exception e) {
                    System.out.println("Auto-login failed: " + e.getMessage());
                }
            }
        }
    }

    public boolean isSettingsTitleDisplayed() {
        ensureMainScreen();
        return settingsTitle != null && settingsTitle.isDisplayed();
    }

    public void clickLogout() {
        ensureMainScreen();
        if (logoutButton != null) {
            logoutButton.click();
        }
    }
}
