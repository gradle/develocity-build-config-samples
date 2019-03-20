package org.gradle.functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Requires
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
    private String snippetsDir = new File('..').absolutePath

    def setup() {
        settingsFile = testProjectDir.newFile('settings.gradle')
    }

    @Unroll
    def "snippet #snippet can be configured for the #dsl DSL"() {
        given:
        settingsFile()
        def isGroovy = dsl == 'groovy'
        def fileName = "${snippet}.${isGroovy ? 'gradle' : 'gradle.kts'}"
        buildFileWithInlinedSnippet(scriptSnippet(fileName), isGroovy)

        when:
        def result = build('help', '--no-scan')

        then:
        result.task(':help').outcome == SUCCESS

        where:
        snippet                    | dsl
        'publishing-basic'         | 'groovy'
        'publishing-basic'         | 'kotlin'
        'publishing-ge'            | 'groovy'
        'publishing-ge'            | 'kotlin'
        'capture-task-input-files' | 'groovy'
        'capture-task-input-files' | 'kotlin'
        'tags-basic'               | 'groovy'
        'tags-basic'               | 'kotlin'
        'tags-android'             | 'groovy'
        'tags-android'             | 'kotlin'
        'tags-slow-tasks'          | 'groovy'
        'tags-slow-tasks'          | 'kotlin'
        'ci-jenkins'               | 'groovy'
        'ci-jenkins'               | 'kotlin'
        'ci-teamcity'              | 'groovy'
        'ci-teamcity'              | 'kotlin'

    }

    @Unroll
    def "snippet #snippet, which does background work, can be configured for the #dsl DSL"() {
        given:
        settingsFile()
        def isGroovy = dsl == 'groovy'
        def fileName = "${snippet}.${isGroovy ? 'gradle' : 'gradle.kts'}"
        buildFileWithInlinedSnippet(scriptSnippet(fileName), isGroovy)
        initGitRepo()

        when:
        def result = build('help', '-Dcom.gradle.scan.server=https://scans.gradle.com')

        then:
        result.task(':help').outcome == SUCCESS
        !result.output.contains('Build scan background action failed')

        where:
        snippet            | dsl
        'git-commit-id'    | 'groovy'
        'git-commit-id'    | 'kotlin'
        'git-branch-name'  | 'groovy'
        'git-branch-name'  | 'kotlin'
        'git-status'       | 'groovy'
        'git-status'       | 'kotlin'
        'git-source'       | 'groovy'
        'git-source'       | 'kotlin'
        'git-commit-scans' | 'groovy'
        'git-commit-scans' | 'kotlin'
        'git-all'          | 'groovy'
        'git-all'          | 'kotlin'
        'gist'             | 'groovy'
        'gist'             | 'kotlin'
    }

    @Requires({ !System.getProperty('gistToken').isEmpty() })
    def "gists can be published for the #dsl DSL"() {
        given:
        settingsFile()
        def isGroovy = dsl == 'groovy'
        def fileName = "gist.${isGroovy ? 'gradle' : 'gradle.kts'}"
        buildFileWithInlinedSnippet(scriptSnippet(fileName), isGroovy)
        initGitRepo()

        and: 'A modified file so there is a diff when generating a gist'
        buildFile << "\n\n"

        when:
        def result = build('help', "-PgistToken=${System.getProperty('gistToken')}", '--info')

        then:
        result.task(':help').outcome == SUCCESS
        result.output.contains('Successfully published gist.')

        where:
        dsl << ['groovy', 'kotlin']
    }

    @Unroll
    def "gists will not be published if #startParameter for the #dsl DSL"() {
        given:
        settingsFile()
        def isGroovy = dsl == 'groovy'
        def fileName = "gist.${isGroovy ? 'gradle' : 'gradle.kts'}"
        buildFileWithInlinedSnippet(scriptSnippet(fileName), isGroovy)
        initGitRepo()

        and: 'A modified file so there is a diff when generating a gist'
        buildFile << "\n\n"

        when:
        def result = build('help', startParameter)

        then:
        result.task(':help').outcome == SUCCESS
        result.output.contains("Build is offline or continuous. Not publishing gist.")

        where:
        startParameter | dsl
        '--offline'    | 'groovy'
        '--offline'    | 'kotlin'
        '--continuous' | 'groovy'
        '--continuous' | 'kotlin'
    }

    private File buildFileWithInlinedSnippet(def snippet, boolean isGroovy = true) {
        def buildFileName = isGroovy ? 'build.gradle' : 'build.gradle.kts'
        buildFile = testProjectDir.newFile(buildFileName)
        buildFile << """
        plugins {
            id("com.gradle.build-scan") version "2.2.1"
        }
        
        $snippet
        """
    }

    private File settingsFile() {
        settingsFile << "rootProject.name = 'scan-snippets-test'"
    }

    private def scriptSnippet(String snippetName) {
        return new File(snippetsDir, snippetName).text
    }

    private void initGitRepo() {
        executeCommandInTempDir("git init")
        executeCommandInTempDir("git config user.email 'dev-null@gradle.com'")
        executeCommandInTempDir("git config user.name 'Dev Null'")
        executeCommandInTempDir("git add .")
        executeCommandInTempDir("git commit -m 'Hello'")
    }

    private void executeCommandInTempDir(String command) {
        command.execute([], testProjectDir.root).with {
            waitFor()
            println "$command exit value: ${exitValue()}"
        }
    }

    private BuildResult build(String... args) {
        return gradleRunnerFor(*args).build()
    }

    private GradleRunner gradleRunnerFor(String... args) {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(*args + '--stacktrace')
            .forwardOutput()
    }
}
