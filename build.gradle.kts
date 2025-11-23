allprojects {
    version = (System.getenv("VERSION") ?: "dev")
    group = "me.whereareiam"

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.whereareiam.me/release")
        maven("https://maven.whereareiam.me/development")
    }

    dependencies {
        // lombok
        "compileOnly"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)

        // general
        "compileOnly"(rootProject.libs.jetbrains.annotations)
        "compileOnly"(rootProject.libs.jdbi.core)
        "compileOnly"(rootProject.libs.jdbi.sqlobject)
    }

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                val realm = (System.getenv("PUBLISH_REALM")
                    ?: if ((System.getenv("VERSION") ?: "dev").contains("dev", true)) "development" else "release")
                    .lowercase()
                url = uri("https://maven.whereareiam.me/$realm")
                credentials {
                    username = System.getenv("PUBLISH_USER") ?: ""
                    password = System.getenv("PUBLISH_TOKEN") ?: ""
                }
            }
        }
    }
}