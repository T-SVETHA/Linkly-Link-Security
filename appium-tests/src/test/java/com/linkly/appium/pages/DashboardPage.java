package com.linkly.appium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class DashboardPage extends BasePage {

    @FindBy(id = "com.linkly_linksystem.parent:id/textDashboardSubtitle")
    private WebElement title;

    @FindBy(id = "com.linkly_linksystem.parent:id/bottomNavigation")
    private WebElement navigationMenuButton;

    @FindBy(id = "com.linkly_linksystem.parent:id/nav_profile")
    private WebElement profileButton;

    @FindBy(id = "com.linkly_linksystem.parent:id/nav_alerts")
    private WebElement notificationsButton;

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    private void ensureLoggedIn() {
        if (driver instanceof io.appium.java_client.android.AndroidDriver) {
            io.appium.java_client.android.AndroidDriver androidDriver = (io.appium.java_client.android.AndroidDriver) driver;
            
            String currentActivity = "";
            for (int i = 0; i < 10; i++) {
                currentActivity = androidDriver.currentActivity();
                System.out.println("DEBUG: DashboardPage currentActivity is: " + currentActivity);
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

    public boolean isTitleDisplayed() {
        ensureLoggedIn();
        return title != null && title.isDisplayed();
    }

    public String getTitleText() {
        ensureLoggedIn();
        return title != null ? title.getText() : "";
    }

    public void openMenu() {
        ensureLoggedIn();
        if (navigationMenuButton != null) {
            navigationMenuButton.click();
        }
    }

    public void clickProfile() {
        ensureLoggedIn();
        if (profileButton != null) {
            profileButton.click();
        }
    }

    public void clickNotifications() {
        ensureLoggedIn();
        if (notificationsButton != null) {
            notificationsButton.click();
        }
    }
}
