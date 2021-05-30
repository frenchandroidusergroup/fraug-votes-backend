plugins {
  kotlin("jvm").version("1.5.10")
  kotlin("plugin.spring").version("1.5.10")
  id("org.springframework.boot").version("2.4.2")
  id("com.bmuschko.docker-spring-boot-application") version "7.0.0"
}

dependencies {
  implementation("com.expediagroup:graphql-kotlin-spring-server:4.1.1")
  implementation("org.mindrot:jbcrypt:0.4")
  implementation("com.google.cloud:google-cloud-firestore:1.32.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.5.10")
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
  kotlinOptions {
    freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
  }
}

/**
 * Deploy the executable jar by default
 * See https://docs.spring.io/spring-boot/docs/2.5.0/gradle-plugin/reference/htmlsingle/#packaging-executable.and-plain-archives
 */
//tasks.getByName<Jar>("jar") {
//  enabled = false
//}

docker {
  springBootApplication {
    // openjdk:8-alpine is not enough because of a TLS thingie
    // See https://github.com/grpc/grpc-java/issues/5369#issuecomment-493463266
    baseImage.set("openjdk:8")
    ports.set(listOf(8080))
    jvmArgs.set(listOf("-Dspring.profiles.active=production", "-Xmx52m"))
  }
}