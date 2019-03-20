package org.gradle.functional

import spock.lang.Requires
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class FunctionalGDSLTest extends AbstractFunctionalTest {

    def setup() {
        settingsFile = testProjectDir.newFile('settings.gradle')
        buildFile = testProjectDir.newFile('build.gradle')
    }

    @Unroll
    def "snippet #snippetTitle can be configured"() {
        given:
        settingsFile()
        buildFileWithAppliedSnippet(scriptSnippet2(snippet))

        when:
        def result = build('help', '--no-scan')

        then:
        result.task(':help').outcome == SUCCESS

        where:
        snippetTitle               | snippet
        'publishing-basic'         | 'publishing-basic.gradle'
        'publishing-ge'            | 'publishing-ge.gradle'
        'capture-task-input-files' | 'capture-task-input-files.gradle'
        'tags-basic'               | 'tags-basic.gradle'
        'tags-android'             | 'tags-android.gradle'
        'tags-slow-tasks'          | 'tags-slow-tasks.gradle'
        'ci-jenkins'               | 'ci-jenkins.gradle'
        'ci-teamcity'              | 'ci-teamcity.gradle'
    }

    @Unroll
    def "snippet #snippetTitle, which does background work, can be configured"() {
        given:
        settingsFile()
        buildFileWithAppliedSnippet(scriptSnippet2(snippet))
        initGitRepo()

        when:
        def result = build('help', '-Dcom.gradle.scan.server=https://scans.gradle.com')

        then:
        result.task(':help').outcome == SUCCESS
        !result.output.contains('Build scan background action failed')

        where:
        snippetTitle       | snippet
        'git-commit-id'    | 'git-commit-id.gradle'
        'git-branch-name'  | 'git-branch-name.gradle'
        'git-status'       | 'git-status.gradle'
        'git-source'       | 'git-source.gradle'
        'git-commit-scans' | 'git-commit-scans.gradle'
        'git-all'          | 'git-all.gradle'
        'gist'             | 'gist.gradle'
    }

    @Requires({ !System.getProperty('gistToken').isEmpty() })
    def "gists can be published"() {
        given:
        settingsFile()
        buildFileWithAppliedSnippet(scriptSnippet2('gist.gradle'))
        initGitRepo()

        and: 'A modified file so there is a diff when generating a gist'
        buildFile << "\n\n"

        when:
        def result = build('help', "-PgistToken=${System.getProperty('gistToken')}", '--info')

        then:
        result.task(':help').outcome == SUCCESS
        result.output.contains('Successfully published gist.')
    }

    @Unroll
    def "gists won't be published if #startParameter"() {
        given:
        settingsFile()
        buildFileWithAppliedSnippet(scriptSnippet2('gist.gradle'))
        initGitRepo()

        and: 'A modified file so there is a diff when generating a gist'
        buildFile << "\n\n"

        when:
        def result = build('help', startParameter)

        then:
        result.task(':help').outcome == SUCCESS
        result.output.contains("Build is offline or continuous. Not publishing gist.")

        where:
        startParameter << ['--offline', '--continuous']
    }

    private File buildFileWithAppliedSnippet(def snippet) {
        buildFile << """
        plugins {
            id 'com.gradle.build-scan' version '2.2.1'
        }
        
        apply from: '$snippet'
        """
    }

    private def scriptSnippet2(String snippetName) {
        return new File(snippetsDir, snippetName).toString()
    }
}
