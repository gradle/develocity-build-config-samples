package org.gradle.functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class FunctionalTest extends Specification {

    @Rule
    TemporaryFolder testProjectDir

    private File settingsFile
    private File buildFile

    @Shared
    private String snippetsDir = new File('snippets').absolutePath

    def setup() {
        settingsFile = testProjectDir.newFile('settings.gradle')
        buildFile = testProjectDir.newFile('build.gradle')
    }

    @Unroll
    def "snippet #snippetTitle can be configured"() {
        given:
        settingsFile()
        buildFileWithAppliedSnippet(snippet)

        when:
        def result = build('help')

        then:
        result.task(':help').outcome == SUCCESS

        // TODO: basic-publishing actually does publish a build scan. Is that a problem?
        where:
        snippetTitle               | snippet
        'basic-publishing'         | scriptSnippet('basic-publishing.gradle')
        'capture-task-input-files' | scriptSnippet('capture-task-input-files.gradle')
        'tags-android'             | scriptSnippet('tags-android.gradle')
        'tags-basic'               | scriptSnippet('tags-basic.gradle')
    }

    @Unroll
    def "snippet #snippetTitle, which does background work, can be configured"() {
        given:
        settingsFile()
        buildFileWithAppliedSnippet(snippet)
        initGitRepo()

        when:
        def result = build('help', '-DgeServer=https://scans.gradle.com')

        then:
        result.task(':help').outcome == SUCCESS
        !result.output.contains('Build scan background action failed')

        where:
        snippetTitle | snippet
        'git'        | scriptSnippet('git.gradle')
        'gist'       | scriptSnippet('gist.gradle')
    }

    def "gists can be published"() {
        given:
        settingsFile()
        buildFileWithAppliedSnippet(scriptSnippet('gist.gradle'))

        when:
        def result = build('help')

        then:
        result.task(':help').outcome == SUCCESS
        !result.output.contains("User has not set 'gistUsername' or 'gistToken'. Cannot publish gist.")
    }

    private File settingsFile() {
        settingsFile << "rootProject.name = 'scan-snippets-test'"
    }

    private File buildFileWithAppliedSnippet(def snippet) {
        buildFile << """
        plugins {
            id 'com.gradle.build-scan' version '2.2.1'
        }
        
        apply from: '$snippet'
        """
    }

    private def scriptSnippet(String snippetName) {
        return new File(snippetsDir, snippetName).toString()
    }

    private initGitRepo() {
        executeCommandInTempDir("git init")
        executeCommandInTempDir("git config user.email 'dev-null@gradle.com'")
        executeCommandInTempDir("git config user.name 'Dev Null'")
        executeCommandInTempDir("touch temp")
        executeCommandInTempDir("git add .")
        executeCommandInTempDir("git commit -m 'Hello'")
        executeCommandInTempDir("git status")
    }

    private void executeCommandInTempDir(String command) {
        def p = command.execute([], testProjectDir.root)
        p.waitFor()
        println "$command exit value: ${p.exitValue()}"
    }

    private BuildResult build(String... args) {
        return gradleRunnerFor(*args).build()
    }

    private BuildResult buildAndFail(String... args) {
        return gradleRunnerFor(*args).buildAndFail()
    }

    private GradleRunner gradleRunnerFor(String... args) {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(*args + '--stacktrace')
            .forwardOutput()
    }
}
