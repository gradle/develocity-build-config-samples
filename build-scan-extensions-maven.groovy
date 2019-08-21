import org.apache.commons.lang3.StringUtils

def buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')

buildScan.tag(isOnCi() ? 'CI' : 'LOCAL')

def os = System.getProperty('os.name')
if (os) {
    buildScan.tag os
}

if (project.hasProperty('android.injected.invoked.from.ide')) {
    buildScan.tag 'Android Studio'
} else if (project.hasProperty('idea.executable')) {
    buildScan.tag 'IDEA'
} // TODO Eclipse?


buildScan.background {
    def gitCommitId = StringUtils.chomp('git rev-parse --short=8 --verify HEAD'.execute().text)
    def gitBranchName = StringUtils.chomp('git rev-parse --abbrev-ref HEAD'.execute().text)
    def gitStatus = StringUtils.chomp('git status --porcelain'.execute().text)

    if (gitBranchName && !gitBranchName.isEmpty() && gitBranchName != 'HEAD') {
        it.tag gitBranchName
        it.value 'Git branch', gitBranchName
    }
    if (gitStatus && !gitStatus.isEmpty()) {
        it.tag 'dirty'
        it.value 'Git status', gitStatus
    }

    def commitIdLabel = 'Git Commit ID'
    it.value commitIdLabel, gitCommitId
    it.link 'Git commit scans', customValueSearchUrl(it, [(commitIdLabel): gitCommitId])
}

def ciBuild = 'CI BUILD'

// Jenkins
if (System.getenv('BUILD_NUMBER')) {
    buildScan.value 'Build number', System.getenv('BUILD_NUMBER')
}
if (System.getenv('JOB_NAME')) {
    buildScan.value 'Job name', System.getenv('JOB_NAME')
}
if (System.getenv('BUILD_URL')) {
    buildScan.link ciBuild, System.getenv('BUILD_URL')
}

// Team City
if (System.getenv('CI_BUILD_URL')) {
    buildScan.link ciBuild, System.getenv('CI_BUILD_URL')
}

// Circle CI
if (System.getenv('CIRCLE_BUILD_URL')) {
    buildScan.link ciBuild, System.getenv('CIRCLE_BUILD_URL')
}

// Bamboo
if (System.getenv('bamboo.resultsUrl')) {
    buildScan.link ciBuild, System.getenv('bamboo.resultsUrl')
}

// Concourse
// https://concourse-ci.org/implementing-resource-types.html#resource-metadata
// https://stackoverflow.com/a/45037360/2740621
// Issue filed on Concourse docs github issue tracker: https://github.com/concourse/docs/issues/240
def concourseAtcExternalUrl = System.getenv('ATC_EXTERNAL_URL')
def concourseBuildTeamName = System.getenv('BUILD_TEAM_NAME')
def concourseBuildPipelineName = System.getenv('BUILD_PIPELINE_NAME')
def concourseBuildJobName = System.getenv('BUILD_JOB_NAME')
def concourseBuildName = System.getenv('BUILD_NAME')
if (concourseAtcExternalUrl && concourseBuildTeamName && concourseBuildPipelineName && concourseBuildJobName && concourseBuildName) {
    String url = "$concourseAtcExternalUrl/teams/$concourseBuildTeamName/pipelines/$concourseBuildPipelineName/jobs/$concourseBuildJobName/builds/$concourseBuildName"
    try {
        buildScan.link ciBuild, url
    } catch (Exception ignored) {
        // Because I don't trust this URL to be correct and I don't want to throw an exception when trying to add it
        project.logger.warn("Cannot add link to Concourse CI job. $url is not a valid url.")
    }
}

static boolean isOnCi() {
    return 'true' == System.getenv('CI')
}

String customValueSearchUrl(buildScan, Map<String, String> search) {
    def query = search.collect { name, value ->
        "search.names=${name.urlEncode()}&search.values=${value.urlEncode()}"
    }.join('&')

    return "$buildScan.server/scans?$query"
}

// An extension method on String
String.metaClass.urlEncode = { -> URLEncoder.encode(delegate as String, 'UTF-8') }
