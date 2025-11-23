dependencies {
    api(project(":dialectica-api"))
    implementation(project(":dialectica-common"))

    // Test dependencies
    testImplementation(rootProject.libs.jdbi.core)
    testImplementation(rootProject.libs.jdbi.sqlobject)
    testImplementation(rootProject.libs.hikaricp)
    testImplementation(rootProject.libs.postgresql)
    testImplementation(rootProject.libs.mariadb)
    testImplementation(rootProject.libs.bundles.testing)
    testRuntimeOnly(rootProject.libs.junit.platform)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica"
            pom {
                name.set("dialectica")
                description.set("Aggregator module for Dialectica")
            }
        }
    }
}

