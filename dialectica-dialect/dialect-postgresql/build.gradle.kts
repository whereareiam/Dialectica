plugins { id("library") }

dependencies {
    api(project(":dialectica-common"))

    testImplementation(testFixtures(project(":dialectica-common")))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-dialect-postgresql"
            pom {
                name.set("dialectica-dialect-postgresql")
                description.set("PostgreSQL dialect for Dialectica")
            }
        }
    }
}
