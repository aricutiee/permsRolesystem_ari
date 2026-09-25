plugins {
    java
    `java-library`
    id("com.gradleup.shadow") version "8.3.6"
}

group = "dev.turtleroles"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val paperVersion = "1.21.11-R0.1-SNAPSHOT"
val sqliteVersion = "3.50.3.0"
val snakeyamlVersion = "2.4"
val junitVersion = "5.13.4"

sourceSets {
    create("assetGenerator") {
        java.srcDir("src/assetGenerator/java")
    }
}

configurations {
    named("assetGeneratorImplementation") {
        extendsFrom(configurations.implementation.get())
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("org.yaml:snakeyaml:$snakeyamlVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    testImplementation("org.yaml:snakeyaml:$snakeyamlVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

val generatedPackDir = layout.buildDirectory.dir("generated/turtleroles/resource-pack")
val generatedBadgeDir = layout.buildDirectory.dir("generated/turtleroles/badges")
val generatedPreviewDir = layout.buildDirectory.dir("generated/turtleroles/previews")
val generatedZip = layout.buildDirectory.file("distributions/TurtleRoles-resource-pack.zip")
val generatedSha1 = layout.buildDirectory.file("distributions/TurtleRoles-resource-pack.sha1")

val generateRoleAssets by tasks.registering(JavaExec::class) {
    group = "turtleroles"
    description = "Generates transparent role badge PNGs, previews, font JSON and the resource-pack ZIP."
    classpath = sourceSets["assetGenerator"].runtimeClasspath
    mainClass.set("dev.turtleroles.assets.BadgeAssetGenerator")
    args(
        generatedPackDir.get().asFile.absolutePath,
        generatedBadgeDir.get().asFile.absolutePath,
        generatedPreviewDir.get().asFile.absolutePath,
        generatedZip.get().asFile.absolutePath,
        generatedSha1.get().asFile.absolutePath,
        file("design/packs/shockSMPpack-fixed.zip").absolutePath,
        file("design/reference/shock-smp-logo.png").absolutePath,
        file("design/reference/tab-shocks").absolutePath,
        file("design/reference/shock-smp-wordmark.png").absolutePath
    )
    inputs.files(fileTree("design/reference"))
    inputs.file("design/packs/shockSMPpack-fixed.zip")
    inputs.file("design/reference/shock-smp-logo.png")
    outputs.dir(generatedPackDir)
    outputs.dir(generatedBadgeDir)
    outputs.dir(generatedPreviewDir)
    outputs.file(generatedZip)
    outputs.file(generatedSha1)
}

tasks.processResources {
    dependsOn(generateRoleAssets)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
    from(generatedPackDir) {
        into("generated-resource-pack")
    }
    from(generatedZip) {
        into("generated-resource-pack")
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.sqlite", "dev.turtleroles.libs.sqlite")
    relocate("org.yaml.snakeyaml", "dev.turtleroles.libs.snakeyaml")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    dependsOn(generateRoleAssets)
    useJUnitPlatform()
    systemProperty("turtleroles.generatedPackDir", generatedPackDir.get().asFile.absolutePath)
    systemProperty("turtleroles.generatedBadgeDir", generatedBadgeDir.get().asFile.absolutePath)
    systemProperty("turtleroles.generatedZip", generatedZip.get().asFile.absolutePath)
}

tasks.register("printArtifacts") {
    dependsOn(tasks.build)
    doLast {
        println("Plugin JAR: ${tasks.shadowJar.get().archiveFile.get().asFile.absolutePath}")
        println("Resource pack ZIP: ${generatedZip.get().asFile.absolutePath}")
        println("Resource pack SHA-1: ${generatedSha1.get().asFile.readText().trim()}")
        println("Badge PNGs: ${generatedBadgeDir.get().asFile.absolutePath}")
        println("Preview PNGs: ${generatedPreviewDir.get().asFile.absolutePath}")
    }
}
