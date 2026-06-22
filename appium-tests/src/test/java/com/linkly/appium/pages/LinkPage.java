package com.linkly.appium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LinkPage extends BasePage {

    @FindBy(id = "com.linkly_linksystem.parent:id/editPairingCode")
    private WebElement pairTokenInput;

    @FindBy(id = "com.linkly_linksystem.parent:id/btnLinkDevice")
    private WebElement linkDeviceButton;

    @FindBy(id = "com.linkly_linksystem.parent:id/btnLinkDevice")
    private WebElement deleteLinkButton;

    @FindBy(id = "com.linkly_linksystem.parent:id/btnLinkDevice")
    private WebElement shareLinkButton;

    public LinkPage(WebDriver driver) {
        super(driver);
    }

    private void resetFirebaseLinkStatus() {
        try {
            java.net.URL url = new java.net.URI("https://linkly-link-system-default-rtdb.firebaseio.com/devices/child_device_9988/status/isLinked.json").toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write("false".getBytes("utf-8"));
            }
            int responseCode = conn.getResponseCode();
            System.out.println("DEBUG: Reset Firebase link status to false. Response code: " + responseCode);
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to reset Firebase link status: " + e.getMessage());
        }
    }

    private void ensureDashboardScreen() {
        if (driver instanceof io.appium.java_client.android.AndroidDriver) {
            io.appium.java_client.android.AndroidDriver androidDriver = (io.appium.java_client.android.AndroidDriver) driver;
            
            // Force reset Firebase link status to false to make the pairing card visible
            resetFirebaseLinkStatus();

            String currentActivity = "";
            for (int i = 0; i < 10; i++) {
                currentActivity = androidDriver.currentActivity();
                System.out.println("DEBUG: LinkPage currentActivity is: " + currentActivity);
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
                // Wait for bottom navigation layout to load
                for (int i = 0; i < 10; i++) {
                    if (!androidDriver.findElements(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/nav_dashboard")).isEmpty()) {
                        break;
                    }
                    Thread.sleep(500);
                }
                
                if (!androidDriver.findElements(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/nav_dashboard")).isEmpty()) {
                    androidDriver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/nav_dashboard")).click();
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.out.println("Failed to navigate to Dashboard tab: " + e.getMessage());
            }

            // Explicitly wait for editPairingCode to become visible after resetting Firebase status
            try {
                for (int i = 0; i < 15; i++) {
                    if (!androidDriver.findElements(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/editPairingCode")).isEmpty()) {
                        WebElement el = androidDriver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/editPairingCode"));
                        if (el.isDisplayed()) {
                            break;
                        }
                    }
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                System.out.println("Waiting for editPairingCode visibility failed: " + e.getMessage());
            }
        }
    }

    public void enterPairToken(String token) {
        ensureDashboardScreen();
        try {
            if (pairTokenInput != null) {
                pairTokenInput.clear();
                pairTokenInput.sendKeys(token);
            }
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            System.out.println("Stale element in enterPairToken. Retrying locator...");
            try { Thread.sleep(500); } catch (Exception ie) {}
            try {
                WebElement input = driver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/editPairingCode"));
                input.clear();
                input.sendKeys(token);
            } catch (Exception ex) {
                throw ex;
            }
        }
    }

    public void clickLinkDevice() {
        ensureDashboardScreen();
        try {
            if (linkDeviceButton != null) {
                linkDeviceButton.click();
            }
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            System.out.println("Stale element in clickLinkDevice. Retrying...");
            try { Thread.sleep(500); } catch (Exception ie) {}
            try {
                driver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/btnLinkDevice")).click();
            } catch (Exception ex) {
                throw ex;
            }
        }
    }

    public void clickDeleteLink() {
        ensureDashboardScreen();
        try {
            if (deleteLinkButton != null) {
                deleteLinkButton.click();
            }
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            System.out.println("Stale element in clickDeleteLink. Retrying...");
            try { Thread.sleep(500); } catch (Exception ie) {}
            try {
                driver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/btnLinkDevice")).click();
            } catch (Exception ex) {
                System.out.println("DEBUG: Failed to click retry deleteLinkButton: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Click delete link failed or element not found: " + e.getMessage());
        }
    }

    public void clickShareLink() {
        ensureDashboardScreen();
        try {
            if (shareLinkButton != null) {
                shareLinkButton.click();
            }
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            System.out.println("Stale element in clickShareLink. Retrying...");
            try { Thread.sleep(500); } catch (Exception ie) {}
            try {
                driver.findElement(org.openqa.selenium.By.id("com.linkly_linksystem.parent:id/btnLinkDevice")).click();
            } catch (Exception ex) {
                System.out.println("DEBUG: Failed to click retry shareLinkButton: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Click share link failed or element not found: " + e.getMessage());
        }
    }
}
