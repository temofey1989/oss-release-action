buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("io.github.gradle-nexus:publish-plugin:2.0.0-rc-1")
    }
}

// --- PROPERTIES --------------------------------------------------------- #

fun property(name: String, defaultValue: String = ""): String {
    val envName = name.replace(".", "_").uppercase()
    return System.getenv(envName) ?: project.findProperty(name)?.toString() ?: defaultValue
}

val description: String by lazy { property("project.description", project.description ?: "") }

val scmUrl: String by lazy { property("scm.url") }

val scmConnection: String by lazy { property("scm.connection") }

val scmDeveloperConnection: String by lazy { property("scm.developerConnection") }

val licenseName: String by lazy { property("license.name") }

val licenseUrl: String by lazy { property("license.url") }

val developerId: String by lazy { property("developer.id") }

val developerName: String by lazy { property("developer.name") }

val developerEmail: String by lazy { property("developer.email") }

// ------------------------------------------------------------------------ #

apply(plugin = "org.gradle.java-library")
//apply<org.gradle.api.plugins.JavaLibraryPlugin>()
apply(plugin = "org.gradle.maven-publish")
//apply<org.gradle.api.publish.maven.plugins.MavenPublishPlugin>()
apply(plugin = "org.gradle.signing")
//apply<org.gradle.plugins.signing.SigningPlugin>()
apply<io.github.gradlenexus.publishplugin.NexusPublishPlugin>()

configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("${project.group}:${project.name}")
                description.set(description)
                url.set(scmUrl)
                licenses {
                    license {
                        name.set(licenseName)
                        url.set(licenseUrl)
                    }
                }
                developers {
                    developer {
                        id.set(developerId)
                        name.set(developerName)
                        email.set(developerEmail)
                    }
                }
                scm {
                    url.set(scmUrl)
                    connection.set(scmConnection)
                    developerConnection.set(scmDeveloperConnection)
                }
            }
            suppressPomMetadataWarningsFor("runtimeElements")
        }
    }
}

configure<SigningExtension> {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

    val publishing = project.extensions["publishing"] as PublishingExtension
    sign(publishing.publications["maven"])
}

configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
    val sonatypeNexusUrl: String? by project
    val sonatypeSnapshotRepositoryUrl: String? by project
    val ossrhUsername: String? by project
    val ossrhPassword: String? by project
    transitionCheckOptions {
        maxRetries.set(100)
        delayBetween.set(java.time.Duration.ofSeconds(5))
    }
    repositories {
        sonatype {
            if (!sonatypeNexusUrl.isNullOrBlank()) {
                nexusUrl.set(uri(sonatypeNexusUrl))
            }
            if (!sonatypeSnapshotRepositoryUrl.isNullOrBlank()) {
                snapshotRepositoryUrl.set(uri(sonatypeSnapshotRepositoryUrl))
            }
            if (!ossrhUsername.isNullOrBlank()) {
                username.set(ossrhUsername)
            }
            if (!ossrhPassword.isNullOrBlank()) {
                password.set(ossrhPassword)
            }
        }
    }
}
