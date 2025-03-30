plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.nexusPublish)
    alias(libs.plugins.dokka)
}

repositories {
    mavenCentral()
}

fun getRepositoryUsername(): String =
    findProperty("OSSRH_USERNAME")?.toString() ?: System.getenv("OSSRH_USERNAME") ?: ""

fun getRepositoryPassword(): String =
    findProperty("OSSRH_PASSWORD")?.toString() ?: System.getenv("OSSRH_PASSWORD") ?: ""

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            username.set(getRepositoryUsername())
            password.set(getRepositoryPassword())
        }
    }
}

allprojects {
    val poketVersion: String by project

    group = "com.github.andresviedma"
    version = poketVersion
}
