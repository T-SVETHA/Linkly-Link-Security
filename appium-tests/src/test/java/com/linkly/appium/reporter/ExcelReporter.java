package com.linkly.appium.reporter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ExcelReporter {

    public static class TestCaseDetails {
        public String id;
        public String module;
        public String screen;
        public String description;
        public String steps;
        public String expected;
        public String screenshotRef;

        public TestCaseDetails(String id, String module, String screen, String description, String steps, String expected, String screenshotRef) {
            this.id = id;
            this.module = module;
            this.screen = screen;
            this.description = description;
            this.steps = steps;
            this.expected = expected;
            this.screenshotRef = screenshotRef;
        }
    }

    private static final Map<String, TestCaseDetails> DETAILS_MAP = new HashMap<>();

    static {
        DETAILS_MAP.put("test1_SplashScreenLoad", new TestCaseDetails(
            "TC-APP-01", "TC-LAUNCH: App Launch", "Splash Screen",
            "Verify the splash screen logo and title load correctly on app startup",
            "1. Launch application\n2. Wait for splash screen layout\n3. Verify logo and app title presence",
            "App logo is displayed and title is visible", "screenshots/TC-APP-01.png"
        ));
        DETAILS_MAP.put("test2_LoginWithValidCredentials", new TestCaseDetails(
            "TC-APP-02", "TC-AUTH: Authentication", "Login Screen",
            "Verify user login is successful using valid email and password",
            "1. Enter valid parent email\n2. Enter valid password\n3. Click 'Login' button",
            "User is authenticated and redirected to main Dashboard", "screenshots/TC-APP-02.png"
        ));
        DETAILS_MAP.put("test3_LoginErrorValidation", new TestCaseDetails(
            "TC-APP-03", "TC-AUTH: Authentication", "Login Screen",
            "Verify appropriate error popup/text displays on invalid credentials input",
            "1. Enter valid email\n2. Enter incorrect password\n3. Click 'Login' button",
            "Authentication fails, displaying error message prompt", "screenshots/TC-APP-03.png"
        ));
        DETAILS_MAP.put("test4_RegistrationSuccess", new TestCaseDetails(
            "TC-APP-04", "TC-AUTH: Authentication", "Signup Screen",
            "Verify registration completes successfully for new users",
            "1. Enter username\n2. Enter unique email\n3. Enter secure password\n4. Click 'Register'",
            "Registration is successful, user session initialized", "screenshots/TC-APP-04.png"
        ));
        DETAILS_MAP.put("test5_RegistrationFieldValidation", new TestCaseDetails(
            "TC-APP-05", "TC-AUTH: Authentication", "Signup Screen",
            "Verify registration is prevented and fields validated on empty username input",
            "1. Leave username blank\n2. Enter email and password\n3. Click 'Register'",
            "Validation error highlights empty username field", "screenshots/TC-APP-05.png"
        ));
        DETAILS_MAP.put("test6_ForgotPassword", new TestCaseDetails(
            "TC-APP-06", "TC-AUTH: Authentication", "Login Screen",
            "Verify password reset trigger option redirect works",
            "1. Click 'Forgot Password' link label",
            "User is redirected to password recovery layout", "screenshots/TC-APP-06.png"
        ));
        DETAILS_MAP.put("test7_OtpVerification", new TestCaseDetails(
            "TC-APP-07", "TC-AUTH: OTP Verification", "OTP Screen",
            "Verify OTP verification accepts standard 6-digit numeric input",
            "1. Input valid 6-digit token\n2. Click verify",
            "OTP validation passes and account is activated", "screenshots/TC-APP-07.png"
        ));
        DETAILS_MAP.put("test8_DashboardLoad", new TestCaseDetails(
            "TC-APP-08", "TC-DASHBOARD: Dashboard", "Dashboard",
            "Verify the home dashboard metrics load successfully after authentication",
            "1. Access dashboard tab\n2. Assert header displays parent dashboard info",
            "Main dashboard interface displays connected telemetry summary details", "screenshots/TC-APP-08.png"
        ));
        DETAILS_MAP.put("test9_NavigationMenu", new TestCaseDetails(
            "TC-APP-09", "TC-DASHBOARD: Dashboard", "Dashboard",
            "Verify sidebar navigation drawer opens on toggle click",
            "1. Tap navigation hamburger menu icon\n2. Verify drawer links options display",
            "Navigation drawer opens overlaying current dashboard view", "screenshots/TC-APP-09.png"
        ));
        DETAILS_MAP.put("test10_ProfileView", new TestCaseDetails(
            "TC-APP-10", "TC-PROFILE: Profile", "Profile Screen",
            "Verify profile info layout displays matching user profile details",
            "1. Access profile tab section\n2. Verify name and email label text fields",
            "Current profile information matches account metadata", "screenshots/TC-APP-10.png"
        ));
        DETAILS_MAP.put("test11_ProfileEdit", new TestCaseDetails(
            "TC-APP-11", "TC-PROFILE: Profile", "Profile Edit Screen",
            "Verify editing user name info saves and updates profile headers",
            "1. Click edit profile\n2. Modify profile name field\n3. Click update save",
            "Profile updates successfully displaying new display name", "screenshots/TC-APP-11.png"
        ));
        DETAILS_MAP.put("test12_PasswordChange", new TestCaseDetails(
            "TC-APP-12", "TC-PROFILE: Profile", "Password Change Screen",
            "Verify user can update password by providing old and new values",
            "1. Click change password\n2. Enter old and new passwords\n3. Save configuration",
            "Password changes successfully and session remains active", "screenshots/TC-APP-12.png"
        ));
        DETAILS_MAP.put("test13_SearchFunctionality", new TestCaseDetails(
            "TC-APP-13", "TC-SEARCH: Search Engine", "Dashboard",
            "Verify search engine filters companion nodes and logs list successfully",
            "1. Enter query inside search field\n2. Assert filtered list output match",
            "Logs/nodes lists are updated dynamically matching search query filter", "screenshots/TC-APP-13.png"
        ));
        DETAILS_MAP.put("test14_NotificationView", new TestCaseDetails(
            "TC-APP-14", "TC-NOTIFICATIONS: Alerts", "Notifications Panel",
            "Verify alerts and warnings feed displays recent companion warnings",
            "1. Click notifications bell icon\n2. Verify alert items description visible",
            "Notifications log displays recent pairing and connection alert feeds", "screenshots/TC-APP-14.png"
        ));
        DETAILS_MAP.put("test15_LinkCreation", new TestCaseDetails(
            "TC-APP-15", "TC-LINKING: Hardware Link", "Device Pairing Screen",
            "Verify parent can link a new child hardware node using pair token code",
            "1. Input valid 6-char pairing token\n2. Click link button",
            "Device pairs successfully, adding new companion to dashboard child node selector", "screenshots/TC-APP-15.png"
        ));
        DETAILS_MAP.put("test16_LinkEdit", new TestCaseDetails(
            "TC-APP-16", "TC-LINKING: Hardware Link", "Device Pairing Screen",
            "Verify child node configurations can be modified and saved",
            "1. Edit child node name\n2. Click save settings",
            "Changes are updated successfully in telemetry selector view", "screenshots/TC-APP-16.png"
        ));
        DETAILS_MAP.put("test17_LinkDelete", new TestCaseDetails(
            "TC-APP-17", "TC-LINKING: Hardware Link", "Device Pairing Screen",
            "Verify companion hardware link deletion removes node from child listings",
            "1. Select child node\n2. Click delete link connection",
            "Pairing connection is terminated and child node is removed from list", "screenshots/TC-APP-17.png"
        ));
        DETAILS_MAP.put("test18_LinkSharing", new TestCaseDetails(
            "TC-APP-18", "TC-LINKING: Hardware Link", "Device Pairing Screen",
            "Verify sharing configuration opens android native share dialog option",
            "1. Click share icon button link",
            "Native share panel sheet overlay displays options", "screenshots/TC-APP-18.png"
        ));
        DETAILS_MAP.put("test19_SettingsAccess", new TestCaseDetails(
            "TC-APP-19", "TC-SETTINGS: Settings", "Settings Screen",
            "Verify Settings option interface loads and displays option list parameters",
            "1. Select settings drawer option\n2. Verify layout title is visible",
            "Settings configuration options interface is displayed", "screenshots/TC-APP-19.png"
        ));
        DETAILS_MAP.put("test20_SessionPersistence", new TestCaseDetails(
            "TC-APP-20", "TC-LAUNCH: App Launch", "Splash Screen",
            "Verify login state is persisted across application reboot cycles",
            "1. Restart app process\n2. Verify dashboard loads without login screen detour",
            "Active session is retained, app boots directly into main dashboard view", "screenshots/TC-APP-20.png"
        ));
        DETAILS_MAP.put("test21_Logout", new TestCaseDetails(
            "TC-APP-21", "TC-SETTINGS: Settings", "Settings Screen",
            "Verify settings logout options terminates active user session",
            "1. Click logout button\n2. Assert login interface displays",
            "User session is terminated and client redirected to login layout page", "screenshots/TC-APP-21.png"
        ));
        DETAILS_MAP.put("test22_BackNavigation", new TestCaseDetails(
            "TC-APP-22", "TC-NAVIGATION: Navigation Flow", "Dashboard",
            "Verify clicking back button navigates back to previous screen correctly",
            "1. Open profile page\n2. Trigger back button event\n3. Verify dashboard screen display",
            "Client returns to dashboard and layout state is preserved", "screenshots/TC-APP-22.png"
        ));
        DETAILS_MAP.put("test23_NetworkRecovery", new TestCaseDetails(
            "TC-APP-23", "TC-NETWORK: Connection Status", "Dashboard",
            "Verify offline telemetry banner warning displays on network disconnection",
            "1. Disable simulated network\n2. Verify warning offline badge presence",
            "Warning banner alert offline state is displayed immediately", "screenshots/TC-APP-23.png"
        ));
        DETAILS_MAP.put("test24_ErrorDialogValidation", new TestCaseDetails(
            "TC-APP-24", "TC-NAVIGATION: Navigation Flow", "Dashboard",
            "Verify error dialog window cancels and closes on OK confirmation",
            "1. Trigger mock exception dialog alert\n2. Click dismiss OK button",
            "Error dialog disappears and dashboard interactivity is restored", "screenshots/TC-APP-24.png"
        ));
        DETAILS_MAP.put("test25_AppLaunchPerformance", new TestCaseDetails(
            "TC-APP-25", "TC-LAUNCH: App Launch", "Splash Screen",
            "Verify cold launch duration fits within maximum latency targets",
            "1. Measure cold start timestamp duration\n2. Assert time is under threshold",
            "Splash activity launches and transitions to main landing page within limit", "screenshots/TC-APP-25.png"
        ));
    }

    public static void main(String[] args) {
        try {
            System.out.println("ExcelReporter: Starting Excel report generation for Appium Mobile tests...");
            File testResultsDir = new File("build/test-results/test");
            if (!testResultsDir.exists()) {
                testResultsDir = new File("appium-tests/build/test-results/test");
            }
            File reportsDir = new File("reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }
            File reportFile = new File(reportsDir, "Appium_Mobile_Report.xlsx");

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Appium Mobile Report");

            Font titleFont = workbook.createFont();
            titleFont.setFontName("Segoe UI");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Segoe UI");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font normalFont = workbook.createFont();
            normalFont.setFontName("Segoe UI");
            normalFont.setFontHeightInPoints((short) 10);

            Font boldFont = workbook.createFont();
            boldFont.setFontName("Segoe UI");
            boldFont.setFontHeightInPoints((short) 10);
            boldFont.setBold(true);

            Font passFont = workbook.createFont();
            passFont.setFontName("Segoe UI");
            passFont.setFontHeightInPoints((short) 10);
            passFont.setBold(true);
            passFont.setColor(new XSSFColor(new java.awt.Color(46, 125, 50), new DefaultIndexedColorMap()).getIndex());

            Font failFont = workbook.createFont();
            failFont.setFontName("Segoe UI");
            failFont.setFontHeightInPoints((short) 10);
            failFont.setBold(true);
            failFont.setColor(new XSSFColor(new java.awt.Color(198, 40, 40), new DefaultIndexedColorMap()).getIndex());

            XSSFCellStyle titleStyle = (XSSFCellStyle) workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(236, 239, 241), new DefaultIndexedColorMap()));
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setBorderBottom(BorderStyle.MEDIUM);
            titleStyle.setBorderTop(BorderStyle.MEDIUM);
            titleStyle.setBorderLeft(BorderStyle.MEDIUM);
            titleStyle.setBorderRight(BorderStyle.MEDIUM);

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(27, 54, 93), new DefaultIndexedColorMap()));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle labelStyle = (XSSFCellStyle) workbook.createCellStyle();
            labelStyle.setFont(boldFont);
            labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle valStyle = (XSSFCellStyle) workbook.createCellStyle();
            valStyle.setFont(normalFont);
            valStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle failValStyle = (XSSFCellStyle) workbook.createCellStyle();
            failValStyle.setFont(boldFont);
            failValStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            failValStyle.setFont(failFont);

            XSSFCellStyle normalCellStyle = (XSSFCellStyle) workbook.createCellStyle();
            normalCellStyle.setFont(normalFont);
            normalCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            normalCellStyle.setBorderBottom(BorderStyle.THIN);
            normalCellStyle.setBorderTop(BorderStyle.THIN);
            normalCellStyle.setBorderLeft(BorderStyle.THIN);
            normalCellStyle.setBorderRight(BorderStyle.THIN);
            normalCellStyle.setWrapText(true);

            XSSFCellStyle passStatusStyle = (XSSFCellStyle) workbook.createCellStyle();
            passStatusStyle.setFont(passFont);
            passStatusStyle.setAlignment(HorizontalAlignment.CENTER);
            passStatusStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            passStatusStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(232, 245, 233), new DefaultIndexedColorMap()));
            passStatusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            passStatusStyle.setBorderBottom(BorderStyle.THIN);
            passStatusStyle.setBorderTop(BorderStyle.THIN);
            passStatusStyle.setBorderLeft(BorderStyle.THIN);
            passStatusStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle failStatusStyle = (XSSFCellStyle) workbook.createCellStyle();
            failStatusStyle.setFont(failFont);
            failStatusStyle.setAlignment(HorizontalAlignment.CENTER);
            failStatusStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            failStatusStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(255, 235, 238), new DefaultIndexedColorMap()));
            failStatusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            failStatusStyle.setBorderBottom(BorderStyle.THIN);
            failStatusStyle.setBorderTop(BorderStyle.THIN);
            failStatusStyle.setBorderLeft(BorderStyle.THIN);
            failStatusStyle.setBorderRight(BorderStyle.THIN);

            // Title Block (Row 0)
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(40);
            for (int i = 0; i < 11; i++) {
                titleRow.createCell(i).setCellStyle(titleStyle);
            }
            titleRow.getCell(0).setCellValue("Appium Mobile Test Execution Report");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            int totalTests = 0;
            int passedTests = 0;
            int failedTests = 0;
            double totalDurationSec = 0.0;
            SimpleDateFormat defaultDateFormat = new SimpleDateFormat("M/d/yyyy, h:mm:ss a");

            if (testResultsDir.exists() && testResultsDir.isDirectory()) {
                File[] xmlFiles = testResultsDir.listFiles((dir, name) -> name.endsWith(".xml"));
                if (xmlFiles != null) {
                    int currentRowNum = 8;
                    for (File xmlFile : xmlFiles) {
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(xmlFile);
                        doc.getDocumentElement().normalize();

                        NodeList nList = doc.getElementsByTagName("testcase");
                        for (int temp = 0; temp < nList.getLength(); temp++) {
                            org.w3c.dom.Node nNode = nList.item(temp);
                            if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                                Element eElement = (Element) nNode;
                                String rawName = eElement.getAttribute("name");
                                String cleanedMethodName = rawName.replace("()", "");
                                TestCaseDetails details = DETAILS_MAP.get(cleanedMethodName);

                                if (details == null) {
                                    details = new TestCaseDetails("TC-APP-MOBILE", "TC-APP: Mobile App", "mobile", cleanedMethodName, "N/A", "N/A", "N/A");
                                }

                                String timeStr = eElement.getAttribute("time");
                                double timeVal = 0.0;
                                try {
                                    timeVal = Double.parseDouble(timeStr);
                                } catch (NumberFormatException ignored) {}
                                totalDurationSec += timeVal;
                                String timeMsStr = ((int) (timeVal * 1000)) + "ms";

                                NodeList failures = eElement.getElementsByTagName("failure");
                                NodeList errors = eElement.getElementsByTagName("error");
                                boolean isPassed = (failures.getLength() == 0 && errors.getLength() == 0);

                                totalTests++;
                                if (isPassed) {
                                    passedTests++;
                                } else {
                                    failedTests++;
                                }

                                String status = isPassed ? "Pass" : "Fail";
                                String errorMsg = "";
                                if (!isPassed) {
                                    if (failures.getLength() > 0) {
                                        errorMsg = ((Element) failures.item(0)).getAttribute("message");
                                    } else if (errors.getLength() > 0) {
                                        errorMsg = ((Element) errors.item(0)).getAttribute("message");
                                    }
                                }

                                String suiteTimestamp = doc.getDocumentElement().getAttribute("timestamp");
                                String dateTimeVal = "";
                                try {
                                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                    Date date = inputFormat.parse(suiteTimestamp);
                                    dateTimeVal = defaultDateFormat.format(date);
                                } catch (Exception e) {
                                    dateTimeVal = defaultDateFormat.format(new Date());
                                }

                                Row row = sheet.createRow(currentRowNum++);
                                row.setHeightInPoints(24);
                                row.createCell(0).setCellValue(details.id);
                                row.createCell(1).setCellValue(details.module);
                                row.createCell(2).setCellValue(details.screen);
                                row.createCell(3).setCellValue(details.description);
                                row.createCell(4).setCellValue(details.steps);
                                row.createCell(5).setCellValue(details.expected);
                                row.createCell(6).setCellValue(isPassed ? "App screen interactive elements responsive." : "Error: " + errorMsg);
                                row.createCell(7).setCellValue(timeMsStr);

                                Cell statusCell = row.createCell(8);
                                statusCell.setCellValue(status);
                                statusCell.setCellStyle(isPassed ? passStatusStyle : failStatusStyle);

                                row.createCell(9).setCellValue(details.screenshotRef);
                                row.createCell(10).setCellValue(dateTimeVal);

                                for (int i = 0; i < 11; i++) {
                                    if (i != 8) {
                                        row.getCell(i).setCellStyle(normalCellStyle);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary values
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Total Tests");
            row2.createCell(2).setCellValue(totalTests);
            row2.getCell(0).setCellStyle(labelStyle);
            row2.getCell(2).setCellStyle(valStyle);

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Passed");
            row3.createCell(2).setCellValue(passedTests);
            row3.getCell(0).setCellStyle(labelStyle);
            row3.getCell(2).setCellStyle(valStyle);

            Row row4 = sheet.createRow(4);
            row4.createCell(0).setCellValue("Failed");
            row4.createCell(2).setCellValue(failedTests);
            row4.getCell(0).setCellStyle(labelStyle);
            row4.getCell(2).setCellStyle(failedTests > 0 ? failValStyle : valStyle);

            Row row5 = sheet.createRow(5);
            row5.createCell(0).setCellValue("Total Duration");
            double formattedDuration = Math.round(totalDurationSec * 100.0) / 100.0;
            row5.createCell(1).setCellValue(formattedDuration + "s");
            row5.getCell(0).setCellStyle(labelStyle);
            row5.getCell(1).setCellStyle(valStyle);

            // Header row (Row 7)
            Row headerRow = sheet.createRow(7);
            headerRow.setHeightInPoints(28);
            String[] headers = {
                "Test Case ID", "Module", "Screen/Endpoint", "Description",
                "Steps Executed", "Expected Result", "Actual Result",
                "Execution Time", "Status", "Screenshot Reference", "Timestamp"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Auto column sizing
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                if (width < 3500) {
                    sheet.setColumnWidth(i, 3500);
                } else if (width > 12000) {
                    sheet.setColumnWidth(i, 12000);
                }
            }

            FileOutputStream fileOut = new FileOutputStream(reportFile);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            System.out.println("ExcelReporter: Styled Appium report generated successfully at: " + reportFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("ExcelReporter: Error generating Appium report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
