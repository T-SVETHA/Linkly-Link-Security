package com.linkly.appium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SignupPage extends BasePage {

    @FindBy(id = "com.linkly_linksystem.parent:id/editUsername")
    private WebElement emailInput;

    @FindBy(id = "com.linkly_linksystem.parent:id/editPassword")
    private WebElement passwordInput;

    @FindBy(id = "com.linkly_linksystem.parent:id/btnLogin")
    private WebElement registerButton;

    public SignupPage(WebDriver driver) {
        super(driver);
    }

    public void enterUsername(String username) {
        // No username input field in parent app layout
    }

    public void enterEmail(String email) {
        if (emailInput != null) {
            emailInput.sendKeys(email);
        }
    }

    public void enterPassword(String password) {
        if (passwordInput != null) {
            passwordInput.sendKeys(password);
        }
    }

    public void clickRegister() {
        if (registerButton != null) {
            registerButton.click();
        }
    }
}
