dependencies {
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
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

