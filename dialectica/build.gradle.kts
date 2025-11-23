dependencies {
    api(project(":dialectica-api"))
    implementation(project(":dialectica-common"))
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

