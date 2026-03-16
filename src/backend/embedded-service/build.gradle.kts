plugins {
    alias(libs.plugins.spring.boot)
}

springBoot {
    mainClass.set("com.smartgym.embeddedservice.EmbeddedServiceApp")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.mongodb)
    implementation(libs.spring.cloud.eureka.client)

    implementation(libs.bundles.vertx.base)
    implementation(libs.vertx.mqtt)

    testImplementation(libs.vertx.junit5)
}

