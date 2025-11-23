dependencies {
    api(project(":dialectica-api"))
    implementation(rootProject.libs.classgraph)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-common"
            pom {
                name.set("dialectica-common")
                description.set("Common implementation for Dialectica")
            }
        }
    }
}

