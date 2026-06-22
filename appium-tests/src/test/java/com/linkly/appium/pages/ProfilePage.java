package com.linkly.appium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ProfilePage extends BasePage {

    @FindBy(id = "com.linkly_linksystem.parent:id/textProfileChildName")
    private WebElement profileNameLabel;

    @FindBy(id = "com.linkly_linksystem.parent:id/textProfileChildName")
    private WebElement editProfileButton;

    @FindBy(id = "com.linkly_linksystem.parent:id/textProfileChildAge")
    private WebElement changePasswordButton;

    public ProfilePage(WebDriver driver) {
        super(driver);
    }

    private void ensureProfileScreen() {
        if (driver instanceof io.appium.java_client.android.AndroidDriver) {
            io.appium.java_client.android.AndroidDriver androidDriver = (io.appium.java_client.android.AndroidDriver) driver;
            
            String currentActivity = "";
            for (int i = 0; i < 10; i++) {
                currentActivity = androidDriver.currentActivity();
                System.out.println("DEBUG: ProfilePage currentActivity is: " + currentActivity);
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
            try {
                if (androidDriver.findElements(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/textProfileChildName")).isEmpty()) {
                    androidDriver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/nav_profile")).click();
                    Thread.sleep(1500);
                }
            } catch (Exception e) {
                System.out.println("Failed to navigate to Profile tab: " + e.getMessage());
            }
        }
    }

    public String getProfileName() {
        ensureProfileScreen();
        return profileNameLabel != null ? profileNameLabel.getText() : "";
    }

    public void clickEditProfile() {
        ensureProfileScreen();
        if (editProfileButton != null) {
            editProfileButton.click();
        }
    }

    public void clickChangePassword() {
        ensureProfileScreen();
        if (changePasswordButton != null) {
            changePasswordButton.click();
        }
    }
}
