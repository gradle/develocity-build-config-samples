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

        when:
        def result = build('help')

        then:
        result.task(':help').outcome == SUCCESS
        !result.output.contains('Build scan background action failed')

        // TODO: git and gist have issues. git needs SSL credentials (or maybe the temp dir isn't a git repo?), and gist just does nothing because it has a credentials check
        where:
        snippetTitle | snippet
        'git'        | scriptSnippet('git.gradle')
        'gist'       | scriptSnippet('gist.gradle')
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
