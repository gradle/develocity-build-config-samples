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
        name = "Deploy to Maven Central"
        val camelCaseId = this@project.name
                .replace(" ", "-")
                .replace(Regex("""[^A-Za-z0-9-]"""), "")
                .split('-')
                .joinToString("", transform = String::capitalize)
        id = AbsoluteId(camelCaseId.toId(this@project.id.toString()))
        params {
            text(
                "env.MAVEN_RELEASE_VERSION",
                "",
                label = "Release Version",
                display = PROMPT,
                allowEmpty = false
            )
            text(
                "env.MAVEN_DEVELOPMENT_VERSION",
                "",
                label = "The next development version (should end with '-SNAPSHOT')",
                display = PROMPT,
                allowEmpty = false
            )
            param("env.MAVEN_CENTRAL_STAGING_REPO_USER", "%mavenCentralStagingRepoUser%")
            param("env.MAVEN_CENTRAL_STAGING_REPO_PASSWORD", "%mavenCentralStagingRepoPassword%")
        }
        steps {
            script {
                name = "Setup GPG"
                scriptContent = "echo %env.PGP_SIGNING_KEY% | base64 --decode | gpg --import"
            }
            maven {
                goals = "release:prepare"
                runnerArgs = """
                    -Dscan=false
                    -DreleaseVersion=%env.MAVEN_RELEASE_VERSION%
                    -DdevelopmentVersion=%env.MAVEN_DEVELOPMENT_VERSION%
                    -Dtag=common-custom-user-data-maven-extension-%env.MAVEN_RELEASE_VERSION%
                    -Prelease
                """.trimIndent()
                mavenVersion = custom {
                    path = "%teamcity.tool.maven.3.6.3%"
                }
                localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
                jdkHome = "%linux.java8.oracle.64bit%"
            }
            maven {
                goals = "release:perform"
                runnerArgs = "-Dscan=false -Prelease"
                mavenVersion = custom {
                    path = "%teamcity.tool.maven.3.6.3%"
                }
                localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
                jdkHome = "%linux.java8.oracle.64bit%"
            }
        }
        triggers {
            vcs {
                id = "vcsTrigger"
            }
        }
        requirements {
            contains("teamcity.agent.jvm.os.name", "Linux")
        }
    }

}
