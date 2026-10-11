plugins { id("library") }

dependencies {
    api(project(":dialectica-dialect:dialect-mysql"))

    testImplementation(testFixtures(project(":dialectica-common")))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-dialect-mariadb"
            pom {
                name.set("dialectica-dialect-mariadb")
                description.set("MariaDB dialect for Dialectica")
            }
        }
    }
}
