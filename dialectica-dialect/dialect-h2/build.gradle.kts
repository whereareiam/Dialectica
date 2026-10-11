plugins { id("library") }

dependencies {
    api(project(":dialectica-common"))

    testImplementation(testFixtures(project(":dialectica-common")))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-dialect-h2"
            pom {
                name.set("dialectica-dialect-h2")
                description.set("H2 dialect for Dialectica")
            }
        }
    }
}
