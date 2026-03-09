plugins {
    alias(libs.plugins.spring.boot)
}

springBoot {
    mainClass.set("com.smartgym.authservice.AuthServiceApplication")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.data.mongodb)

    implementation(libs.bundles.vertx.base)

    implementation(libs.spring.cloud.eureka.client)
}