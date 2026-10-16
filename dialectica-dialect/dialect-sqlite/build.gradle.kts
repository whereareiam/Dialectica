plugins { id("library") }

dependencies {
    api(project(":dialectica-common"))

    // The driver comes with the project that uses SQLite; the dialect reads its extended result codes.
    compileOnly(rootProject.libs.sqlite)

    testImplementation(testFixtures(project(":dialectica-common")))
    testImplementation(rootProject.libs.sqlite)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-dialect-sqlite"
            pom {
                name.set("dialectica-dialect-sqlite")
                description.set("SQLite dialect for Dialectica")
            }
        }
    }
}
