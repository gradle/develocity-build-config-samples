import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import org.gradle.internal.os.OperatingSystem
import com.gradle.scan.plugin.BuildScanExtension

import java.nio.charset.Charset
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.Queue

import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.FinishEvent

/**
 * This Gradle script captures the thermal throttling level and adds it as a tag.
 * Some parameters can be tweaked according to your build:
 * - SAMPLING_INTERVAL_IN_SECONDS, frequency at which the capture command is run
 * - THROTLLING_LEVEL, list of throttling levels and value ranges (to be compared with captured throttling average value)
 *
 * WARNINGS:
 * - This is supported on MacOS only.
 */

project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        val api = buildScan
        if (OperatingSystem.current().isMacOsX()) {
            // Register Thermal throttling service
            val throttlingServiceProvider =
                gradle.sharedServices.registerIfAbsent("thermalThrottling", ThermalThrottlingService::class.java) {}
            // start service
            (project as ProjectInternal).services.get(BuildEventsListenerRegistry::class.java).onTaskCompletion(throttlingServiceProvider)

            buildScan.buildFinished {
                throttlingServiceProvider.get().use { s ->
                    // process results on build completion
                    s.processResults(api)
                }
            }
        } else {
            println("INFO - Not running on MacOS - no thermal throttling data will be captured")
        }
    }
}

// Thermal Throttling service implementation
// OperationCompletionListener is a marker interface to get the service instantiated when configuration cache kicks in
abstract class ThermalThrottlingService : BuildService<BuildServiceParameters.None>, AutoCloseable, OperationCompletionListener {

    /**
     * Sampling interval can be adjusted according to total build time.
     */
    val SAMPLING_INTERVAL_IN_SECONDS = 5L

    /**
     * Throttling levels by throttling average value.
     */
    val THROTTLING_LEVEL = mapOf("THROTTLING_HIGH" to 0..40, "THROTTLING_MEIUM" to 40..80, "THROTTLING_LOW" to 80..100)

    val COMMAND_ARGS = listOf("pmset", "-g", "therm")
    val COMMAND_OUTPUT_PARSING_PATTERN = Regex("""CPU_Speed_Limit\s+=\s+""")

    val scheduler: ExecutorService
    val samples: Queue<Int>

    init {
        scheduler = Executors.newScheduledThreadPool(1)
        samples = ConcurrentLinkedQueue<Int>()
        scheduler.scheduleAtFixedRate(ProcessRunner(COMMAND_ARGS, this::processCommandOutput), 0, SAMPLING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS)
    }

    override fun close() {
        scheduler.shutdownNow()
    }

    override fun onFinish(ignored: FinishEvent){
        // ignored
    }

    private fun processCommandOutput(commandOutput: String): Unit {
        val tokens = commandOutput.split(COMMAND_OUTPUT_PARSING_PATTERN)
        if (tokens != null && tokens.size > 0) {
            val sample = tokens[1].toIntOrNull()
            if (sample != null) {
                samples.offer(sample)
            }
        }
    }

    fun processResults(api: BuildScanExtension): Unit {
        if (!samples.isEmpty()) {
            val average = samples.stream().mapToInt{ it as Int }.average().getAsDouble()
            if (average < 100.0) {
                api.value("CPU Thermal Throttling Average", String.format("%.2f", average) + "%")
                THROTTLING_LEVEL.entries.stream().filter { e ->
                    e.value.start <= average && average < e.value.endInclusive
                }.forEach { e -> api.tag(e.key) }
            }
        }
    }
}

// Process Runner implementation
class ProcessRunner : Runnable {

    val args: List<String>
    val outputProcessor: Function<String, Unit>

    constructor(args: List<String>, outputProcessor: Function<String, Unit>) {
        this.args = args
        this.outputProcessor = outputProcessor
    }

    override fun run() {
        val stdout = execAndGetStdout(args)
        if (stdout != null) {
            outputProcessor.apply(stdout)
        }
    }

    private fun execAndGetStdout(args: List<String>): String? {
        val process = ProcessBuilder(args).start()
        try {
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            val standardText = process.inputStream.bufferedReader().readText()
            val ignore = process.errorStream.bufferedReader().readText()
            return if (finished && process.exitValue() == 0) trimAtEnd(standardText) else null
        } finally {
            process.destroyForcibly()
        }
    }

    private fun trimAtEnd(str: String): String {
        return ("x" + str).trim().substring(1)
    }

}
