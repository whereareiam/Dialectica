plugins { id("library") }

dependencies {
    api(project(":dialectica-common"))

    compileOnly(rootProject.libs.jdbi.core)
    compileOnly(rootProject.libs.jdbi.sqlobject)

    testImplementation(project(":dialectica-bundle"))
    testImplementation(testFixtures(project(":dialectica-bundle")))
    testImplementation(rootProject.libs.h2)
    testImplementation(rootProject.libs.jdbi.core)
    testImplementation(rootProject.libs.jdbi.sqlobject)
    testImplementation(rootProject.libs.mariadb)
    testImplementation(rootProject.libs.mysql)
    testImplementation(rootProject.libs.postgresql)
    testImplementation(rootProject.libs.sqlite)
    testImplementation(rootProject.libs.bundles.testing)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "dialectica-adapter-jdbi"
            pom {
                name.set("dialectica-adapter-jdbi")
                description.set("Jdbi access to Dialectica entities")
            }
        }
    }
}
