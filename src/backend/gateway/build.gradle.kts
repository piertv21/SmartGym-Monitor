plugins {
    alias(libs.plugins.spring.boot)
}

springBoot {
    mainClass.set("com.smartgym.gateway.GatewayApp")
}

dependencies {
    implementation(libs.spring.boot.starter.webflux)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.cloud.eureka.client)
}