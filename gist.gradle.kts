buildScan {
    gradle.rootProject.logger.warn("Attempting to configure buildScan.")
    background {
        publishGist()
    }
}

fun publishGist() {
    // For this to work, you need must specify this property in ~/.gradle/gradle.properties
    // TODO add a property for enabling/disabling this, as it takes a while
    val isCapturingScan = !gradle.startParameter.isOffline && !gradle.startParameter.isContinuous
    if (!isCapturingScan) {
        gradle.rootProject.logger.warn("Build is offline or continuous. Not publishing gist.")
    }

    val hasGistCredentials = gradle.rootProject.hasProperty("gistToken")
    if (!hasGistCredentials) {
        gradle.rootProject.logger.warn("User has not set 'gistToken'. Cannot publish gist.")
        return
    }

    val diff = execAndGetStdout("git", "diff")
    if (diff.isNotEmpty()) {
        try {
            val baseUrl = java.net.URL("https://api.github.com/gists")
            val credentials = "${gradle.rootProject.findProperty("gistToken")}"
            val basicAuth = "Basic ${credentials.toByteArray().encodeBase64()}"

            val connection: java.net.HttpURLConnection = baseUrl.openConnection() as java.net.HttpURLConnection
            with(connection) {
                setRequestProperty("Authorization", basicAuth)
                doOutput = true
                requestMethod = "POST"
                org.codehaus.groovy.runtime.IOGroovyMethods.withWriter(outputStream, closureOf<java.io.Writer> {
                    jsonRequest(this, gradle.rootProject, diff)
                })
                createLink(content as java.io.InputStream)
            }
            gradle.rootProject.logger.info("Successfully published gist.")
        } catch (ex: Exception) {
            gradle.rootProject.logger.warn("Unable to publish gist", ex)
        }
    }
}

fun ByteArray.encodeBase64() = org.codehaus.groovy.runtime.EncodingGroovyMethods.encodeBase64(this)

fun execAndGetStdout(vararg args: String): String {
    val stdout = java.io.ByteArrayOutputStream()
    exec {
        commandLine(*args)
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

fun jsonRequest(out: java.io.Writer, project: Project, diff: String) {
    val builder = groovy.json.JsonBuilder()
    builder.call(delegateClosureOf<Any> {
        withGroovyBuilder {
            "description"("Git diff for $project.name")
            "public"(false)
            "files" {
                "${project.name}.diff" {
                    "content"(diff)
                }
            }
        }
    })
    builder.writeTo(out)
}

fun createLink(content: java.io.InputStream) {
    val parser = groovy.json.JsonSlurper()
    @Suppress("UNCHECKED_CAST")
    val url = (parser.parse(content.readBytes()) as Map<String, String>)["html_url"]
    buildScan.link("Git diff", url)
}