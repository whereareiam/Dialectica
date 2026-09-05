plugins {
    `java-library`
    `maven-publish`
}
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
group = "me.whereareiam"
version = providers.environmentVariable("VERSION").orElse("dev").get()
repositories {
    mavenCentral()
    maven("https://registry.whereareiam.me/maven/packages")
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
dependencies {
    "compileOnly"(libs.findLibrary("jetbrains-annotations").get())
    "compileOnly"(libs.findLibrary("lombok").get())
    "compileOnly"(libs.findLibrary("jdbi-core").get())
    "compileOnly"(libs.findLibrary("jdbi-sqlobject").get())
    "annotationProcessor"(libs.findLibrary("lombok").get())
    "testImplementation"(libs.findLibrary("junit-jupiter").get())
    "testCompileOnly"(libs.findLibrary("jetbrains-annotations").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform").get())
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (!providers.gradleProperty("integrationTests").map(String::toBoolean).getOrElse(false))
            excludeTags("database-container")
    }
}
publishing {
    repositories {
        maven {
            val base = providers.environmentVariable("PUBLISH_MAVEN_BASE_URL").orElse("https://registry.whereareiam.me/maven").get()
            val repository = providers.environmentVariable("PUBLISH_MAVEN_REPOSITORY").orElse("packages").get()
            url = uri("$base/$repository")
            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull
            }
        }
    }
}
