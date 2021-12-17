import de.undercouch.gradle.tasks.download.Download
import com.felipefzdz.gradle.shellcheck.Shellcheck

plugins {
    id("base")
    id("de.undercouch.download") version "4.1.1"
    id("com.felipefzdz.gradle.shellcheck") version "1.4.4"
}

repositories {
    mavenCentral()
}

version = "0.0.1-SNAPSHOT"

val mavenComponents by configurations.creating

dependencies {
    mavenComponents("com.gradle:capture-published-build-scan-maven-extension:1.0.0-SNAPSHOT")
    mavenComponents("com.gradle:gradle-enterprise-maven-extension:1.11.1")
    mavenComponents("com.gradle:common-custom-user-data-maven-extension:1.9")
}

val argbashVersion by extra("2.10.0")

shellcheck {
    additionalArguments = "-a -x"
    shellcheckVersion = "v0.7.2"
}

tasks.register<Download>("downloadArgbash") {
    group = "argbash"
    description = "Downloads Argbash."
    src("https://github.com/matejak/argbash/archive/refs/tags/${argbashVersion}.zip")
    dest(file("${buildDir}/argbash/argbash-${argbashVersion}.zip"))
    overwrite(false)
}

tasks.register<Copy>("unpackArgbash") {
    group = "argbash"
    description = "Unpacks the downloaded Argbash archive."
    from(zipTree(tasks.getByName("downloadArgbash").outputs.files.singleFile))
    into(layout.buildDirectory.dir("argbash"))
    dependsOn("downloadArgbash")
}

tasks.register<ApplyArgbash>("generateBashCliParsers") {
    group = "argbash"
    description = "Uses Argbash to generate Bash command line argument parsing code."

    scriptTemplates.set(fileTree("../scripts") {
        include("**/*-cli-parser.m4")
        exclude("gradle/.data/")
        exclude("maven/.data/")
    })

    supportingTemplates.set(fileTree("../scripts") {
        include("**/*.m4")
        exclude("gradle/.data/")
        exclude("maven/.data/")
    })

    argbashVersion.set(project.extra["argbashVersion"].toString())

    dependsOn("unpackArgbash")
}

tasks.register<Copy>("copyGradleScripts") {
    group = "build"
    description = "Copies the Gradle source and generated scripts to output directory."
    dependsOn(gradle.includedBuild("fetch-build-validation-data").task(":shadowJar"))
    dependsOn("generateBashCliParsers")

    from(layout.projectDirectory.dir("../scripts/gradle")) {
        exclude(".data/")
        filter { line: String -> line.replace("/../lib", "/lib").replace("<HEAD>","${project.version}") }
    }
    from(layout.projectDirectory.dir("../scripts")) {
        include("lib/**")
        exclude("maven")
        exclude("lib/cli-parsers")
        exclude("**/*.m4")
        filter { line: String -> line.replace("/../lib", "/lib").replace("<HEAD>","${project.version}") }
    }
    from(layout.buildDirectory.dir("generated/scripts/lib/cli-parsers/gradle")) {
        into("lib/")
    }
    from(gradle.includedBuild("fetch-build-validation-data").projectDir.resolve("build/libs/fetch-build-validation-data-1.0.0-SNAPSHOT-all.jar")) {
        into("lib/export-api-clients/")
    }
    into(layout.buildDirectory.dir("scripts/gradle"))
}

tasks.register<Copy>("copyMavenScripts") {
    group = "build"
    description = "Copies the Maven source and generated scripts to output directory."
    dependsOn(gradle.includedBuild("fetch-build-validation-data").task(":shadowJar"))
    dependsOn("generateBashCliParsers")

    from(layout.projectDirectory.dir("../scripts/maven")) {
        exclude(".data/")
        filter { line: String -> line.replace("/../lib", "/lib").replace("<HEAD>","${project.version}") }
    }
    from(layout.projectDirectory.dir("../scripts/")) {
        include("lib/**")
        exclude("gradle")
        exclude("lib/cli-parsers")
        exclude("**/*.m4")
        filter { line: String -> line.replace("/../lib", "/lib").replace("<HEAD>","${project.version}") }
    }
    from(layout.buildDirectory.dir("generated/scripts/lib/cli-parsers/maven")) {
        into("lib/")
    }
    from(gradle.includedBuild("fetch-build-validation-data").projectDir.resolve("build/libs/fetch-build-validation-data-1.0.0-SNAPSHOT-all.jar")) {
        into("lib/export-api-clients/")
    }
    from(mavenComponents) {
        into("lib/maven/")
    }
    into(layout.buildDirectory.dir("scripts/maven"))
}

tasks.register<Task>("copyScripts") {
    group = "build"
    description = "Copies source scripts and autogenerated scripts to output directory."
    dependsOn("copyGradleScripts")
    dependsOn("copyMavenScripts")
}

tasks.register<Zip>("assembleGradleScripts") {
    group = "build"
    description = "Packages the Gradle experiment scripts in a zip archive."
    archiveBaseName.set("gradle-enterprise-gradle-build-validation")
    archiveFileName.set("${archiveBaseName.get()}.zip")

    from(layout.buildDirectory.dir("scripts/gradle")) {
        exclude("**/.data")
    }
    into(archiveBaseName.get())
    dependsOn("generateBashCliParsers")
    dependsOn("copyGradleScripts")
}

tasks.register<Zip>("assembleMavenScripts") {
    group = "build"
    description = "Packages the Maven experiment scripts in a zip archive."
    archiveBaseName.set("gradle-enterprise-maven-build-validation")
    archiveFileName.set("${archiveBaseName.get()}.zip")

    from(layout.buildDirectory.dir("scripts/maven")) {
        exclude("**/.data")
    }
    into(archiveBaseName.get())
    dependsOn("generateBashCliParsers")
    dependsOn("copyMavenScripts")
}

tasks.named("assemble") {
    dependsOn("assembleGradleScripts")
    dependsOn("assembleMavenScripts")
}

tasks.register<Shellcheck>("shellcheckGradleScripts") {
    group = "verification"
    description = "Perform quality checks on Gradle build validation scripts using Shellcheck."
    sourceFiles = fileTree("${buildDir}/scripts/gradle") {
        include("**/*.sh")
        exclude("lib/")
    }
    workingDir = file("${buildDir}/scripts/gradle")

    reports {
        html.destination = file("${buildDir}/reports/shellcheck-gradle/shellcheck.html")
        xml.destination = file("${buildDir}/reports/shellcheck-gradle/shellcheck.xml")
        txt.destination = file("${buildDir}/reports/shellcheck-gradle/shellcheck.txt")
    }

    dependsOn("generateBashCliParsers")
    dependsOn("copyGradleScripts")
}

tasks.register<Shellcheck>("shellcheckMavenScripts") {
    group = "verification"
    description = "Perform quality checks on Maven build validation scripts using Shellcheck."
    sourceFiles = fileTree("${buildDir}/scripts/maven") {
        include("**/*.sh")
        exclude("lib/")
    }
    workingDir = file("${buildDir}/scripts/maven")

    reports {
        html.destination = file("${buildDir}/reports/shellcheck-maven/shellcheck.html")
        xml.destination = file("${buildDir}/reports/shellcheck-maven/shellcheck.xml")
        txt.destination = file("${buildDir}/reports/shellcheck-maven/shellcheck.txt")
    }

    dependsOn("generateBashCliParsers")
    dependsOn("copyMavenScripts")
}

tasks.named("check") {
    dependsOn("shellcheckGradleScripts")
    dependsOn("shellcheckMavenScripts")
}
