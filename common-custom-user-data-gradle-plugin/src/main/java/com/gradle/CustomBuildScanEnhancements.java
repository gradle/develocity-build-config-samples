package com.gradle;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.testing.Test;

import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gradle.Utils.appendIfMissing;
import static com.gradle.Utils.execAndCheckSuccess;
import static com.gradle.Utils.execAndGetStdOut;
import static com.gradle.Utils.isNotEmpty;
import static com.gradle.Utils.stripPrefix;
import static com.gradle.Utils.urlEncode;

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
final class CustomBuildScanEnhancements {

    private final BuildScanExtension buildScan;
    private final ProviderFactory providers;
    private final Gradle gradle;

    CustomBuildScanEnhancements(BuildScanExtension buildScan, ProviderFactory providers, Gradle gradle) {
        this.buildScan = buildScan;
        this.providers = providers;
        this.gradle = gradle;
    }

    void apply() {
        captureOs();
        captureIde();
        captureCiOrLocal();
        captureCiMetadata();
        captureGitMetadata();
        captureTestParallelization();
    }

    private void captureOs() {
        sysProperty("os.name").ifPresent(buildScan::tag);
    }

    private void captureIde() {
        if (!isCi()) {
            // Wait for projects to load to ensure Gradle project properties are initialized
            gradle.projectsEvaluated(g -> {
                Optional<String> invokedFromAndroidStudio = projectProperty("android.injected.invoked.from.ide");
                Optional<String> androidStudioVersion = projectProperty("android.injected.studio.version");
                Optional<String> newIdeaVersion = sysProperty("idea.version");
                Optional<String> oldIdeaVersion = firstSysPropertyKeyStartingWith("idea.version");
                Optional<String> eclipseVersion = sysProperty("eclipse.buildId");

                if (invokedFromAndroidStudio.isPresent()) {
                    buildScan.tag("Android Studio");
                    androidStudioVersion.ifPresent(v -> buildScan.value("Android Studio version", v));
                } else if (newIdeaVersion.isPresent()) {
                    buildScan.tag("IntelliJ IDEA");
                    buildScan.value("IntelliJ IDEA version", newIdeaVersion.get());
                } else if (oldIdeaVersion.isPresent()) {
                    buildScan.tag("IntelliJ IDEA");
                    buildScan.value("IntelliJ IDEA version", stripPrefix("idea.version", oldIdeaVersion.get()));
                } else if (eclipseVersion.isPresent()) {
                    buildScan.tag("Eclipse");
                    buildScan.value("Eclipse version", eclipseVersion.get());
                } else {
                    buildScan.tag("Cmd Line");
                }
            });
        }
    }

    private void captureCiOrLocal() {
        buildScan.tag(isCi() ? "CI" : "LOCAL");
    }

    private void captureCiMetadata() {
        if (isJenkins() || isHudson()) {
            envVariable("BUILD_URL").ifPresent(url ->
                buildScan.link(isJenkins() ? "Jenkins build" : "Hudson build", url));
            envVariable("BUILD_NUMBER").ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("NODE_NAME").ifPresent(value ->
                addCustomValueAndSearchLink("CI node", value));
            envVariable("JOB_NAME").ifPresent(value ->
                addCustomValueAndSearchLink("CI job", value));
            envVariable("STAGE_NAME").ifPresent(value ->
                addCustomValueAndSearchLink("CI stage", value));
        }

        if (isTeamCity()) {
            // Wait for projects to load to ensure Gradle project properties are initialized
            gradle.projectsEvaluated(g -> {
                Optional<String> teamCityConfigFile = projectProperty("teamcity.configuration.properties.file");
                Optional<String> buildNumber = projectProperty("build.number");
                Optional<String> buildTypeId = projectProperty("teamcity.buildType.id");
                if (teamCityConfigFile.isPresent()
                    && buildNumber.isPresent()
                    && buildTypeId.isPresent()) {
                    Properties properties = readPropertiesFile(teamCityConfigFile.get());
                    String teamCityServerUrl = properties.getProperty("teamcity.serverUrl");
                    if (teamCityServerUrl != null) {
                        String buildUrl = appendIfMissing(teamCityServerUrl, "/") + "viewLog.html?buildNumber=" + urlEncode(buildNumber.get()) + "&buildTypeId=" + urlEncode(buildTypeId.get());
                        buildScan.link("TeamCity build", buildUrl);
                    }
                }
                buildNumber.ifPresent(value ->
                    buildScan.value("CI build number", value));
                buildTypeId.ifPresent(value ->
                    addCustomValueAndSearchLink("CI build config", value));
                projectProperty("agent.name").ifPresent(value ->
                    addCustomValueAndSearchLink("CI agent", value));
            });
        }

        if (isCircleCI()) {
            envVariable("CIRCLE_BUILD_URL").ifPresent(url ->
                buildScan.link("CircleCI build", url));
            envVariable("CIRCLE_BUILD_NUM").ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("CIRCLE_JOB").ifPresent(value ->
                addCustomValueAndSearchLink("CI job", value));
            envVariable("CIRCLE_WORKFLOW_ID").ifPresent(value ->
                addCustomValueAndSearchLink("CI workflow", value));
        }

        if (isBamboo()) {
            envVariable("bamboo_resultsUrl").ifPresent(url ->
                buildScan.link("Bamboo build", url));
            envVariable("bamboo_buildNumber").ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("bamboo_planName").ifPresent(value ->
                addCustomValueAndSearchLink("CI plan", value));
            envVariable("bamboo_buildPlanName").ifPresent(value ->
                addCustomValueAndSearchLink("CI build plan", value));
            envVariable("bamboo_agentId").ifPresent(value ->
                addCustomValueAndSearchLink("CI agent", value));
        }

        if (isGitHubActions()) {
            Optional<String> gitHubRepository = envVariable("GITHUB_REPOSITORY");
            Optional<String> gitHubRunId = envVariable("GITHUB_RUN_ID");
            if (gitHubRepository.isPresent() && gitHubRunId.isPresent()) {
                buildScan.link("GitHub Actions build", "https://github.com/" + gitHubRepository.get() + "/actions/runs/" + gitHubRunId.get());
            }
            envVariable("GITHUB_WORKFLOW").ifPresent(value ->
                addCustomValueAndSearchLink("GitHub workflow", value));
        }

        if (isGitLab()) {
            envVariable("CI_JOB_URL").ifPresent(url ->
                buildScan.link("GitLab build", url));
            envVariable("CI_PIPELINE_URL").ifPresent(url ->
                buildScan.link("GitLab pipeline", url));
            envVariable("CI_JOB_NAME").ifPresent(value1 ->
                addCustomValueAndSearchLink("CI job", value1));
            envVariable("CI_JOB_STAGE").ifPresent(value ->
                addCustomValueAndSearchLink("CI stage", value));
        }

        if (isTravis()) {
            envVariable("TRAVIS_BUILD_WEB_URL").ifPresent(url ->
                buildScan.link("Travis build", url));
            envVariable("TRAVIS_BUILD_NUMBER").ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("TRAVIS_JOB_NAME").ifPresent(value ->
                addCustomValueAndSearchLink("CI job", value));
            envVariable("TRAVIS_EVENT_TYPE").ifPresent(buildScan::tag);
        }

        if (isBitrise()) {
            envVariable("BITRISE_BUILD_URL").ifPresent(url ->
                buildScan.link("Bitrise build", url));
            envVariable("BITRISE_BUILD_NUMBER").ifPresent(value ->
                buildScan.value("CI build number", value));
        }
    }

    private boolean isCi() {
        return isGenericCI() || isJenkins() || isHudson() || isTeamCity() || isCircleCI() || isBamboo() || isGitHubActions() || isGitLab() || isTravis() || isBitrise();
    }

    private boolean isGenericCI() {
        return envVariable("CI").isPresent() || sysProperty("CI").isPresent();
    }

    private boolean isJenkins() {
        return envVariable("JENKINS_URL").isPresent();
    }

    private boolean isHudson() {
        return envVariable("HUDSON_URL").isPresent();
    }

    private boolean isTeamCity() {
        return envVariable("TEAMCITY_VERSION").isPresent();
    }

    private boolean isCircleCI() {
        return envVariable("CIRCLE_BUILD_URL").isPresent();
    }

    private boolean isBamboo() {
        return envVariable("bamboo_resultsUrl").isPresent();
    }

    private boolean isGitHubActions() {
        return envVariable("GITHUB_ACTIONS").isPresent();
    }

    private boolean isGitLab() {
        return envVariable("GITLAB_CI").isPresent();
    }

    private boolean isTravis() {
        return envVariable("TRAVIS_JOB_ID").isPresent();
    }

    private boolean isBitrise() {
        return envVariable("BITRISE_BUILD_URL").isPresent();
    }

    private void captureGitMetadata() {
        buildScan.background(captureGitMetadataAction);
    }

    private static final Action<BuildScanExtension> captureGitMetadataAction =
        buildScan -> {
            if (!isGitInstalled()) {
                return;
            }

            String gitRepo = execAndGetStdOut("git", "config", "--get", "remote.origin.url");
            String gitCommitId = execAndGetStdOut("git", "rev-parse", "--verify", "HEAD");
            String gitCommitShortId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD");
            String gitBranchName = execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD");
            String gitStatus = execAndGetStdOut("git", "status", "--porcelain");

            if (isNotEmpty(gitRepo)) {
                buildScan.value("Git repository", gitRepo);
            }
            if (isNotEmpty(gitCommitId)) {
                buildScan.value("Git commit id", gitCommitId);
            }
            if (isNotEmpty(gitCommitShortId)) {
                addCustomValueAndSearchLink(buildScan, "Git commit id", "Git commit id short", gitCommitShortId);
            }
            if (isNotEmpty(gitBranchName)) {
                buildScan.tag(gitBranchName);
                buildScan.value("Git branch", gitBranchName);
            }
            if (isNotEmpty(gitStatus)) {
                buildScan.tag("Dirty");
                buildScan.value("Git status", gitStatus);
            }

            if (isNotEmpty(gitRepo) && isNotEmpty(gitCommitId)) {
                if (gitRepo.contains("github.com/") || gitRepo.contains("github.com:")) {
                    Matcher matcher = Pattern.compile("(.*)github\\.com[/|:](.*)").matcher(gitRepo);
                    if (matcher.matches()) {
                        String rawRepoPath = matcher.group(2);
                        String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                        buildScan.link("Github source", "https://github.com/" + repoPath + "/tree/" + gitCommitId);
                    }
                } else if (gitRepo.contains("gitlab.com/") || gitRepo.contains("gitlab.com:")) {
                    Matcher matcher = Pattern.compile("(.*)gitlab\\.com[/|:](.*)").matcher(gitRepo);
                    if (matcher.matches()) {
                        String rawRepoPath = matcher.group(2);
                        String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                        buildScan.link("GitLab Source", "https://gitlab.com/" + repoPath + "/-/commit/" + gitCommitId);
                    }
                }
            }
        };

    private static boolean isGitInstalled() {
        return execAndCheckSuccess("git", "--version");
    }

    private void addCustomValueAndSearchLink(String name, String value) {
        addCustomValueAndSearchLink(name, name, value);
    }

    private void addCustomValueAndSearchLink(String linkLabel, String name, String value) {
        addCustomValueAndSearchLink(buildScan, linkLabel, name, value);
    }

    private static void addCustomValueAndSearchLink(BuildScanExtension buildScan, String linkLabel, String name, String value) {
        buildScan.value(name, value);
        String server = buildScan.getServer();
        if (server != null) {
            String searchParams = "search.names=" + urlEncode(name) + "&search.values=" + urlEncode(value);
            String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
            buildScan.link(linkLabel + " build scans", url);
        }
    }

    private void captureTestParallelization() {
        gradle.allprojects(p ->
            p.getTasks().withType(Test.class).configureEach(captureMaxParallelForks(buildScan))
        );
    }

    private static Action<Test> captureMaxParallelForks(BuildScanExtension buildScan) {
        return test -> {
            test.doFirst(new Action<Task>() {
                // use anonymous inner class to keep Test task instance cacheable
                // additionally, using lambdas as task actions is deprecated
                @Override
                public void execute(Task task) {
                    buildScan.value(test.getIdentityPath() + "#maxParallelForks", String.valueOf(test.getMaxParallelForks()));
                }
            });
        };
    }

    private Optional<String> envVariable(String name) {
        return Utils.envVariable(name, providers);
    }

    private Optional<String> projectProperty(String name) {
        return Utils.projectProperty(name, providers, gradle);
    }

    private Optional<String> sysProperty(String name) {
        return Utils.sysProperty(name, providers);
    }

    private Optional<String> firstSysPropertyKeyStartingWith(String keyPrefix) {
        return Utils.firstSysPropertyKeyStartingWith(keyPrefix, providers);
    }

    private Properties readPropertiesFile(String fileName) {
        return Utils.readPropertiesFile(fileName, providers, gradle);
    }

}
