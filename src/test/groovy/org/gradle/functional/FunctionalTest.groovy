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
    def "snippet can be configured"() {
        given:
        settingsFile << "rootProject.name = 'scan-snippets-test'"
        buildFile << """
        plugins {
            id 'com.gradle.build-scan' version '2.2.1'
        }
        
        apply from: '$snippet'
    """

        when:
        def result = build('help')

        then:
        result.task(':help').outcome == SUCCESS

        where:
        snippet << [
            scriptSnippet('basic-publishing.gradle'),
            scriptSnippet('capture-task-input-files.gradle'),
            scriptSnippet('git.gradle'),
            scriptSnippet('gist.gradle'),
            scriptSnippet('tags-android.gradle'),
            scriptSnippet('tags-basic.gradle')
        ]
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
    }
}
