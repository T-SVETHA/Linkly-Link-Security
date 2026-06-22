package com.linkly.appium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SplashPage extends BasePage {

    @FindBy(xpath = "//android.widget.ImageView")
    private WebElement logo;

    @FindBy(xpath = "//android.widget.TextView[@text='Linkly-Link']")
    private WebElement appTitle;

    public SplashPage(WebDriver driver) {
        super(driver);
    }

    private void ensureSplashScreen() {
        if (driver instanceof io.appium.java_client.android.AndroidDriver) {
            io.appium.java_client.android.AndroidDriver androidDriver = (io.appium.java_client.android.AndroidDriver) driver;
            String currentActivity = androidDriver.currentActivity();
            if (!currentActivity.contains("SplashActivity")) {
                System.out.println("Auto-restart: App is not on SplashActivity (currently " + currentActivity + "). Restarting to SplashActivity...");
                try {
                    androidDriver.executeScript(
                        "mobile: startActivity", 
                        java.util.Map.of("component", "com.linkly_linksystem.parent/com.linkly_linksystem.parent.SplashActivity")
                    );
                    for (int i = 0; i < 10; i++) {
                        currentActivity = androidDriver.currentActivity();
                        if (currentActivity.contains("SplashActivity")) {
                            break;
                        }
                        Thread.sleep(300);
                    }
                } catch (Exception e) {
                    System.out.println("Failed to start SplashActivity: " + e.getMessage());
                }
            }
        }
    }

    public boolean isLogoDisplayed() {
        ensureSplashScreen();
        return logo != null && logo.isDisplayed();
    }

    public String getAppTitle() {
        ensureSplashScreen();
        return appTitle != null ? appTitle.getText() : "";
    }
}
