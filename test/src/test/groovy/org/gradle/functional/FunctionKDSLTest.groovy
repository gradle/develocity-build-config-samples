package org.gradle.functional

import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class FunctionKDSLTest extends AbstractFunctionalTest {

    def setup() {
        settingsFile = testProjectDir.newFile('settings.gradle')
        buildFile = testProjectDir.newFile('build.gradle.kts')
    }

    @Unroll
    def "kotlin DSL snippet #snippetTitle can be configured"() {
        given:
        settingsFile()
        buildFileWithInlinedSnippet(snippet)

        when:
        def result = build('help', '--no-scan')

        then:
        result.task(':help').outcome == SUCCESS

        where:
        snippetTitle               | snippet
        'publishing-basic'         | scriptSnippet('publishing-basic.gradle.kts')
        'publishing-ge'            | scriptSnippet('publishing-ge.gradle.kts')
        'capture-task-input-files' | scriptSnippet('capture-task-input-files.gradle.kts')
        'tags-basic'               | scriptSnippet('tags-basic.gradle.kts')
        'tags-android'             | scriptSnippet('tags-android.gradle.kts')
        'tags-slow-tasks'          | scriptSnippet('tags-slow-tasks.gradle.kts')
        'ci-jenkins'               | scriptSnippet('ci-jenkins.gradle.kts')
        'ci-teamcity'              | scriptSnippet('ci-teamcity.gradle.kts')
    }

    @Unroll
    def "snippet #snippetTitle, which does background work, can be configured"() {
        given:
        settingsFile()
        buildFileWithInlinedSnippet(snippet)
        initGitRepo()

        when:
        def result = build('help', '-Dcom.gradle.scan.server=https://scans.gradle.com')

        then:
        result.task(':help').outcome == SUCCESS
        !result.output.contains('Build scan background action failed')

        where:
        snippetTitle       | snippet
        'git-commit-id'    | scriptSnippet('git-commit-id.gradle.kts')
        'git-branch-name'  | scriptSnippet('git-branch-name.gradle.kts')
        'git-status'       | scriptSnippet('git-status.gradle.kts')
        'git-source'       | scriptSnippet('git-source.gradle.kts')
        'git-commit-scans' | scriptSnippet('git-commit-scans.gradle.kts')
        'git-all'          | scriptSnippet('git-all.gradle.kts')
        'gist'             | scriptSnippet('gist.gradle.kts')
    }

//    @Requires({ !System.getProperty('gistToken').isEmpty() })
//    def "kotlin DSL gists can be published"() {
//        given:
//        settingsFile()
//        kotlinDSBbuildFileWithAppliedSnippet(scriptSnippet('gist.gradle.kts'))
//        initGitRepo()
//
//        and: 'A modified file so there is a diff when generating a gist'
//        kotlinBuildFile << "\n\n"
//
//        when:
//        def result = build('help', "-PgistToken=${System.getProperty('gistToken')}", '--info')
//
//        then:
//        result.task(':help').outcome == SUCCESS
//        result.output.contains('Successfully published gist.')
//    }

    private File buildFileWithInlinedSnippet(def snippet) {
        buildFile << """
        plugins {
            id("com.gradle.build-scan") version "2.2.1"
        }
        
        $snippet
        """
    }

}
