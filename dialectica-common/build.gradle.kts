plugins {
    `java-test-fixtures`
    id("library")
}

dependencies {
    api(project(":dialectica-api"))

    compileOnly(rootProject.libs.jetbrains.annotations)

    testFixturesImplementation(rootProject.libs.junit.jupiter)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-common"
            pom {
                name.set("dialectica-common")
                description.set("What Dialectica's dialects share, JDBC inspection and the entity reader")
            }
        }
    }
}

// Test fixtures are shared by the dialect modules' tests and are not published.
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
