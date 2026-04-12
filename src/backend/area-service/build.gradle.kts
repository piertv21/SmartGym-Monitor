plugins {
    alias(libs.plugins.spring.boot)
}

springBoot {
    mainClass.set("com.smartgym.areaservice.AreaServiceApp")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.mongodb)


    implementation(libs.spring.cloud.eureka.client)
}