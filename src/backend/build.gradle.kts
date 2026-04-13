import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.mgmt) apply false
    alias(libs.plugins.spotless) apply false
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.diffplug.spotless")

    group = "com.smartgym"
    version = "1.0.0"

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(
                JavaLanguageVersion.of(libsCatalog.findVersion("java").get().requiredVersion.toInt())
            )
        }
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat().aosp()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    extensions.configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libsCatalog.findVersion("spring-cloud").get().requiredVersion}")
        }
    }

    dependencies {
        add("implementation", libsCatalog.findBundle("spring-common").get())
        add("testImplementation", libsCatalog.findLibrary("spring-boot-starter-test").get())
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}