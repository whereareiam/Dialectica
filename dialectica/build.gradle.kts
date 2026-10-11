plugins { id("library") }

dependencies {
    api(project(":dialectica-common"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica"
            pom {
                name.set("dialectica")
                description.set("Entry point of Dialectica; add the dialect modules of your databases")
            }
        }
    }
}
