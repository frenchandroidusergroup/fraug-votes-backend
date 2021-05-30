plugins {
  kotlin("jvm").version("1.5.10")
  kotlin("plugin.spring").version("1.5.10")
  id("org.springframework.boot").version("2.5.0")
  id("com.google.cloud.tools.appengine").version("2.4.1")
}

dependencies {
  implementation("com.expediagroup:graphql-kotlin-spring-server:4.1.1")
  implementation("org.mindrot:jbcrypt:0.4")
  implementation("com.google.cloud:google-cloud-firestore:1.32.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.5.10")
}

appengine {
  deploy {
    projectId = "fraug-votes"
    version = "GCLOUD_CONFIG"
  }
}

