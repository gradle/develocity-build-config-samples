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
import static com.gradle.Utils.envVariable;
import static com.gradle.Utils.execAndCheckSuccess;
import static com.gradle.Utils.execAndGetStdOut;
import static com.gradle.Utils.firstSysPropertyKeyStartingWith;
import static com.gradle.Utils.isNotEmpty;
import static com.gradle.Utils.projectProperty;
import static com.gradle.Utils.readPropertiesFile;
import static com.gradle.Utils.stripPrefix;
import static com.gradle.Utils.sysProperty;
import static com.gradle.Utils.urlEncode;

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
final class CustomBuildScanEnhancements {

    static void configureBuildScan(BuildScanExtension buildScan, ProviderFactory providers, Gradle gradle) {
        captureOs(buildScan, providers);
        captureIde(buildScan, providers, gradle);
        captureCiOrLocal(buildScan, providers);
        captureCiMetadata(buildScan, providers, gradle);
        captureGitMetadata(buildScan);
        captureTestParallelization(buildScan, gradle);
    }

    private static void captureOs(BuildScanExtension buildScan, ProviderFactory providers) {
        sysProperty("os.name", providers).ifPresent(buildScan::tag);
    }

    private static void captureIde(BuildScanExtension buildScan, ProviderFactory providers, Gradle gradle) {
        if (!isCi(providers)) {
            // Wait for projects to load to ensure Gradle project properties are initialized
            gradle.projectsEvaluated(g -> {
                Optional<String> invokedFromAndroidStudio = projectProperty("android.injected.invoked.from.ide", providers, gradle);
                Optional<String> androidStudioVersion = projectProperty("android.injected.studio.version", providers, gradle);
                Optional<String> newIdeaVersion = sysProperty("idea.version", providers);
                Optional<String> oldIdeaVersion = firstSysPropertyKeyStartingWith("idea.version", providers);
                Optional<String> eclipseVersion = sysProperty("eclipse.buildId", providers);

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

    private static void captureCiOrLocal(BuildScanExtension buildScan, ProviderFactory providers) {
        buildScan.tag(isCi(providers) ? "CI" : "LOCAL");
    }

    private static void captureCiMetadata(BuildScanExtension buildScan, ProviderFactory providers, Gradle gradle) {
        if (isJenkins(providers) || isHudson(providers)) {
            envVariable("BUILD_URL", providers).ifPresent(url ->
                buildScan.link(isJenkins(providers) ? "Jenkins build" : "Hudson build", url));
            envVariable("BUILD_NUMBER", providers).ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("NODE_NAME", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI node", value));
            envVariable("JOB_NAME", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI job", value));
            envVariable("STAGE_NAME", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI stage", value));
        }

        if (isTeamCity(providers)) {
            // Wait for projects to load to ensure Gradle project properties are initialized
            gradle.projectsEvaluated(g -> {
                Optional<String> teamCityConfigFile = projectProperty("teamcity.configuration.properties.file", providers, gradle);
                Optional<String> buildNumber = projectProperty("build.number", providers, gradle);
                Optional<String> buildTypeId = projectProperty("teamcity.buildType.id", providers, gradle);
                if (teamCityConfigFile.isPresent()
                    && buildNumber.isPresent()
                    && buildTypeId.isPresent()) {
                    Properties properties = readPropertiesFile(teamCityConfigFile.get(), providers, gradle);
                    String teamCityServerUrl = properties.getProperty("teamcity.serverUrl");
                    if (teamCityServerUrl != null) {
                        String buildUrl = appendIfMissing(teamCityServerUrl, "/") + "viewLog.html?buildNumber=" + buildNumber.get() + "&buildTypeId=" + buildTypeId.get();
                        buildScan.link("TeamCity build", buildUrl);
                    }
                }
                buildNumber.ifPresent(value ->
                    buildScan.value("CI build number", value));
                buildTypeId.ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI build config", value));
                projectProperty("agent.name", providers, gradle).ifPresent(value ->
                    addCustomValueAndSearchLink(buildScan, "CI agent", value));
            });
        }

        if (isCircleCI(providers)) {
            envVariable("CIRCLE_BUILD_URL", providers).ifPresent(url ->
                buildScan.link("CircleCI build", url));
            envVariable("CIRCLE_BUILD_NUM", providers).ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("CIRCLE_JOB", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI job", value));
            envVariable("CIRCLE_WORKFLOW_ID", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI workflow", value));
        }

        if (isBamboo(providers)) {
            envVariable("bamboo_resultsUrl", providers).ifPresent(url ->
                buildScan.link("Bamboo build", url));
            envVariable("bamboo_buildNumber", providers).ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("bamboo_planName", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI plan", value));
            envVariable("bamboo_buildPlanName", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI build plan", value));
            envVariable("bamboo_agentId", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI agent", value));
        }

        if (isGitHubActions(providers)) {
            Optional<String> gitHubRepository = envVariable("GITHUB_REPOSITORY", providers);
            Optional<String> gitHubRunId = envVariable("GITHUB_RUN_ID", providers);
            if (gitHubRepository.isPresent() && gitHubRunId.isPresent()) {
                buildScan.link("GitHub Actions build", "https://github.com/" + gitHubRepository.get() + "/actions/runs/" + gitHubRunId.get());
            }
            envVariable("GITHUB_WORKFLOW", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "GitHub workflow", value));
        }

        if (isGitLab(providers)) {
            envVariable("CI_JOB_URL", providers).ifPresent(url ->
                buildScan.link("GitLab build", url));
            envVariable("CI_PIPELINE_URL", providers).ifPresent(url ->
                buildScan.link("GitLab pipeline", url));
            envVariable("CI_JOB_NAME", providers).ifPresent(value1 ->
                addCustomValueAndSearchLink(buildScan, "CI job", value1));
            envVariable("CI_JOB_STAGE", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI stage", value));
        }

        if (isTravis(providers)) {
            envVariable("TRAVIS_BUILD_WEB_URL", providers).ifPresent(url ->
                buildScan.link("Travis build", url));
            envVariable("TRAVIS_BUILD_NUMBER", providers).ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("TRAVIS_JOB_NAME", providers).ifPresent(value ->
                addCustomValueAndSearchLink(buildScan, "CI job", value));
            envVariable("TRAVIS_EVENT_TYPE", providers).ifPresent(buildScan::tag);
        }

        if (isBitrise(providers)) {
            envVariable("BITRISE_BUILD_URL", providers).ifPresent(url ->
                buildScan.link("Bitrise build", url));
            envVariable("BITRISE_BUILD_NUMBER", providers).ifPresent(value ->
                buildScan.value("CI build number", value));
        }
    }

    private static boolean isCi(ProviderFactory providers) {
        return isGenericCI(providers) || isJenkins(providers) || isHudson(providers) || isTeamCity(providers) || isCircleCI(providers) || isBamboo(providers) || isGitHubActions(providers) || isGitLab(providers) || isTravis(providers) || isBitrise(providers);
    }

    private static boolean isGenericCI(ProviderFactory providers) {
        return envVariable("CI", providers).isPresent() || sysProperty("CI", providers).isPresent();
    }

    private static boolean isJenkins(ProviderFactory providers) {
        return envVariable("JENKINS_URL", providers).isPresent();
    }

    private static boolean isHudson(ProviderFactory providers) {
        return envVariable("HUDSON_URL", providers).isPresent();
    }

    private static boolean isTeamCity(ProviderFactory providers) {
        return envVariable("TEAMCITY_VERSION", providers).isPresent();
    }

    private static boolean isCircleCI(ProviderFactory providers) {
        return envVariable("CIRCLE_BUILD_URL", providers).isPresent();
    }

    private static boolean isBamboo(ProviderFactory providers) {
        return envVariable("bamboo_resultsUrl", providers).isPresent();
    }

    private static boolean isGitHubActions(ProviderFactory providers) {
        return envVariable("GITHUB_ACTIONS", providers).isPresent();
    }

    private static boolean isGitLab(ProviderFactory providers) {
        return envVariable("GITLAB_CI", providers).isPresent();
    }

    private static boolean isTravis(ProviderFactory providers) {
        return envVariable("TRAVIS_JOB_ID", providers).isPresent();
    }

    private static boolean isBitrise(ProviderFactory providers) {
        return envVariable("BITRISE_BUILD_URL", providers).isPresent();
    }

    private static void captureGitMetadata(BuildScanExtension buildScan) {
        buildScan.background(api -> {
            if (!isGitInstalled()) {
                return;
            }

            String gitRepo = execAndGetStdOut("git", "config", "--get", "remote.origin.url");
            String gitCommitId = execAndGetStdOut("git", "rev-parse", "--verify", "HEAD");
            String gitCommitShortId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD");
            String gitBranchName = execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD");
            String gitStatus = execAndGetStdOut("git", "status", "--porcelain");

            if (isNotEmpty(gitRepo)) {
                api.value("Git repository", gitRepo);
            }
            if (isNotEmpty(gitCommitId)) {
                buildScan.value("Git commit id", gitCommitId);
            }
            if (isNotEmpty(gitCommitShortId)) {
                addCustomValueAndSearchLink(buildScan, "Git commit id short", gitCommitShortId);
            }
            if (isNotEmpty(gitBranchName)) {
                api.tag(gitBranchName);
                api.value("Git branch", gitBranchName);
            }
            if (isNotEmpty(gitStatus)) {
                api.tag("Dirty");
                api.value("Git status", gitStatus);
            }

            if (isNotEmpty(gitRepo) && isNotEmpty(gitCommitId)) {
                if (gitRepo.contains("github.com/") || gitRepo.contains("github.com:")) {
                    Matcher matcher = Pattern.compile("(.*)github\\.com[/|:](.*)").matcher(gitRepo);
                    if (matcher.matches()) {
                        String rawRepoPath = matcher.group(2);
                        String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                        api.link("Github source", "https://github.com/" + repoPath + "/tree/" + gitCommitId);
                    }
                } else if (gitRepo.contains("gitlab.com/") || gitRepo.contains("gitlab.com:")) {
                    Matcher matcher = Pattern.compile("(.*)gitlab\\.com[/|:](.*)").matcher(gitRepo);
                    if (matcher.matches()) {
                        String rawRepoPath = matcher.group(2);
                        String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                        api.link("GitLab Source", "https://gitlab.com/" + repoPath + "/-/commit/" + gitCommitId);
                    }
                }
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
