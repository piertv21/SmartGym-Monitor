plugins {
    alias(libs.plugins.spring.boot)
}

springBoot {
    mainClass.set("com.smartgym.discovery.ServiceDiscoveryApp")
}

dependencies {
    implementation(libs.spring.cloud.eureka.server)
}