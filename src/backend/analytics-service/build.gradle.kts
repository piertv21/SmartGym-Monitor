plugins {
    alias(libs.plugins.spring.boot)
}

springBoot {
    mainClass.set("com.smartgym.analyticsservice.AnalyticsServiceApp")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.mongodb)

    implementation(libs.vertx.core)

    implementation(libs.spring.cloud.eureka.client)
}