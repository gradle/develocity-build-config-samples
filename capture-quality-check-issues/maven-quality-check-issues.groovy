import java.nio.file.Paths

/**
 * This Groovy script captures issues found by reporting goals and stores them in build scans via custom values.
 */

BuildScanApi buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')
if (!buildScan) {
    return
}

buildScan.executeOnce('reporting-goals') { api ->
    addReportingGoalIssues(api)
}

void addReportingGoalIssues(def api) {
    api.buildFinished { result ->
        def topLevelProject = session.topLevelProject
        while (topLevelProject.hasParent()) {
            topLevelProject = topLevelProject.parent
        }
        def topLevelProjectPath = Paths.get(topLevelProject.basedir.absolutePath as String)
        session.projectDependencyGraph.sortedProjects.each { project ->
            session.currentProject = project
            def lifecycleExecutor = container.lookup('org.apache.maven.lifecycle.LifecycleExecutor')
            def mojoExecutions = lifecycleExecutor.calculateExecutionPlan(session, *session.goals).getMojoExecutions()
            def reportingMojoExecutions = mojoExecutions.findAll { isCapturedReportingPlugin(it.plugin) }
            if (!reportingMojoExecutions) {
                return
            }
            def mojoInterface = Class.forName('org.apache.maven.plugin.Mojo')
            def componentConfiguratorClass = Class.forName('org.codehaus.plexus.component.configurator.ComponentConfigurator')
            def xmlPlexusConfigurationClass = Class.forName('org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration')
            def pluginParameterExpressionEvaluatorClass = Class.forName('org.apache.maven.plugin.PluginParameterExpressionEvaluator')
            reportingMojoExecutions.each { mojoExecution ->
                def (mojo, oldLookupRealm, oldClassLoader) = reportingMojo(session, mojoExecution, mojoInterface, componentConfiguratorClass, xmlPlexusConfigurationClass, pluginParameterExpressionEvaluatorClass)
                def valueName = '' as String
                def errors = [] as List<String>
                if (ReportingPlugin.CHECKSTYLE.isPlugin(mojoExecution.plugin)) {
                    def outputFileFormat = mojo.outputFileFormat
                    if (outputFileFormat != 'xml') {
                        return
                    }
                    def reportFile = mojo.outputFile
                    if (!reportFile.exists()) {
                        return
                    }
                    def report = new XmlSlurper().parse(reportFile)
                    valueName = 'Checkstyle Issue'
                    errors = report.file.collect {
                        String filePath = topLevelProjectPath.relativize(Paths.get(it.@name.text() as String))
                        it.error.collect { "${filePath}:${it.@line}:${it.@column} \u2192 ${it.@message}" }
                    }.flatten()
                } else if (ReportingPlugin.CODENARC.isPlugin(mojoExecution.plugin)) {
                    def xmlOutputDirectory = mojo.xmlOutputDirectory
                    def reportFile = new File(xmlOutputDirectory, 'CodeNarc.xml')
                    if (!reportFile.exists()) {
                        return
                    }
                    def report = new XmlSlurper().parse(reportFile)
                    valueName = 'CodeNarc Issue'
                    errors = report.Package.collect {
                        def sourceDirectory = appendIfMissing(it.parent().Project.SourceDirectory.text() as String, '/') as String
                        it.File.collect {
                            String filePath = topLevelProjectPath.relativize(Paths.get(sourceDirectory + (it.@name.text() as String)))
                            it.Violation.collect { "${filePath}:${it.@lineNumber} \u2192 ${it.Message.text()}" }
                        }.flatten()
                    }.flatten()
                } else if (ReportingPlugin.FINDBUGS.isPlugin(mojoExecution.plugin)) {
                    if (!mojo.xmlOutput) {
                        return
                    }
                    def xmlOutputDirectory = mojo.xmlOutputDirectory
                    def reportFile = new File(xmlOutputDirectory, 'findbugsXml.xml')
                    if (!reportFile.exists()) {
                        return
                    }
                    def report = new XmlSlurper().parse(reportFile)
                    valueName = 'Verification FindBugs'
                    errors = report.BugInstance.collect { bugInstance ->
                        String type = bugInstance.ShortMessage.text()
                        def error = bugInstance.breadthFirst().findAll { it.name() == 'SourceLine' }.sort(false) { it.parent().name() as SpotBugsParent }.first()
                        String startLine = error.@start.text()
                        String endLine = error.@end.text()
                        String lineNumber = endLine == startLine ? startLine : "$startLine-$endLine"
                        def absoluteFilePath = bugInstance.parent().Project.SrcDir.collect { it.text() }.collect { new File(it as String, error.@sourcepath.text()) }.find { it.exists() }.absolutePath
                        String filePath = topLevelProjectPath.relativize(Paths.get(absoluteFilePath))
                        "${filePath}:${lineNumber} \u2192 ${type}"
                    }.flatten()
                } else if (ReportingPlugin.SPOTBUGS.isPlugin(mojoExecution.plugin)) {
                    if (!mojo.xmlOutput) {
                        return
                    }
                    def xmlOutputDirectory = mojo.xmlOutputDirectory
                    def reportFile = new File(xmlOutputDirectory, 'spotbugsXml.xml')
                    if (!reportFile.exists()) {
                        return
                    }
                    def report = new XmlSlurper().parse(reportFile)
                    valueName = 'Verification SpotBugs'
                    errors = report.BugInstance.collect { bugInstance ->
                        String type = bugInstance.ShortMessage.text()
                        def error = bugInstance.breadthFirst().findAll { it.name() == 'SourceLine' }.sort(false) { it.parent().name() as SpotBugsParent }.first()
                        String startLine = error.@start.text()
                        String endLine = error.@end.text()
                        String lineNumber = endLine == startLine ? startLine : "$startLine-$endLine"
                        def absoluteFilePath = bugInstance.parent().Project.SrcDir.collect { it.text() }.collect { new File(it as String, error.@sourcepath.text()) }.find { it.exists() }.absolutePath
                        String filePath = topLevelProjectPath.relativize(Paths.get(absoluteFilePath))
                        "${filePath}:${lineNumber} \u2192 ${type}"
                    }.flatten()
                }
                errors.each { api.value valueName, it }
                Thread.currentThread().contextClassLoader = oldClassLoader
                session.container.setLookupRealm(oldLookupRealm)
            }
        }
    }
}

enum SpotBugsParent {
    BugInstance, Method, Class
}

enum ReportingPlugin {

    CHECKSTYLE('org.apache.maven.plugins:maven-checkstyle-plugin'),
    CODENARC('org.codehaus.mojo:codenarc-maven-plugin'),
    FINDBUGS('org.codehaus.mojo:findbugs-maven-plugin'),
    SPOTBUGS('com.github.spotbugs:spotbugs-maven-plugin')

    String pluginKey

    private ReportingPlugin(String pluginKey) {
        this.pluginKey = pluginKey
    }

    ReportingPlugin isPlugin(plugin) {
        plugin.key == pluginKey ? this : null
    }
}

static boolean isCapturedReportingPlugin(plugin) {
    ReportingPlugin.values().toList().any { it.isPlugin(plugin) }
}

static def reportingMojo(def session, def mojoExecution, def mojoInterface, def componentConfiguratorClass, def xmlPlexusConfigurationClass, def pluginParameterExpressionEvaluatorClass) {
    def pluginRealm = mojoExecution.mojoDescriptor.pluginDescriptor.classRealm
    def oldLookupRealm = session.container.setLookupRealm(pluginRealm)
    def oldClassLoader = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = pluginRealm
    def mojo = session.container.lookup(mojoInterface, mojoExecution.mojoDescriptor.roleHint)
    def configurator = session.container.lookup(componentConfiguratorClass, mojoExecution.mojoDescriptor.componentConfigurator ?: 'basic')
    def configuration
    if (mojoExecution.configuration) {
        def xmlPlexusConfigurationConstructor = xmlPlexusConfigurationClass.getConstructor(mojoExecution.configuration.class)
        configuration = xmlPlexusConfigurationConstructor.newInstance(mojoExecution.configuration)
    } else {
        def xmlPlexusConfigurationConstructor = xmlPlexusConfigurationClass.getConstructor(String.class)
        configuration = xmlPlexusConfigurationConstructor.newInstance('configuration')
    }
    def pluginParameterExpressionEvaluatorConstructor = pluginParameterExpressionEvaluatorClass.getConstructor(session.class, mojoExecution.class)
    def expressionEvaluator = pluginParameterExpressionEvaluatorConstructor.newInstance(session, mojoExecution)
    configurator.configureComponent(mojo, configuration, expressionEvaluator, pluginRealm)
    session.container.release(configurator)
    [mojo, oldLookupRealm, oldClassLoader]
}

static String appendIfMissing(String str, String suffix) {
    str.endsWith(suffix) ? str : str + suffix
}
