import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import org.gradle.util.internal.VersionNumber
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Optional
import java.util.jar.JarFile
import java.util.stream.Stream
import java.util.stream.Collectors
import groovy.transform.Field

/**
 * This Gradle script captures Predictive Test Selection and Test Distribution compatibility for each Test task,
 * adding a flag as custom value.
 */

project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        val api = buildScan
        val capture = Capture(api, gradle.rootProject.logger)
        allprojects {
            tasks.withType<Test>().configureEach {
                doFirst {
                    capture.capturePts(this as Test)
                }
            }
        }
    }
}

class Capture(val api: BuildScanExtension, val logger: Logger) {
    val supportedEngines: Map<String, String> = mapOf(
        "org.junit.support.testng.engine.TestNGTestEngine" to "testng",
        "org.junit.jupiter.engine.JupiterTestEngine" to "junit-jupiter",
        "org.junit.vintage.engine.VintageTestEngine" to "junit-vintage",
        "org.spockframework.runtime.SpockEngine" to "spock",
        "net.jqwik.engine.JqwikTestEngine" to "jqwik",
        "com.tngtech.archunit.junit.ArchUnitTestEngine" to "archunit",
        "co.helmethair.scalatest.ScalatestEngine" to "scalatest",
        "io.kotest.runner.junit.platform.KotestJunitPlatformTestEngine" to "kotest-runner",
        "io.cucumber.junit.platform.engine.CucumberTestEngine" to "cucumber-junit-platform"
    )

    fun capturePts(t: Test) {
        if (t.getTestFramework()::class.java.name == "org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework") {
            val engines = testEngines(t)
            api.value("${t.identityPath}#engines", "${engines}")
            if (ptsSupported(t, engines)) {
                api.value("${t.identityPath}#pts", "SUPPORTED")
            } else {
                api.value("${t.identityPath}#pts", "ENGINES_NOT_ALL_SUPPORTED")
            }
        } else {
            api.value("${t.identityPath}#pts", "NO_JUNIT_PLATFORM")
        }
    }

    private fun ptsSupported(t: Test, engines: Set<String>): Boolean {
        return if (!engines.isEmpty() && engines.stream().allMatch { e -> supportedEngines.containsKey(e) }) {
            // If cucumber is used without companion it's not supported, otherwise it is.
            !(cucumberUsed(t) && !t.project.plugins.hasPlugin("com.gradle.cucumber.companion"))
        } else {
            false
        }
    }

    private fun cucumberUsed(t: Test): Boolean {
        return t.project.configurations.filter { it.isCanBeResolved }.any {
            it.resolvedConfiguration.resolvedArtifacts.any {
                it.moduleVersion.id.group == "io.cucumber"
            }
        }
    }

    private fun testEngines(t: Test): Set<String> {
        try {
            var engines = t.classpath.files.stream().filter { f -> f.name.endsWith(".jar") }
                .filter { f -> supportedEngines.values.stream().anyMatch { e -> f.name.contains(e) } }
                .filter { f -> isCompatibleVersion(f, t) }
                .map { f -> findTestEngine(f) }
                .flatMap { o -> if (o.isPresent()) Stream.of(o.get()) else Stream.empty() }

            // We take into account included/excluded engines (but only known ones)
            val included = (t.options as JUnitPlatformOptions).includeEngines
            if (!included.isEmpty()) {
                engines =
                    engines.filter { e -> supportedEngines.get(e) == null || included.contains(supportedEngines.get(e)) }
            }
            val excluded = (t.options as JUnitPlatformOptions).excludeEngines
            if (!excluded.isEmpty()) {
                engines =
                    engines.filter { e -> supportedEngines.get(e) == null || !excluded.contains(supportedEngines.get(e)) }
            }
            return engines.collect(Collectors.toSet())
        } catch (e: Exception) {
            logger.warn("Could not detect test engines", e)
        }
        return Collections.emptySet()
    }

    private fun findTestEngine(jar: File): Optional<String> {
        JarFile(jar).use { j ->
            return Optional.ofNullable(j.getEntry("META-INF/services/org.junit.platform.engine.TestEngine"))
                .map { e ->
                    j.getInputStream(e).bufferedReader().use { b -> b.readText().trim() }
                }
        }
    }

    private fun isCompatibleVersion(f: File, t: Test): Boolean {
        if (f.name.contains("kotest-runner")) {
            val kotestVersionString = f.name.split("-")[f.name.split("-").lastIndex].replace(".jar", "")
            val kotestVersion = VersionNumber.parse(kotestVersionString)
            if (VersionNumber.UNKNOWN == kotestVersion) {
                logger.error("Unable to parse kotest version from file name ${f.name}")
                api.value("${t.identityPath}#unknownKotestVersion", "${f.name}")
                return false
            }
            return VersionNumber.parse("5.6.0") <= kotestVersion
        } else {
            return true
        }
    }
}
