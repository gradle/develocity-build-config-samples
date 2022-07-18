import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * This Gradle script captures the <i>Git diff</i> in a GitHub gist,
 * and references the gist via a custom link.
 * This should be applied to the root project:
 * <code> apply from: file('gradle-git-diffs.gradle.kts') </code>
 *
 * In order for the gist to be created successfully, you must specify the Gradle property `gistToken` where
 * the value is a Github access token that has gist permission on the given Github repo.
 *
 * See https://docs.github.com/en/free-pro-team@latest/rest/reference/gists#create-a-gist for reference.
 */
project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        background {
            captureGitDiffInGist(buildScan)
        }
    }
}

fun captureGitDiffInGist(buildScan: BuildScanExtension): Unit {
    val isCapturingScan = !gradle.startParameter.isOffline && !gradle.startParameter.isContinuous
    if (!isCapturingScan) {
        gradle.rootProject.logger.warn("Build is offline or continuous. Will not publish gist.")
        return
    }

    val hasGistCredentials = gradle.rootProject.hasProperty("gistToken")
    if (!hasGistCredentials) {
        gradle.rootProject.logger.warn("User has not set 'gistToken'. Cannot publish gist.")
        return
    }

    val diff = execAndGetStdout("git", "diff")
    if (!diff.isNullOrEmpty()) {
        try {
            val baseUrl = java.net.URL("https://api.github.com/gists")
            val credentials = gradle.rootProject.findProperty("gistToken") as String
            val auth = "token " + credentials

            val connection = baseUrl.openConnection() as java.net.HttpURLConnection
            connection.apply {
                // request
                setRequestProperty("Authorization", auth)
                setRequestProperty("Accept", "application/vnd.github+json")
                requestMethod = "POST"
                doOutput = true
                outputStream.bufferedWriter().use { writer ->
                    jsonRequest(writer, diff, gradle.rootProject)
                }

                // response
                val url = JsonSlurper().parse(content as java.io.InputStream).withGroovyBuilder { getProperty("html_url") }
                buildScan.link("Git diff", url as String)
            }
            gradle.rootProject.logger.info("Successfully published gist.")
        } catch (ex: Exception) {
            gradle.rootProject.logger.warn("Unable to publish gist", ex)
        }
    }
}

// this method must be static, otherwise Gradle will interpret `files` as Project.files() and this won't work
fun jsonRequest(writer: java.io.Writer, diff: String, project: Project): Unit {
    val json = groovy.json.JsonOutput.toJson(mapOf(
        "description" to "Git diff for ${project.name}",
        "public" to false,
        "files" to mapOf("${project.name}.diff" to mapOf("content" to diff))))
    writer.write(json)
}

fun execAndGetStdout(vararg args: String): String {
    val process = ProcessBuilder(*args)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
    try {
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            val standardText = process.inputStream.bufferedReader().readText()
            val ignore = process.errorStream.bufferedReader().readText()

            return if (finished && process.exitValue() == 0) trimAtEnd(standardText) else return ""
    } finally {
        process.destroyForcibly()
    }
}

fun trimAtEnd(str: String): String {
    return ("x" + str).trim().substring(1)
}
