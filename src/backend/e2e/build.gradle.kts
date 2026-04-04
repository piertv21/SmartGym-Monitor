plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    testImplementation(libs.vertx.web.client)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.spring.boot.starter.data.mongodb)
}

tasks.test {
    useJUnitPlatform()
}


