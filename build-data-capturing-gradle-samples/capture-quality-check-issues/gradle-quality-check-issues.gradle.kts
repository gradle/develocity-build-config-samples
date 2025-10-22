import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.NodeChild
import groovy.xml.slurpersupport.NodeChildren

/**
 * This Gradle script captures issues found by reporting tasks,
 * and adds these as custom values.
 */

project.extensions.configure<DevelocityConfiguration>() {
    buildScan {
        project.tasks.configureEach {
            if (!this.reportingSupported) {
                return@configureEach
            }

            if (this is Reporting<*>) {
                val reportTask = this as Reporting<ReportContainer<*>>
                reportTask.reports.withGroovyBuilder { "xml" { setProperty("enabled", true) } }
            } else if (ReportingTask.SPOTBUGS.isTask(this)) {
                this.withGroovyBuilder {
                    val reports = getProperty("reports")
                    val xml = (reports as NamedDomainObjectContainer<*>).create("xml") {
                        enabled = true
                    }
                }
            }

            doLast {
                val reportFile = reportFromTask(this)
                if (reportFile.exists()) {
                    val report = XmlSlurper().parse(reportFile)
                    var valueName = ""
                    var errors = mutableListOf<String>()

                    if (ReportingTask.CHECKSTYLE.isTask(this)) {
                        valueName = "Verification Checkstyle"
                        val files = report.getProperty("file") as NodeChildren
                        files.forEach { f ->
                            val file = f as NodeChild
                            val filePath = project.rootProject.relativePath(file.attributes()["name"]!!)
                            val checkErrors = file.getProperty("error") as NodeChildren
                            checkErrors.forEach { e ->
                                val error = e as NodeChild
                                errors.add("${filePath}:${error.attributes()["line"]}:${error.attributes()["column"]} \u2192 ${error.attributes()["message"]}")
                            }
                        }
                    } else if (ReportingTask.CODENARC.isTask(this)) {
                        valueName = "Verification CodeNarc"
                        val packages = report.getProperty("Package") as NodeChildren
                        packages.forEach { p ->
                            val proj = report.getProperty("Project") as NodeChildren
                            val sourceDirectoryNodes = proj.getProperty("SourceDirectory") as NodeChildren
                            val sourceDirectories = sourceDirectoryNodes.map {
                                it as NodeChild
                                appendIfMissing(it.text() as String, "/")
                            }
                            val files = (p as NodeChild).getProperty("File") as NodeChildren
                            files.forEach { f ->
                                val file = f as NodeChild
                                val fileWithViolation = sourceDirectories.map {
                                    file(project.rootProject.relativePath(it + file.attributes()["name"]))
                                }.first {
                                    it.exists()
                                }
                                val filePath = project.rootProject.relativePath(fileWithViolation)
                                (file.getProperty("Violation") as NodeChildren).forEach { v ->
                                    val violation = v as NodeChild
                                    errors.add("${filePath}:${violation.attributes()["lineNumber"]} \u2192 ${violation.attributes()["ruleName"]}")
                                }
                            }
                        }
                    } else if (ReportingTask.SPOTBUGS.isTask(this)) {
                        valueName = "Verification SpotBugs"
                        val bugs = report.getProperty("BugInstance") as NodeChildren
                        bugs.forEach { b ->
                            val bug = b as NodeChild
                            val type = bug.attributes()["type"]
                            val error = bug.breadthFirst().asSequence().filter { l ->
                                val line = l as NodeChild
                                l.name() == "SourceLine"
                            }.sortedBy { l ->
                                (l as NodeChild).parent().name()
                            }.first() as NodeChild
                            val startLine = error.attributes()["start"]
                            val endLine = error.attributes()["end"]
                            val lineNumber = if (endLine == startLine) startLine else "${startLine}-${endLine}"
                            val className = error.attributes()["classname"]
                            errors.add("${className}:${lineNumber} \u2192 ${type}")
                        }
                    }
                    errors.forEach { e -> buildScan.value(valueName, e) }
                }
            }
        }
    }
}

fun reportFromTask(task: Task): File {
    if (task is Reporting<*>) {
        val task = task as Reporting<ReportContainer<*>>
        return (task.reports.withGroovyBuilder { "xml" { getProperty("destination") } } as Report).getOutputLocation().get().asFile as File
    } else if (ReportingTask.SPOTBUGS.isTask(task)) {
        val reports = task.withGroovyBuilder { getProperty("reports") as NamedDomainObjectContainer<*> }
        val report = (reports.named("XML") as NamedDomainObjectProvider).get() as SingleFileReport
        return report.getOutputLocation().get().asFile
    } else {
        throw IllegalStateException("Unsupported report task: " + task)
    }
}

fun appendIfMissing(str: String, suffix: String): String {
    return if (str.endsWith(suffix)) str else str + suffix
}

enum class SpotBugsParent {
    BugInstance, Method, Class
}

enum class ReportingTask(val className: String) {

    CHECKSTYLE("org.gradle.api.plugins.quality.Checkstyle"),
    CODENARC("org.gradle.api.plugins.quality.CodeNarc"),
    SPOTBUGS("com.github.spotbugs.snom.SpotBugsTask");

    fun isTask(task: Task): Boolean {
        return task::class.java.name.contains(className)
    }
}

val Task.reportingSupported: Boolean
    get() = ReportingTask.values().any { value -> value.isTask(this) }
