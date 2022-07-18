import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
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
        val capture = Capture(gradle.rootProject.logger)
        allprojects {
            tasks.withType<Test>().configureEach {
                doFirst {
                    capture.capturePts(this as Test, api)
                }
            }
        }
    }
}

class Capture(val logger: Logger) {
    val supportedEngines: Map<String, String> = mapOf(
        "org.junit.support.testng.engine.TestNGTestEngine" to "testng",
        "org.junit.jupiter.engine.JupiterTestEngine" to "junit-jupiter",
        "org.junit.vintage.engine.VintageTestEngine" to "junit-vintage",
        "org.spockframework.runtime.SpockEngine" to "spock",
        "net.jqwik.engine.JqwikTestEngine" to "jqwik",
        "com.tngtech.archunit.junit.ArchUnitTestEngine" to "archunit",
        "co.helmethair.scalatest.ScalatestEngine" to "scalatest"
    )

    fun capturePts(t: Test, api: BuildScanExtension): Unit {
        if (t.getTestFramework()::class.java.name == "org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework") {
            val engines = testEngines(t)
            api.value("${t.identityPath}#engines", "${engines}")
            if (!engines.isEmpty() && engines.stream().allMatch { e -> supportedEngines.containsKey(e) }) {
                api.value("${t.identityPath}#pts", "SUPPORTED")
            } else {
                api.value("${t.identityPath}#pts", "ENGINES_NOT_ALL_SUPPORTED")
            }
        } else {
            api.value("${t.identityPath}#pts", "NO_JUNIT_PLATFORM")
        }
    }

    private fun testEngines(t: Test): Set<String> {
        try {
            var engines = t.classpath.files.stream().filter { f -> f.name.endsWith(".jar") }
                .filter { f -> supportedEngines.values.stream().anyMatch { e -> f.name.contains(e) } }
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
}
