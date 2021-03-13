package com.gradle;

import com.gradle.scan.plugin.BuildScanExtension;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.testing.Test;

import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gradle.Utils.*;

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
final class CustomBuildScanEnhancements {

    static void configureBuildScan(BuildScanExtension buildScan, Gradle gradle) {
        captureOs(buildScan);
        captureIde(buildScan, gradle);
        captureCiOrLocal(buildScan);
        captureCiMetadata(buildScan, gradle);
        captureGitMetadata(buildScan);
        captureTestParallelization(buildScan, gradle);
    }

    private static Optional<String> projectProperty(Gradle gradle, String name) {
        String value = (String) gradle.getRootProject().findProperty(name);
        return Optional.ofNullable(value);
    }

    private static void captureOs(BuildScanExtension buildScan) {
        sysProperty("os.name").ifPresent(buildScan::tag);
    }

    private static void captureIde(BuildScanExtension buildScan, Gradle gradle) {
        // Wait for projects to load to ensure Gradle project properties are initialized
        gradle.projectsEvaluated(g -> {
            Project project = g.getRootProject();
            if (project.hasProperty("android.injected.invoked.from.ide")) {
                buildScan.tag("Android Studio");
                if (project.hasProperty("android.injected.studio.version")) {
                    buildScan.value("Android Studio version", String.valueOf(project.property("android.injected.studio.version")));
                }
            } else if (sysProperty("idea.version").isPresent() || sysPropertyKeyStartingWith("idea.version")) {
                buildScan.tag("IntelliJ IDEA");
            } else if (sysProperty("eclipse.buildId").isPresent()) {
                buildScan.tag("Eclipse");
            } else if (!isCi()) {
                buildScan.tag("Cmd Line");
            }
        });
    }

    private static void captureCiOrLocal(BuildScanExtension buildScan) {
        buildScan.tag(isCi() ? "CI" : "LOCAL");
    }

    private static void captureCiMetadata(BuildScanExtension buildScan, Gradle gradle) {
        if (isJenkins() || isHudson()) {
            envVariable("BUILD_URL").ifPresent(url ->
                    buildScan.link(isJenkins() ? "Jenkins build" : "Hudson build", url));
            envVariable("BUILD_NUMBER").ifPresent(value ->
                    buildScan.value("CI build number", value));
            envVariable("NODE_NAME").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI node", value));
            envVariable("JOB_NAME").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI job", value));
            envVariable("STAGE_NAME").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI stage", value));
        }

        if (isTeamCity()) {
            // Wait for projects to load to ensure Gradle project properties are initialized
            gradle.projectsEvaluated(g -> {
                Optional<String> teamCityConfigFile = projectProperty(gradle, "teamcity.configuration.properties.file");
                Optional<String> buildNumber = projectProperty(gradle, "build.number");
                Optional<String> buildTypeId = projectProperty(gradle, "teamcity.buildType.id");
                Optional<String> agentName = projectProperty(gradle, "agent.name");
                if (teamCityConfigFile.isPresent()
                        && buildNumber.isPresent()
                        && buildTypeId.isPresent()) {
                    Properties properties = readPropertiesFile(teamCityConfigFile.get());
                    String teamCityServerUrl = properties.getProperty("teamcity.serverUrl");
                    if (teamCityServerUrl != null) {
                        String buildUrl = appendIfMissing(teamCityServerUrl, "/") + "viewLog.html?buildNumber=" + buildNumber.get() + "&buildTypeId=" + buildTypeId.get();
                        buildScan.link("TeamCity build", buildUrl);
                    }
                }
                buildNumber.ifPresent(value ->
                        buildScan.value("CI build number", value));
                agentName.ifPresent(value ->
                        addCustomValueAndSearchLink(buildScan, "CI agent", value));

            });
        }

        if (isCircleCI()) {
            envVariable("CIRCLE_BUILD_URL").ifPresent(url ->
                    buildScan.link("CircleCI build", url));
            envVariable("CIRCLE_BUILD_NUM").ifPresent(value ->
                    buildScan.value("CI build number", value));
            envVariable("CIRCLE_JOB").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI job", value));
            envVariable("CIRCLE_WORKFLOW_ID").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI workflow", value));
        }

        if (isBamboo()) {
            envVariable("bamboo_resultsUrl").ifPresent(url ->
                    buildScan.link("Bamboo build", url));
            envVariable("bamboo_buildNumber").ifPresent(value ->
                    buildScan.value("CI build number", value));
            envVariable("bamboo_planName").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI plan", value));
            envVariable("bamboo_buildPlanName").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI build plan", value));
            envVariable("bamboo_agentId").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI agent", value));
        }

        if (isGitHubActions()) {
            Optional<String> gitHubRepository = envVariable("GITHUB_REPOSITORY");
            Optional<String> gitHubRunId = envVariable("GITHUB_RUN_ID");
            if (gitHubRepository.isPresent() && gitHubRunId.isPresent()) {
                buildScan.link("GitHub Actions build", "https://github.com/" + gitHubRepository.get() + "/actions/runs/" + gitHubRunId.get());
            }
            envVariable("GITHUB_WORKFLOW").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "GitHub workflow", value));
        }

        if (isGitLab()) {
            envVariable("CI_JOB_URL").ifPresent(url ->
                    buildScan.link("GitLab build", url));
            envVariable("CI_PIPELINE_URL").ifPresent(url ->
                    buildScan.link("GitLab pipeline", url));
            envVariable("CI_JOB_NAME").ifPresent(value1 ->
                    addCustomValueAndSearchLink(buildScan, "CI job", value1));
            envVariable("CI_JOB_STAGE").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI stage", value));
        }

        if (isTravis()) {
            envVariable("TRAVIS_BUILD_WEB_URL").ifPresent(url ->
                    buildScan.link("Travis build", url));
            envVariable("TRAVIS_BUILD_NUMBER").ifPresent(value ->
                    buildScan.value("CI build number", value));
            envVariable("TRAVIS_JOB_NAME").ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI job", value));
            envVariable("TRAVIS_EVENT_TYPE").ifPresent(buildScan::tag);
        }

        if (isBitrise()) {
            envVariable("BITRISE_BUILD_URL").ifPresent(url ->
                    buildScan.link("Bitrise build", url));
            envVariable("BITRISE_BUILD_NUMBER").ifPresent(value ->
                    buildScan.value("CI build number", value));
        }
    }

    private static boolean isCi() {
        return isGenericCI() || isJenkins() || isHudson() || isTeamCity() || isCircleCI() || isBamboo() || isGitHubActions() || isGitLab() || isTravis() || isBitrise();
    }

    private static boolean isGenericCI() {
        return envVariable("CI").isPresent() || sysProperty("CI").isPresent();
    }

    private static boolean isJenkins() {
        return envVariable("JENKINS_URL").isPresent();
    }

    private static boolean isHudson() {
        return envVariable("HUDSON_URL").isPresent();
    }

    private static boolean isTeamCity() {
        return envVariable("TEAMCITY_VERSION").isPresent();
    }

    private static boolean isCircleCI() {
        return envVariable("CIRCLE_BUILD_URL").isPresent();
    }

    private static boolean isBamboo() {
        return envVariable("bamboo_resultsUrl").isPresent();
    }

    private static boolean isGitHubActions() {
        return envVariable("GITHUB_ACTIONS").isPresent();
    }

    private static boolean isGitLab() {
        return envVariable("GITLAB_CI").isPresent();
    }

    private static boolean isTravis() {
        return envVariable("TRAVIS_JOB_ID").isPresent();
    }

    private static boolean isBitrise() {
        return envVariable("BITRISE_BUILD_URL").isPresent();
    }

    private static void captureGitMetadata(BuildScanExtension buildScan) {
        buildScan.background(api -> {
            if (!isGitInstalled()) {
                return;
            }

            String gitCommitId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD");
            String gitBranchName = execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD");
            String gitStatus = execAndGetStdOut("git", "status", "--porcelain");

            if (gitCommitId != null) {
                addCustomValueAndSearchLink(buildScan, "Git commit id", gitCommitId);

                String originUrl = execAndGetStdOut("git", "config", "--get", "remote.origin.url");
                if (isNotEmpty(originUrl)) {
                    if (originUrl.contains("github.com/") || originUrl.contains("github.com:")) {
                        Matcher matcher = Pattern.compile("(.*)github\\.com[/|:](.*)").matcher(originUrl);
                        if (matcher.matches()) {
                            String rawRepoPath = matcher.group(2);
                            String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                            api.link("Github source", "https://github.com/" + repoPath + "/tree/" + gitCommitId);
                        }
                    } else if (originUrl.contains("gitlab.com/") || originUrl.contains("gitlab.com:")) {
                        Matcher matcher = Pattern.compile("(.*)gitlab\\.com[/|:](.*)").matcher(originUrl);
                        if (matcher.matches()) {
                            String rawRepoPath = matcher.group(2);
                            String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                            api.link("GitLab Source", "https://gitlab.com/" + repoPath + "/-/commit/" + gitCommitId);
                        }
                    }
                }
            }
            if (isNotEmpty(gitBranchName)) {
                api.tag(gitBranchName);
                api.value("Git branch", gitBranchName);
            }
            if (isNotEmpty(gitStatus)) {
                api.tag("Dirty");
                api.value("Git status", gitStatus);
            }
        });
    }

    private static boolean isGitInstalled() {
        return execAndCheckSuccess("git", "--version");
    }

    private static void captureTestParallelization(BuildScanExtension buildScan, Gradle gradle) {
        gradle.allprojects(p ->
                p.getTasks().withType(Test.class).configureEach(test ->
                        test.doFirst(new Action<Task>() {
                            // use anonymous inner class to keep Test task instance cacheable
                            @Override
                            public void execute(Task task) {
                                buildScan.value(test.getIdentityPath() + "#maxParallelForks", String.valueOf(test.getMaxParallelForks()));
                            }
                        })
                )
        );
    }

    private static void addCustomValueAndSearchLink(BuildScanExtension buildScan, String label, String value) {
        buildScan.value(label, value);
        String server = buildScan.getServer();
        if (server != null) {
            String searchParams = "search.names=" + urlEncode(label) + "&search.values=" + urlEncode(value);
            String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
            buildScan.link(label + " build scans", url);
        }
    }
}
