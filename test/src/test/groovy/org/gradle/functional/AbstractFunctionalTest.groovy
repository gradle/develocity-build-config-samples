package org.gradle.functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

abstract class AbstractFunctionalTest extends Specification {

    @Rule
    TemporaryFolder testProjectDir

    protected File settingsFile
    protected File buildFile

    @Shared
    private String snippetsDir = new File('..').absolutePath

    protected File settingsFile() {
        settingsFile << "rootProject.name = 'scan-snippets-test'"
    }

    protected def scriptSnippet(String snippetName) {
        return new File(snippetsDir, snippetName).text
    }

    protected void initGitRepo() {
        executeCommandInTempDir("git init")
        executeCommandInTempDir("git config user.email 'dev-null@gradle.com'")
        executeCommandInTempDir("git config user.name 'Dev Null'")
        executeCommandInTempDir("git add .")
        executeCommandInTempDir("git commit -m 'Hello'")
    }

    protected void executeCommandInTempDir(String command) {
        command.execute([], testProjectDir.root).with {
            waitFor()
            println "$command exit value: ${exitValue()}"
        }
    }

    protected BuildResult build(String... args) {
        return gradleRunnerFor(*args).build()
    }

    protected GradleRunner gradleRunnerFor(String... args) {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(*args + '--stacktrace')
            .forwardOutput()
    }
}
