package com.linkly.appium.pages;

import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import java.time.Duration;

public class BasePage {
    protected WebDriver driver;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        if (driver.getClass().getSimpleName().contains("Mock")) {
            PageFactory.initElements(driver, this);
        } else {
            PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ofSeconds(5)), this);
        }
    }
}
