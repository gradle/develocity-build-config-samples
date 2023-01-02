import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * This Gradle script captures the <i>Git diff</i> in a GitHub gist,
 * and references the gist via a custom link.
 *
 * In order for the gist to be created successfully, you must specify the Gradle property `gistToken` where
 * the value is a Github access token that has gist permission on the given Github repo.
 *
 * See https://docs.github.com/en/rest/gists/gists#create-a-gist for reference.
 */

project.extensions.configure<GradleEnterpriseExtension>() {

    val capture = Capture(gradle.startParameter.isOffline,
        gradle.startParameter.isContinuous,
        gradle.rootProject.logger,
        providers.gradleProperty("gistToken"),
        rootProject.name)

    buildScan {
        background {
            if(capture.isEnabled()) {
                capture.captureGitDiffInGist(buildScan)
            }
        }
    }
}

class Capture(val offline: Boolean,
              val continuous: Boolean,
              val logger: Logger,
              val gistTokenProvider: Provider<String>,
              val projectName: String) {

    fun isEnabled(): Boolean {
        val isCapturingScan = !offline && !continuous
        if (!isCapturingScan) {
            logger.warn("Build is offline or continuous. Will not publish gist.")
            return false
        }
        return true
    }

    fun captureGitDiffInGist(api: BuildScanExtension): Unit {
        val hasCredentials = gistTokenProvider.isPresent
        if (!hasCredentials) {
            logger.warn("User has not set 'gistToken'. Cannot publish gist.")
            return
        }

        val diff = execAndGetStdout("git", "diff")
        if (!diff.isNullOrEmpty()) {
            try {
                val baseUrl = java.net.URL("https://api.github.com/gists")
                val credentials = Base64.getEncoder().encodeToString(gistTokenProvider.get().toByteArray())
                val basicAuth = "Basic ${credentials}"

                val connection = baseUrl.openConnection() as java.net.HttpURLConnection
                connection.apply {
                    // request
                    setRequestProperty("Authorization", basicAuth)
                    setRequestProperty("Accept", "application/vnd.github+json")
                    requestMethod = "POST"
                    doOutput = true
                    outputStream.bufferedWriter().use { writer ->
                        jsonRequest(writer, diff)
                    }

                    // response
                    val url = JsonSlurper().parse(content as java.io.InputStream).withGroovyBuilder { getProperty("html_url") }
                    api.link("Git diff", url as String)
                }
                logger.info("Successfully published gist.")
            } catch (ex: Exception) {
                logger.warn("Unable to publish gist", ex)
            }
        }
    }

    // this method must be static, otherwise Gradle will interpret `files` as Project.files() and this won't work
    private fun jsonRequest(writer: java.io.Writer, diff: String): Unit {
        val json = groovy.json.JsonOutput.toJson(mapOf(
            "description" to "Git diff for ${projectName}",
            "public" to false,
            "files" to mapOf("${projectName}.diff" to mapOf("content" to diff))))
        writer.write(json)
    }

    private fun execAndGetStdout(vararg args: String): String {
        val process = ProcessBuilder(*args)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
        try {
                val finished = process.waitFor(10, TimeUnit.SECONDS)
                val standardText = process.inputStream.bufferedReader().use { it.readText() }
                val ignore = process.errorStream.bufferedReader().use { it.readText() }

                return if (finished && process.exitValue() == 0) trimAtEnd(standardText) else return ""
        } finally {
            process.destroyForcibly()
        }
    }

    private fun trimAtEnd(str: String): String {
        return ("x" + str).trim().substring(1)
    }

}
