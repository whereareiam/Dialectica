plugins { id("library") }

dependencies {
    api(project(":dialectica-common"))

    testImplementation(testFixtures(project(":dialectica-common")))
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
