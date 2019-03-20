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
    def "snippet #snippet can be configured"() {
        given:
        settingsFile()
        buildFileWithInlinedSnippet(scriptSnippet("${snippet}.gradle"))

        when:
        def result = build('help', '--no-scan')

        then:
        result.task(':help').outcome == SUCCESS

        where:
        snippet << [
            'publishing-basic',
            'publishing-ge',
            'capture-task-input-files',
            'tags-basic',
            'tags-android',
            'tags-slow-tasks',
            'ci-jenkins',
            'ci-teamcity',
        ]
    }

    @Unroll
    def "snippet #snippet, which does background work, can be configured"() {
        given:
        settingsFile()
        buildFileWithInlinedSnippet(scriptSnippet("${snippet}.gradle"))
        initGitRepo()

        when:
        def result = build('help', '-Dcom.gradle.scan.server=https://scans.gradle.com')

        then:
        result.task(':help').outcome == SUCCESS
        !result.output.contains('Build scan background action failed')

        where:
        snippet << [
            'git-commit-id',
            'git-branch-name',
            'git-status',
            'git-source',
            'git-commit-scans',
            'git-all',
            'gist',
        ]
    }

    @Requires({ !System.getProperty('gistToken').isEmpty() })
    def "gists can be published"() {
        given:
        settingsFile()
        buildFileWithInlinedSnippet(scriptSnippet('gist.gradle'))
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
        buildFileWithInlinedSnippet(scriptSnippet('gist.gradle'))
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

    private File buildFileWithInlinedSnippet(def snippet) {
        buildFile << """
        plugins {
            id 'com.gradle.build-scan' version '2.2.1'
        }
        
        $snippet
        """
    }

}
