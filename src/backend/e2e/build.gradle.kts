plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    testImplementation(libs.vertx.web.client)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
}

tasks.test {
    useJUnitPlatform()
}


