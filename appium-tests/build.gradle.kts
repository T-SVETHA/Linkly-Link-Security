plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation("io.appium:java-client:9.2.2")
    testImplementation("org.seleniumhq.selenium:selenium-java:4.21.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Apache POI for Excel reports
    testImplementation("org.apache.poi:poi-ooxml:5.2.5")
}

tasks.test {
    useJUnitPlatform()
    reports.html.required.set(true)
    reports.junitXml.required.set(true)
    finalizedBy("generateExcelReport")
}

tasks.register<JavaExec>("generateExcelReport") {
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.linkly.appium.reporter.ExcelReporter")
}
