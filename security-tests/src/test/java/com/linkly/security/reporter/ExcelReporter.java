package com.linkly.security.reporter;

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
        DETAILS_MAP.put("test1_SqlInjectionProtection", new TestCaseDetails(
            "TC-SEC-01", "TC-INJ: Injection Controls", "/api/login",
            "Verify single quotes are correctly escaped in parameters to prevent SQL injection",
            "1. Send parameter: 1' OR '1'='1\n2. Verify database queries treat input as literal string",
            "Queries are escaped correctly; database ignores injection characters", "N/A"
        ));
        DETAILS_MAP.put("test2_XssProtection", new TestCaseDetails(
            "TC-SEC-02", "TC-XSS: Scripting Controls", "/api/dashboard",
            "Verify HTML entities are escaped before rendering to prevent XSS payloads",
            "1. Submit input: <script>alert('xss')</script>\n2. Assert output displays escaped brackets",
            "Script brackets are replaced with safe HTML entities", "N/A"
        ));
        DETAILS_MAP.put("test3_CsrfProtection", new TestCaseDetails(
            "TC-SEC-03", "TC-CSRF: Session Integrity", "/api/lockdown",
            "Verify state-changing POST requests block execution when CSRF token is empty",
            "1. Send POST request with empty token header\n2. Assert access is blocked",
            "Request is rejected immediately returning validation error", "N/A"
        ));
        DETAILS_MAP.put("test4_AuthenticationValidation", new TestCaseDetails(
            "TC-SEC-04", "TC-AUTH: Identity Access", "/api/telemetry",
            "Verify protected resources reject requests lacking a Bearer Auth token header",
            "1. Query telemetry endpoint with empty Auth header\n2. Assert HTTP response status is 401",
            "Access is denied, request blocked with status 401", "N/A"
        ));
        DETAILS_MAP.put("test5_AuthorizationValidation", new TestCaseDetails(
            "TC-SEC-05", "TC-AUTH: Privilege Levels", "/api/parent/config",
            "Verify child user account roles are denied access to parent dashboard APIs",
            "1. Authenticate as CHILD\n2. Send request to parent configuration API\n3. Assert status is 403",
            "Privilege escalation blocked; request rejected with status 403", "N/A"
        ));
        DETAILS_MAP.put("test6_JwtSignatureValidation", new TestCaseDetails(
            "TC-SEC-06", "TC-CRYPTO: Data Integrity", "/api/session",
            "Verify altered JWT token signatures are rejected on validation attempts",
            "1. Tamper signature bytes of active JWT token\n2. Attempt access using tampered token",
            "Authentication verification checks reject tampered signature token", "N/A"
        ));
        DETAILS_MAP.put("test7_SessionExpiration", new TestCaseDetails(
            "TC-SEC-07", "TC-SESSION: State Management", "/api/session/refresh",
            "Verify session manager times out and invalidates sessions inactive for 15+ minutes",
            "1. Simulate inactive state for 20 minutes\n2. Assert active session validation fails",
            "Inactivity leads to automatic session timeout and logout redirection", "N/A"
        ));
        DETAILS_MAP.put("test8_PasswordPolicyValidation", new TestCaseDetails(
            "TC-SEC-08", "TC-AUTH: Password Policies", "/api/register",
            "Verify password hashing utility rejects low-complexity passwords",
            "1. Attempt registration using password: 123\n2. Verify rejection response details",
            "Complexity check blocks policy-violating password submission", "N/A"
        ));
        DETAILS_MAP.put("test9_BruteForceProtection", new TestCaseDetails(
            "TC-SEC-09", "TC-AUTH: Identity Access", "/api/login",
            "Verify user login lockout is triggered after 5 consecutive auth failures",
            "1. Perform 6 consecutive incorrect password login attempts\n2. Assert account state locked",
            "Brute force lockout occurs, blocking subsequent attempts", "N/A"
        ));
        DETAILS_MAP.put("test10_RateLimiting", new TestCaseDetails(
            "TC-SEC-10", "TC-DOS: Rate Limiting", "/api/logs",
            "Verify API throttle limits trigger HTTP 429 when client limits exceed 100 requests",
            "1. Send 101 requests within one minute\n2. Verify rate limit response code",
            "Requests throttle, returning HTTP status code 429", "N/A"
        ));
        DETAILS_MAP.put("test11_InputValidation", new TestCaseDetails(
            "TC-SEC-11", "TC-VAL: Input Filtering", "/api/profile",
            "Verify field email format pattern validations reject invalid syntax forms",
            "1. Submit invalid email format: svetha@@linkly\n2. Assert validation feedback",
            "Input verification regex rejects malformed email syntax", "N/A"
        ));
        DETAILS_MAP.put("test12_ApiSchemaValidation", new TestCaseDetails(
            "TC-SEC-12", "TC-VAL: Input Filtering", "/api/register",
            "Verify API schema validator rejects body payload missing required parameters",
            "1. Submit register JSON missing email field\n2. Assert validation failure response",
            "Schema verification rejects incomplete payload schema", "N/A"
        ));
        DETAILS_MAP.put("test13_HttpsEnforcement", new TestCaseDetails(
            "TC-SEC-13", "TC-NETWORK: Transport", "/api/*",
            "Verify insecure HTTP request routes redirect automatically to secure HTTPS",
            "1. Send GET request to port 80 (HTTP)\n2. Assert redirect location is HTTPS port 443",
            "Secure channel redirects all HTTP traffic to HTTPS", "N/A"
        ));
        DETAILS_MAP.put("test14_SecureHeadersValidation", new TestCaseDetails(
            "TC-SEC-14", "TC-NETWORK: HTTP Headers", "/api/*",
            "Verify standard secure security headers are returned in HTTP responses",
            "1. Query API endpoint\n2. Verify presence of X-Frame-Options and X-Content-Type-Options",
            "Security headers present to deny clickjacking and restrict MIME sniffing", "N/A"
        ));
        DETAILS_MAP.put("test15_SensitiveDataMasking", new TestCaseDetails(
            "TC-SEC-15", "TC-DATA: Data Protection", "/api/logs",
            "Verify audit log writing filters out and masks sensitive variables",
            "1. Log request with password parameter\n2. Verify logged entry replaces password with mask",
            "Log audit trail masks sensitive data fields with placeholder characters", "N/A"
        ));
        DETAILS_MAP.put("test16_FileUploadValidation", new TestCaseDetails(
            "TC-SEC-16", "TC-VAL: File Controls", "/api/upload",
            "Verify file upload handlers restrict uploads of non-whitelisted extension forms",
            "1. Upload file named: shell.exe\n2. Assert upload handler returns rejection",
            "Upload rejected; only image formats (.jpg, .png) are permitted", "N/A"
        ));
        DETAILS_MAP.put("test17_PathTraversalProtection", new TestCaseDetails(
            "TC-SEC-17", "TC-VAL: Path Traversal", "/api/download",
            "Verify filename parameter check strips out directory traversal character patterns",
            "1. Request file download path: ../../etc/passwd\n2. Assert warning traversal blocked",
            "Directory traversal attempt is recognized and blocked by file utility", "N/A"
        ));
        DETAILS_MAP.put("test18_AccessControlVerification", new TestCaseDetails(
            "TC-SEC-18", "TC-CORS: Origin Controls", "/api/*",
            "Verify CORS headers block wildcard wildcard access on authenticated APIs",
            "1. Inspect Access-Control-Allow-Origin response header",
            "Wildcard origin * is rejected; only specific trusted domains allowed", "N/A"
        ));
        DETAILS_MAP.put("test19_TokenExpirationValidation", new TestCaseDetails(
            "TC-SEC-19", "TC-AUTH: Identity Access", "/api/profile",
            "Verify API rejects requests containing expired authorization Bearer tokens",
            "1. Supply expired Bearer JWT token in Authorization header\n2. Assert status is 401",
            "Expired authentication token is identified and blocked", "N/A"
        ));
        DETAILS_MAP.put("test20_ReplayAttackProtection", new TestCaseDetails(
            "TC-SEC-20", "TC-REPLAY: Nonce Validation", "/api/lockdown",
            "Verify duplicated nonce values inside timestamp window are blocked",
            "1. Submit request with nonce123456\n2. Submit identical request nonce123456\n3. Assert rejection",
            "Replay attack check rejects duplicates nonce configuration", "N/A"
        ));
        DETAILS_MAP.put("test21_ApiErrorHandling", new TestCaseDetails(
            "TC-SEC-21", "TC-LOGGING: Error Handling", "/api/*",
            "Verify internal server error responses withhold debug stack trace details",
            "1. Trigger internal server exception\n2. Assert error response body omits system stack traces",
            "API hides stack traces, displaying generic error descriptions to client", "N/A"
        ));
        DETAILS_MAP.put("test22_SecureCookieValidation", new TestCaseDetails(
            "TC-SEC-22", "TC-SESSION: Cookies", "/api/*",
            "Verify session cookies have Secure and HttpOnly flags configured",
            "1. Inspect Set-Cookie response header attributes",
            "Secure and HttpOnly flags are active on session cookies", "N/A"
        ));
        DETAILS_MAP.put("test23_DataExposureVerification", new TestCaseDetails(
            "TC-SEC-23", "TC-DATA: Data Protection", "/api/profile",
            "Verify API serialization configuration excludes internal passwords hash fields",
            "1. Query profile endpoint\n2. Assert response JSON excludes fields like passwordHash",
            "Database password hashes are excluded from serialised response", "N/A"
        ));
        DETAILS_MAP.put("test24_SecurityLoggingVerification", new TestCaseDetails(
            "TC-SEC-24", "TC-LOGGING: Audit Trail", "/api/login",
            "Verify authentication failure triggers write audit trail in log logs",
            "1. Submit incorrect password login\n2. Verify security logs record auth failure event",
            "Authentication failure event is logged to secure audit trails", "N/A"
        ));
        DETAILS_MAP.put("test25_EndpointEnumerationProtection", new TestCaseDetails(
            "TC-SEC-25", "TC-LOGGING: Endpoint Protection", "/api/missing",
            "Verify requesting invalid endpoints displays uniform error pages",
            "1. Query invalid URL path\n2. Compare output text with other invalid paths",
            "Error messages for missing endpoints are uniform, preventing enumeration", "N/A"
        ));
    }

    public static void main(String[] args) {
        try {
            System.out.println("ExcelReporter: Starting Excel report generation for Security vulnerability tests...");
            File currentDir = new File(".").getAbsoluteFile();
            if (currentDir.getName().equals(".")) {
                currentDir = currentDir.getParentFile();
            }
            File projectRoot = currentDir.getName().equals("security-tests") ? currentDir.getParentFile() : currentDir;
            File testResultsDir = new File(projectRoot, "security-tests/build/test-results/test");
            File reportsDir = new File(projectRoot, "security-tests/reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }
            File reportFile = new File(reportsDir, "Vulnerability_Report.xlsx");

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Vulnerability Report");

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
            titleRow.getCell(0).setCellValue("OWASP Security Control Verification Report");
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
                                    details = new TestCaseDetails("TC-SEC-VULN", "TC-SEC: Vulnerability", "/api/endpoint", cleanedMethodName, "N/A", "N/A", "N/A");
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
                                row.createCell(6).setCellValue(isPassed ? "Security control validated successfully." : "Control Failure: " + errorMsg);
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
            System.out.println("ExcelReporter: Styled Security report generated successfully at: " + reportFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("ExcelReporter: Error generating Security report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
