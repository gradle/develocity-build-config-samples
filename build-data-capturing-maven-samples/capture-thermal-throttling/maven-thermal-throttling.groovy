import java.nio.charset.Charset
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Function

/**
 * This Groovy script captures the thermal throttling level and adds it as a tag.
 * Some parameters can be tweaked according to your build:
 * - SAMPLING_INTERVAL_IN_SECONDS, frequency at which the capture command is run
 * - THROTLLING_LEVEL, list of throttling levels and value ranges (to be compared with captured throttling average value)
 *
 * WARNING:
 * - This is supported on MacOS only.
 */

def osName = System.getProperty("os.name")
if (osName.contains("OS X") || osName.startsWith("Darwin")) {
  def thermalThrottlingService = new ThermalThrottlingService()

  // start service
  thermalThrottlingService.start()

  buildScan.buildFinished(buildResult -> {
    try (thermalThrottlingService) {
      // process results on build completion
      thermalThrottlingService.processResults(buildScan)
    }
  })
} else {
  println "INFO - Not running on MacOS - no thermal throttling data will be captured"
}

// Thermal Throttling service implementation
class ThermalThrottlingService implements AutoCloseable {

  /**
   * Sampling interval can be adjusted according to total build time.
   */
  def SAMPLING_INTERVAL_IN_SECONDS = 5

  /**
   * Throttling levels by throttling average value.
   */
  def THROTTLING_LEVEL =
          [
                  [level: "THROTTLING_HIGH", range: 0..40],
                  [level: "THROTTLING_MEDIUM", range: 40..80],
                  [level: "THROTTLING_LOW", range: 80..100]
          ]

  def COMMAND_ARGS = ["pmset", "-g", "therm"]
  def COMMAND_OUTPUT_PARSING_PATTERN = /CPU_Speed_Limit\s+=\s+/

  def scheduler
  def samples

  ThermalThrottlingService() {
    scheduler = Executors.newScheduledThreadPool(1)
    samples = new ConcurrentLinkedQueue<Integer>()
  }

  void start() {
    scheduler.scheduleAtFixedRate(new ProcessRunner(COMMAND_ARGS, this::processCommandOutput), 0, SAMPLING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS)
  }

  @Override
  void close() throws Exception {
    scheduler.shutdownNow()
  }

  void processCommandOutput(String commandOutput) {
    def tokens = commandOutput.split(COMMAND_OUTPUT_PARSING_PATTERN)
    if (tokens != null && tokens.length > 0) {
      def sample = tokens[1] as Integer
      if (sample != null) {
        samples.offer(sample)
      }
    }
  }

  void processResults(def buildScanApi) {
    if (!samples.isEmpty()) {
      def average = samples.stream().mapToInt(Integer::intValue).average().getAsDouble()
      if (average < 100) {
        buildScanApi.value "CPU Thermal Throttling Average", String.format("%.2f", average) + "%"

        THROTTLING_LEVEL.findAll { entry ->
          entry.range.from <= average && average < entry.range.to
        }.each { entry ->
          buildScanApi.tag entry.level
        }
      }
    }
  }
}

// Process Runner implementation
class ProcessRunner implements Runnable {

  private List<String> args
  private Function<String, Integer> outputProcessor

  ProcessRunner(List<String> args, Function<String, Integer> outputProcessor) {
    this.args = args
    this.outputProcessor = outputProcessor
  }

  @Override
  void run() throws Exception {
    def stdout = execAndGetStdout(args)
    outputProcessor.apply(stdout)
  }

  private String execAndGetStdout(List<String> args) {
    Process process = args.execute()
    try {
      def standardText = process.inputStream.withStream { s -> s.getText(Charset.defaultCharset().name()) }
      def ignore = process.errorStream.withStream { s -> s.getText(Charset.defaultCharset().name()) }

      def finished = process.waitFor(10, TimeUnit.SECONDS)
      finished && process.exitValue() == 0 ? trimAtEnd(standardText) : null
    } finally {
      process.destroyForcibly()
    }
  }

  private String trimAtEnd(String str) {
    ('x' + str).trim().substring(1)
  }

}
