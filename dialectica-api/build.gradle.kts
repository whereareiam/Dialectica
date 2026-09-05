plugins { id("library") }

dependencies {
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
    compileOnly(libs.jetbrains.annotations)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-api"
            pom {
                name.set("dialectica-api")
                description.set("Public API for Dialectica")
            }
        }
    }
}
