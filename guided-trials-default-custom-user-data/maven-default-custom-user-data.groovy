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

def buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')

buildScan.executeOnce('custom-data') { api ->
    tagOs(api)
    tagIde(api)
    tagCiOrLocal(api)
    addCiMetadata(api)
    addGitMetadata(api)
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
    } else if (!isCi()) {
        api.tag 'Cmd Line'
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
        def teamCityConfigurationFileProp = 'teamcity.configuration.properties.file'
        if (System.getProperty(teamCityConfigurationFileProp)) {
            def properties = new Properties()
            properties.load(new FileInputStream(System.getProperty(teamCityConfigurationFileProp)))
            def teamCityServerUrl = properties.getProperty("teamcity.serverUrl")
            if (teamCityServerUrl && System.getProperty('build.number') && System.getProperty('teamcity.buildType.id')) {
                def teamCityBuildNumber = System.getProperty('build.number')
                def teamCityBuildTypeId = System.getProperty('teamcity.buildType.id')
                api.link 'TeamCity build', "${appendIfMissing(teamCityServerUrl, '/')}viewLog.html?buildNumber=${teamCityBuildNumber}&buildTypeId=${teamCityBuildTypeId}"
            }
        }
        if (System.getProperty('build.number')) {
            api.value 'CI build number', System.getProperty('build.number')
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
                def repoPath = (originUrl =~ /(.*)github\.com[\/|:](.*)/)[0][2]
                if (repoPath.endsWith('.git')) {
                    repoPath = repoPath.substring(0, repoPath.length() - 4)
                }
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
    trimAtEnd(exec.text)
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
    "${appendIfMissing(api.server, '/')}scans?$query#selection.buildScanB=%7BSCAN_ID%7D"
}

static String encodeURL(String url) {
    URLEncoder.encode(url, 'UTF-8')
}

static boolean isGitInstalled() {
    try {
        'git --version'.execute().waitFor() == 0
    } catch (IOException ignored) {
        false
    }
}

static String appendIfMissing(String str, String suffix) {
    str.endsWith(suffix) ? str : str + suffix
}

static String trimAtEnd(String str) {
    ('x' + str).trim().substring(1)
}
