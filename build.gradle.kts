plugins {
    checkstyle
}

allprojects {
    apply(plugin = "checkstyle")

    configure<org.gradle.api.plugins.quality.CheckstyleExtension> {
        toolVersion = "13.2.0"
        configFile = rootProject.file("config/checkstyle/naver-checkstyle-rules.xml")
        configProperties =
            mapOf("suppressionFile" to rootProject.file("config/checkstyle/naver-checkstyle-suppressions.xml"))
        isIgnoreFailures = true
        maxWarnings = 0
    }

    tasks.withType<Checkstyle> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
