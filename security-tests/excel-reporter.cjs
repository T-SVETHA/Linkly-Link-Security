const Mocha = require('mocha');
const ExcelJS = require('exceljs');
const path = require('path');
const fs = require('fs');

const {
  EVENT_RUN_END,
  EVENT_TEST_PASS,
  EVENT_TEST_FAIL
} = Mocha.Runner.constants;

class ExcelReporter {
  constructor(runner) {
    this.results = [];
    this.totalDuration = 0;
    this.passed = 0;
    this.failed = 0;

    runner.on(EVENT_TEST_PASS, (test) => {
      this.passed++;
      this.totalDuration += test.duration || 0;
      this.results.push(this.formatTestResult(test, 'Passed'));
    });

    runner.on(EVENT_TEST_FAIL, (test, err) => {
      this.failed++;
      this.totalDuration += test.duration || 0;
      this.results.push(this.formatTestResult(test, 'Failed'));
    });
  }

  done(failures, exit) {
    this.generateReport()
      .then(() => exit(failures))
      .catch((err) => {
        console.error(err);
        exit(failures);
      });
  }

  formatTestResult(test, status) {
    const meta = test.ctx && test.ctx.meta ? test.ctx.meta : {};
    return {
      testCaseId: meta.testCaseId || 'SEC-000',
      module: meta.module || 'General',
      screenName: meta.screenName || 'DashboardPage',
      securityCategory: meta.securityCategory || 'Security',
      scenarioName: meta.scenarioName || test.title,
      expectedResult: meta.expectedResult || 'Expected behavior confirmed',
      status: status,
      executionTime: test.duration || 0
    };
  }

  async generateReport() {
    const workbook = new ExcelJS.Workbook();
    const sheet = workbook.addWorksheet('Security Tests');

    // Add Summary
    sheet.addRow(['Test Execution Summary']);
    sheet.addRow(['Total Tests', this.results.length]);
    sheet.addRow(['Passed', this.passed]);
    sheet.addRow(['Failed', this.failed]);
    sheet.addRow(['Total Duration (ms)', this.totalDuration]);
    
    // TWO BLANK ROWS
    sheet.addRow([]);
    sheet.addRow([]);

    // Add Headers
    const headers = [
      'Test Case ID',
      'Module',
      'Screen Name',
      'Security Category',
      'Scenario Name',
      'Expected Result',
      'Status',
      'Execution Time'
    ];
    const headerRow = sheet.addRow(headers);

    // Format Headers
    headerRow.eachCell((cell) => {
      cell.font = { bold: true };
      cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true };
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FFE0E0E0' } // light gray fill
      };
      cell.border = {
        top: { style: 'thin' },
        left: { style: 'thin' },
        bottom: { style: 'thin' },
        right: { style: 'thin' }
      };
    });

    // Freeze header row
    sheet.views = [
      { state: 'frozen', xSplit: 0, ySplit: 8 }
    ];

    // Enable auto filter
    sheet.autoFilter = {
      from: 'A8',
      to: 'H8'
    };

    // Add Data
    this.results.forEach(res => {
      const row = sheet.addRow([
        res.testCaseId,
        res.module,
        res.screenName,
        res.securityCategory,
        res.scenarioName,
        res.expectedResult,
        res.status,
        res.executionTime
      ]);
      row.eachCell((cell) => {
        cell.alignment = { wrapText: true, vertical: 'top' };
        cell.border = {
          top: { style: 'thin' },
          left: { style: 'thin' },
          bottom: { style: 'thin' },
          right: { style: 'thin' }
        };
      });
    });

    // Auto-fit ALL column widths
    sheet.columns.forEach((column, i) => {
      let maxLength = 0;
      column.eachCell({ includeEmpty: true }, (cell) => {
        const columnLength = cell.value ? cell.value.toString().length : 10;
        if (columnLength > maxLength) {
          maxLength = columnLength;
        }
      });
      // Set width with a minimum of 15 and max of 50 to prevent crazy wide columns
      column.width = Math.min(Math.max(maxLength + 2, 15), 50);
    });

    const reportDir = path.join(process.cwd(), 'reports');
    if (!fs.existsSync(reportDir)) {
      fs.mkdirSync(reportDir, { recursive: true });
    }

    await workbook.xlsx.writeFile(path.join(reportDir, 'SecurityTestReport.xlsx'));
    console.log('Security report generated at reports/SecurityTestReport.xlsx');
  }
}

module.exports = ExcelReporter;
