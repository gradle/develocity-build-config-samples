import java.nio.file.Paths

/**
 * This Groovy script captures data about the OS, IDE, CI, and Git and stores it in build scans via custom tags, custom links, and custom values.
 *
 * Proceed as following to benefit from this script in your Maven build:
 *
 * - Copy this script to the root folder of your Maven project, renaming it to 'build-scan-user-data.groovy'
 * - Apply the Gradle Enterprise Maven extension
 * - Point to your Gradle Enterprise server
 * - Include this script in your parent pom following https://docs.gradle.com/enterprise/maven-extension/#using_the_build_scan_api_in_a_groovy_script
 * - Further customize this script to your needs
 */

def buildScan = session.lookup("com.gradle.maven.extension.api.scan.BuildScanApi")

buildScan.executeOnce('custom-data') { api ->
    tagOs(api)
    tagIde(api)
    tagCiOrLocal(api)
    addCiMetadata(api)
    addGitMetadata(api)
    addReportingGoalIssues(api)
}

// Add here other scripts, if needed
//evaluate(new File("${session.request.multiModuleProjectDirectory}/<<other-script.groovy>>"))

static void tagOs(def api) {
    api.tag System.getProperty('os.name')
}

static void tagIde(def api) {
    if (System.getProperty('idea.version')) {
        api.tag 'IntelliJ IDEA'
    } else if (System.getProperty('eclipse.buildId')) {
        api.tag 'Eclipse'
    }
}

static void tagCiOrLocal(def api) {
    api.tag(isCi() ? 'CI' : 'LOCAL')
}

static void addCiMetadata(def api) {
    if (isJenkins()) {
        if (System.getenv('BUILD_URL')) {
            api.link 'Jenkins build', System.getenv('BUILD_URL')
        }
        if (System.getenv('BUILD_NUMBER')) {
            api.value 'CI build number', System.getenv('BUILD_NUMBER')
        }
        if (System.getenv('NODE_NAME')) {
            def agentName = System.getenv('NODE_NAME') == 'master' ? 'master-node' : System.getenv('NODE_NAME')
            api.tag agentName
            api.value 'CI node name', agentName
        }
        if (System.getenv('JOB_NAME')) {
            def jobNameLabel = 'CI job'
            def jobName = System.getenv('JOB_NAME')
            api.value jobNameLabel, jobName
            addCustomValueSearchLink api, 'CI job build scans', [(jobNameLabel): jobName]
        }
        if (System.getenv('STAGE_NAME')) {
            def stageNameLabel = 'CI stage'
            def stageName = System.getenv('STAGE_NAME')
            api.value stageNameLabel, stageName
            addCustomValueSearchLink api, 'CI stage build scans', [(stageNameLabel): stageName]
        }
    }

    if (isTeamCity()) {
        if (System.getenv('SERVER_URL') && System.getProperty('teamcity.agent.dotnet.build_id')) {
            def teamCityServerUrl = System.getenv('SERVER_URL')
            def teamCityBuildId = System.getProperty('teamcity.agent.dotnet.build_id')
            api.link 'TeamCity build', "${appendIfMissing(teamCityServerUrl, "/")}viewLog.html?buildId=${teamCityBuildId}"
        }
        if (System.getenv('BUILD_NUMBER')) {
            api.value 'CI build number', System.getenv('BUILD_NUMBER')
        }
        if (System.getProperty('agent.name')) {
            def agentName = System.getProperty('agent.name')
            api.tag agentName
            api.value 'CI agent name', agentName
        }
    }

    if (isCircleCI()) {
        if (System.getenv('CIRCLE_BUILD_URL')) {
            api.link 'CircleCI build', System.getenv('CIRCLE_BUILD_URL')
        }
        if (System.getenv('CIRCLE_BUILD_NUM')) {
            api.value 'CI build number', System.getenv('CIRCLE_BUILD_NUM')
        }
        if (System.getenv('CIRCLE_JOB')) {
            def jobLabel = 'CI job'
            def job = System.getenv('CIRCLE_JOB')
            api.value jobLabel, job
            addCustomValueSearchLink api, 'CI job build scans', [(jobLabel): job]
        }
        if (System.getenv('CIRCLE_WORKFLOW_ID')) {
            def workflowIdLabel = 'CI workflow'
            def workflowId = System.getenv('CIRCLE_WORKFLOW_ID')
            api.value workflowIdLabel, workflowId
            addCustomValueSearchLink api, 'CI workflow build scans', [(workflowIdLabel): workflowId]
        }
    }

    if (isBamboo()) {
        if (System.getenv('bamboo_resultsUrl')) {
            api.link 'Bamboo build', System.getenv('bamboo_resultsUrl')
        }
        if (System.getenv('bamboo_buildNumber')) {
            api.value 'CI build number', System.getenv('bamboo_buildNumber')
        }
        if (System.getenv('bamboo_planName')) {
            def planNameLabel = 'CI plan'
            def planName = System.getenv('bamboo_planName')
            api.value planNameLabel, planName
            addCustomValueSearchLink api, 'CI plan build scans', [(planNameLabel): planName]
        }
        if (System.getenv('bamboo_buildPlanName')) {
            def jobNameLabel = 'CI job'
            def jobName = System.getenv('bamboo_buildPlanName')
            api.value jobNameLabel, jobName
            addCustomValueSearchLink api, 'CI job build scans', [(jobNameLabel): jobName]
        }
        if (System.getenv('bamboo_agentId')) {
            def agentId = System.getenv('bamboo_agentId')
            api.tag agentId
            api.value 'CI agent ID', agentId
        }
    }
}

static void addGitMetadata(def api) {
    api.background { bck ->
        if (!isGitInstalled()) {
            return
        }
        def gitCommitId = execAndGetStdout('git', 'rev-parse', '--short=8', '--verify', 'HEAD')
        def gitBranchName = execAndGetStdout('git', 'rev-parse', '--abbrev-ref', 'HEAD')
        def gitStatus = execAndGetStdout('git', 'status', '--porcelain')

        if (gitCommitId) {
            def commitIdLabel = 'Git commit id'
            bck.value commitIdLabel, gitCommitId
            addCustomValueSearchLink bck, 'Git commit id build scans', [(commitIdLabel): gitCommitId]
            def originUrl = execAndGetStdout('git', 'config', '--get', 'remote.origin.url')
            if (originUrl.contains('github.com')) { // only for GitHub
                def repoPath = (originUrl =~ /(.*)github\.com[\/|:](.*)(.git)?/)[0][2]
                bck.link 'Github Source', "https://github.com/$repoPath/tree/" + gitCommitId
            }
        }
        if (gitBranchName) {
            bck.tag gitBranchName
            bck.value 'Git branch', gitBranchName
        }
        if (gitStatus) {
            bck.tag 'Dirty'
            bck.value 'Git status', gitStatus
        }
    }
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

static boolean isCi() {
    isJenkins() || isTeamCity() || isCircleCI() || isBamboo()
}

static boolean isJenkins() {
    System.getenv('JENKINS_URL')
}

static boolean isTeamCity() {
    System.getenv('TEAMCITY_VERSION')
}

static boolean isCircleCI() {
    System.getenv('CIRCLE_BUILD_URL')
}

static boolean isBamboo() {
    System.getenv('bamboo_resultsUrl')
}

static String execAndGetStdout(String... args) {
    def exec = args.toList().execute()
    exec.waitFor()
    exec.text.replaceAll('\\s+\$', '')
}

static void addCustomValueSearchLink(def api, String title, Map<String, String> search) {
    if (api.server) {
        api.link title, customValueSearchUrl(api, search)
    }
}

static String customValueSearchUrl(def api, Map<String, String> search) {
    def query = search.collect { name, value ->
        "search.names=${encodeURL(name)}&search.values=${encodeURL(value)}"
    }.join('&')
    "${appendIfMissing(api.server as String, "/")}scans?$query"
}

static String encodeURL(String url) {
    URLEncoder.encode(url, 'UTF-8')
}

static String appendIfMissing(String str, String suffix) {
    str.endsWith(suffix) ? str : str + suffix
}

static boolean isGitInstalled() {
    try {
        "git --version".execute().waitFor() == 0
    } catch (IOException ignored) {
        false
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
