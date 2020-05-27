import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay.PROMPT
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2019.2"

project {
    buildType {
        name = "Verify"
        id = RelativeId(name.toId())
        vcs {
            root(DslContext.settingsRootId)
            cleanCheckout = true
        }
        triggers {
            vcs {
                branchFilter = "+:<default>"
            }
        }
        steps {
            maven("verify")
        }
    }
    buildType {
        name = "Deploy to Maven Central"
        id = RelativeId(name.toId())
        params {
            text(
                "env.MAVEN_RELEASE_VERSION",
                "",
                label = "Release Version",
                display = PROMPT,
                regex = "\\d+\\.\\d+(\\.\\d+)?",
                validationMessage = "The version needs to be of the form <major>.<minor>(.<patch>)"
            )
            text(
                "env.MAVEN_DEVELOPMENT_VERSION",
                "",
                label = "The next development version (should end with '-SNAPSHOT')",
                display = PROMPT,
                regex = "\\d+\\.\\d+(\\.\\d+)?-SNAPSHOT",
                validationMessage = "The version needs to be of the form <major>.<minor>(.<patch>)-SNAPSHOT"
            )
            text(
                "env.GITHUB_USER",
                "",
                label = "GitHub Username",
                display = PROMPT,
                allowEmpty = false
            )
            password("env.GITHUB_ACCESS_TOKEN", "", label = "GitHub Access Token", display = PROMPT)
            param("env.MAVEN_CENTRAL_STAGING_REPO_USER", "%mavenCentralStagingRepoUser%")
            param("env.MAVEN_CENTRAL_STAGING_REPO_PASSWORD", "%mavenCentralStagingRepoPassword%")
        }
        vcs {
            root(DslContext.settingsRootId)
            cleanCheckout = true
        }
        steps {
            script {
                name = "Import signing key"
                scriptContent = "echo \"\$PGP_SIGNING_KEY\" | gpg --import --batch"
            }
            maven("release:prepare", """
                    -DreleaseVersion=%env.MAVEN_RELEASE_VERSION%
                    -DdevelopmentVersion=%env.MAVEN_DEVELOPMENT_VERSION%
                    -Dtag=common-custom-user-data-maven-extension-%env.MAVEN_RELEASE_VERSION%
                    -Prelease
                """.trimIndent())
            maven("release:perform", "-Prelease")
            script {
                name = "Remove signing key"
                scriptContent = "gpg --delete-secret-and-public-key --batch --yes 314FE82E5A4C5377BCA2EDEC5208812E1E4A6DB0"
                executionMode = BuildStep.ExecutionMode.ALWAYS
            }
        }
        requirements {
            contains("teamcity.agent.jvm.os.name", "Linux")
        }
    }
}

fun BuildSteps.maven(goal: String) {
    this.maven(goal, null)
}

fun BuildSteps.maven(goal: String, args: String?) {
    this.maven {
        pomLocation = "common-custom-user-data-maven-extension/pom.xml"
        goals = goal
        runnerArgs = "-Dgradle.enterprise.url=https://e.grdev.net" + (args?.let { " $it" } ?: "")
        mavenVersion = custom {
            path = "%teamcity.tool.maven.3.6.3%"
        }
        localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
        userSettingsSelection = "settings.xml"
        jdkHome = "%linux.java8.oracle.64bit%"
    }
}
