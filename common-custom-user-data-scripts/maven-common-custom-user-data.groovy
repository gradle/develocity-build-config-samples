import com.gradle.maven.extension.api.scan.BuildScanApi
import org.apache.maven.Maven

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

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

BuildScanApi buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')
if (!buildScan) {
    return
}

buildScan.executeOnce('common-custom-user-data') { BuildScanApi api ->
    tagOs(api)
    tagIde(api)
    tagCiOrLocal(api)
    addCiMetadata(api)
    addGitMetadata(api)
}

// Add here other scripts, if needed
// evaluate(new File("${session.request.multiModuleProjectDirectory}/<<other-script.groovy>>"))

static void tagOs(def buildScan) {
    buildScan.tag System.getProperty('os.name')
}

static void tagIde(def buildScan) {
    if (System.getProperty('idea.version')) {
        buildScan.tag 'IntelliJ IDEA'
    } else if (System.getProperty('eclipse.buildId')) {
        buildScan.tag 'Eclipse'
    } else if (!isCi()) {
        buildScan.tag 'Cmd Line'
    }
}

static void tagCiOrLocal(def buildScan) {
    buildScan.tag isCi() ? 'CI' : 'LOCAL'
}

static void addCiMetadata(def buildScan) {
    if (isJenkins()) {
        if (System.getenv('BUILD_URL')) {
            buildScan.link 'Jenkins build', System.getenv('BUILD_URL')
        }
        if (System.getenv('BUILD_NUMBER')) {
            buildScan.value 'CI build number', System.getenv('BUILD_NUMBER')
        }
        if (System.getenv('NODE_NAME')) {
            def nodeNameLabel = 'CI node'
            def nodeName = System.getenv('NODE_NAME')
            buildScan.value nodeNameLabel, nodeName
            addCustomLinkWithSearchTerms buildScan, 'CI node build scans', [(nodeNameLabel): nodeName]
        }
        if (System.getenv('JOB_NAME')) {
            def jobNameLabel = 'CI job'
            def jobName = System.getenv('JOB_NAME')
            buildScan.value jobNameLabel, jobName
            addCustomLinkWithSearchTerms buildScan, 'CI job build scans', [(jobNameLabel): jobName]
        }
        if (System.getenv('STAGE_NAME')) {
            def stageNameLabel = 'CI stage'
            def stageName = System.getenv('STAGE_NAME')
            buildScan.value stageNameLabel, stageName
            addCustomLinkWithSearchTerms buildScan, 'CI stage build scans', [(stageNameLabel): stageName]
        }
    }

    if (isTeamCity()) {
        if (System.getProperty('teamcity.configuration.properties.file')) {
            def properties = readPropertiesFile(System.getProperty('teamcity.configuration.properties.file'))
            def teamCityServerUrl = properties.getProperty('teamcity.serverUrl')
            if (teamCityServerUrl && System.getProperty('build.number') && System.getProperty('teamcity.buildType.id')) {
                def buildNumber = System.getProperty('build.number')
                def buildTypeId = System.getProperty('teamcity.buildType.id')
                buildScan.link 'TeamCity build', "${appendIfMissing(teamCityServerUrl, '/')}viewLog.html?buildNumber=${buildNumber}&buildTypeId=${buildTypeId}"
            }
        }
        if (System.getProperty('build.number')) {
            buildScan.value 'CI build number', System.getProperty('build.number')
        }
        if (System.getProperty('agent.name')) {
            def agentNameLabel = 'CI agent'
            def agentName = System.getProperty('agent.name')
            buildScan.value agentNameLabel, agentName
            addCustomLinkWithSearchTerms buildScan, 'CI agent build scans', [(agentNameLabel): agentName]
        }
    }

    if (isCircleCI()) {
        if (System.getenv('CIRCLE_BUILD_URL')) {
            buildScan.link 'CircleCI build', System.getenv('CIRCLE_BUILD_URL')
        }
        if (System.getenv('CIRCLE_BUILD_NUM')) {
            buildScan.value 'CI build number', System.getenv('CIRCLE_BUILD_NUM')
        }
        if (System.getenv('CIRCLE_JOB')) {
            def jobNameLabel = 'CI job'
            def jobName = System.getenv('CIRCLE_JOB')
            buildScan.value jobNameLabel, jobName
            addCustomLinkWithSearchTerms buildScan, 'CI job build scans', [(jobNameLabel): jobName]
        }
        if (System.getenv('CIRCLE_WORKFLOW_ID')) {
            def workflowIdLabel = 'CI workflow'
            def workflowId = System.getenv('CIRCLE_WORKFLOW_ID')
            buildScan.value workflowIdLabel, workflowId
            addCustomLinkWithSearchTerms buildScan, 'CI workflow build scans', [(workflowIdLabel): workflowId]
        }
    }

    if (isBamboo()) {
        if (System.getenv('bamboo_resultsUrl')) {
            buildScan.link 'Bamboo build', System.getenv('bamboo_resultsUrl')
        }
        if (System.getenv('bamboo_buildNumber')) {
            buildScan.value 'CI build number', System.getenv('bamboo_buildNumber')
        }
        if (System.getenv('bamboo_planName')) {
            def planNameLabel = 'CI plan'
            def planName = System.getenv('bamboo_planName')
            buildScan.value planNameLabel, planName
            addCustomLinkWithSearchTerms buildScan, 'CI plan build scans', [(planNameLabel): planName]
        }
        if (System.getenv('bamboo_buildPlanName')) {
            def buildPlanNameLabel = 'CI build plan'
            def buildPlanName = System.getenv('bamboo_buildPlanName')
            buildScan.value buildPlanNameLabel, buildPlanName
            addCustomLinkWithSearchTerms buildScan, 'CI build plan build scans', [(buildPlanNameLabel): buildPlanName]
        }
        if (System.getenv('bamboo_agentId')) {
            def agentIdLabel = 'CI agent';
            def agentId = System.getenv('bamboo_agentId')
            buildScan.value agentIdLabel, agentId
            addCustomLinkWithSearchTerms buildScan, 'CI agent build scans', [(agentIdLabel): agentId]
        }
    }

    if (isGitHubActions()) {
        if (System.getenv('GITHUB_REPOSITORY') && System.getenv('GITHUB_RUN_ID')) {
            buildScan.link 'GitHub Actions build', "https://github.com/${System.getenv('GITHUB_REPOSITORY')}/actions/runs/${System.getenv('GITHUB_RUN_ID')}"
        }
        if (System.getenv('GITHUB_WORKFLOW')) {
            def workflowNameLabel = 'GitHub workflow'
            def workflowName = System.getenv('GITHUB_WORKFLOW')
            buildScan.value workflowNameLabel, workflowName
            addCustomLinkWithSearchTerms buildScan, 'GitHub workflow build scans', [(workflowNameLabel): workflowName]
        }
    }

    if (isGitLab()) {
        if (System.getenv('CI_JOB_URL')) {
            buildScan.link 'GitLab build', System.getenv('CI_JOB_URL')
        }
        if (System.getenv('CI_PIPELINE_URL')) {
            buildScan.link 'GitLab pipeline', System.getenv('CI_PIPELINE_URL')
        }
        if (System.getenv('CI_JOB_NAME')) {
            def jobNameLabel = 'CI job'
            def jobName = System.getenv('CI_JOB_NAME')
            buildScan.value jobNameLabel, jobName
            addCustomLinkWithSearchTerms buildScan, 'CI job build scans', [(jobNameLabel): jobName]
        }
        if (System.getenv('CI_JOB_STAGE')) {
            def stageNameLabel = 'CI stage'
            def stageName = System.getenv('CI_JOB_STAGE')
            buildScan.value stageNameLabel, stageName
            addCustomLinkWithSearchTerms buildScan, 'CI stage build scans', [(stageNameLabel): stageName]
        }
    }

    if (isTravis()) {
        if (System.getenv('TRAVIS_BUILD_WEB_URL')) {
            buildScan.link 'TRAVIS build', System.getenv('TRAVIS_BUILD_WEB_URL')
        }
        if (System.getenv('TRAVIS_BUILD_NUMBER')) {
            buildScan.value 'TRAVIS build number', System.getenv('TRAVIS_BUILD_NUMBER')
        }
        if (System.getenv('TRAVIS_EVENT_TYPE')) {
            buildScan.tag System.getenv('TRAVIS_EVENT_TYPE')
        }
        if (System.getenv('TRAVIS_TAG')) {
            buildScan.value 'TRAVIS tag', System.getenv('TRAVIS_TAG')
        }
        if (System.getenv('TRAVIS_JOB_NAME')) {
            def jobNameLabel = 'TRAVIS job'
            def jobName = System.getenv('TRAVIS_JOB_NAME')
            buildScan.value jobNameLabel, jobName
            addCustomLinkWithSearchTerms buildScan, 'TRAVIS job build scans', [(jobNameLabel): jobName]
        }
    }
}

static void addGitMetadata(def buildScan) {
    buildScan.background { BuildScanApi api ->
        if (!isGitInstalled()) {
            return
        }

        def gitCommitId = execAndGetStdout('git', 'rev-parse', '--short=8', '--verify', 'HEAD')
        def gitBranchName = execAndGetStdout('git', 'rev-parse', '--abbrev-ref', 'HEAD')
        def gitStatus = execAndGetStdout('git', 'status', '--porcelain')

        if (gitCommitId) {
            def gitCommitIdLabel = 'Git commit id'
            api.value gitCommitIdLabel, gitCommitId
            addCustomLinkWithSearchTerms api, 'Git commit id build scans', [(gitCommitIdLabel): gitCommitId]

            def originUrl = execAndGetStdout('git', 'config', '--get', 'remote.origin.url')
            if (originUrl) {
                if (originUrl.contains('github.com/') || originUrl.contains('github.com:')) {
                    def rawRepoPath = (originUrl =~ /(.*)github\.com[\/|:](.*)/)[0][2]
                    def repoPath = rawRepoPath.endsWith('.git') ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath
                    api.link 'Github Source', "https://github.com/$repoPath/tree/$gitCommitId"
                } else if (originUrl.contains('gitlab.com/') || originUrl.contains('gitlab.com:')) {
                    def rawRepoPath = (originUrl =~ /(.*)gitlab\.com[\/|:](.*)/)[0][2]
                    def repoPath = rawRepoPath.endsWith('.git') ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath
                    api.link 'GitLab Source', "https://gitlab.com/$repoPath/-/commit/$gitCommitId"
                }
            }
        }
        if (gitBranchName) {
            api.tag gitBranchName
            api.value 'Git branch', gitBranchName
        }
        if (gitStatus) {
            api.tag 'Dirty'
            api.value 'Git status', gitStatus
        }
    }
}

static boolean isCi() {
    isJenkins() || isTeamCity() || isCircleCI() || isBamboo() || isGitHubActions() || isGitLab() || isTravis()
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

static boolean isGitHubActions() {
    System.getenv('GITHUB_ACTIONS')
}

static boolean isGitLab() {
    System.getenv('GITLAB_CI')
}

static boolean isTravis() {
    System.getenv('TRAVIS_JOB_ID')
}

static boolean isGitInstalled() {
    Process process
    try {
        process = 'git --version'.execute()
        def finished = process.waitFor(10, TimeUnit.SECONDS)
        finished && process.exitValue() == 0;
    } catch (IOException ignored) {
        false
    } finally {
        if (process) {
            process.destroyForcibly()
        }
    }
}

static String execAndGetStdout(String... args) {
    Process process = args.toList().execute()
    try {
        def standardText = process.getInputStream().getText(Charset.defaultCharset().name())
        def ignore = process.getErrorStream().getText(Charset.defaultCharset().name())

        def finished = process.waitFor(10, TimeUnit.SECONDS)
        finished && process.exitValue() == 0 ? trimAtEnd(standardText) : null
    } finally {
        process.destroyForcibly()
    }
}

static void addCustomLinkWithSearchTerms(def buildScan, String title, Map<String, String> search) {
    def server = buildScan.server
    if (server) {
        String searchParams = customValueSearchParams(search);
        String url = "${appendIfMissing(server, '/')}scans?$searchParams#selection.buildScanB=${urlEncode('{SCAN_ID}')}"
        buildScan.link title, url
    }
}

static String customValueSearchParams(Map<String, String> search) {
    search.collect { name, value ->
        "search.names=${urlEncode(name)}&search.values=${urlEncode(value)}"
    }.join('&')
}

static String appendIfMissing(String str, String suffix) {
    str.endsWith(suffix) ? str : str + suffix
}

static String trimAtEnd(String str) {
    ('x' + str).trim().substring(1)
}

static String urlEncode(String url) {
    URLEncoder.encode(url, 'UTF-8')
}

static Properties readPropertiesFile(String name) {
    Properties properties = new Properties()
    File file = new File(name)
    file.withInputStream {
        properties.load it
    }
    properties
}

