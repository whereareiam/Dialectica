plugins { id("library") }

dependencies {
    api(project(":dialectica-common"))

    testImplementation(testFixtures(project(":dialectica-common")))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-dialect-mysql"
            pom {
                name.set("dialectica-dialect-mysql")
                description.set("MySQL dialect for Dialectica")
            }
        }
    }
}
