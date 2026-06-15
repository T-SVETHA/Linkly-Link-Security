package com.linkly.appium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPage extends BasePage {

    @FindBy(id = "com.linkly_linksystem.parent:id/editUsername")
    private WebElement emailInput;

    @FindBy(id = "com.linkly_linksystem.parent:id/editPassword")
    private WebElement passwordInput;

    @FindBy(id = "com.linkly_linksystem.parent:id/btnLogin")
    private WebElement loginButton;

    @FindBy(id = "com.linkly_linksystem.parent:id/btnToggleMode")
    private WebElement toggleModeButton;

    @FindBy(id = "com.linkly_linksystem.parent:id/textForgotPassword")
    private WebElement forgotPasswordLink;

    public LoginPage(WebDriver driver) {
        super(driver);
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

    public void clickLogin() {
        if (loginButton != null) {
            loginButton.click();
        }
    }

    public String getErrorMessage() {
        return "";
    }

    public void clickForgotPassword() {
        if (forgotPasswordLink != null) {
            forgotPasswordLink.click();
        }
    }

    public void clickToggleMode() {
        if (toggleModeButton != null) {
            toggleModeButton.click();
        }
    }
}
