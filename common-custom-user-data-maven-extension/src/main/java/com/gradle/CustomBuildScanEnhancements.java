package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gradle.Utils.appendIfMissing;
import static com.gradle.Utils.envVariable;
import static com.gradle.Utils.envVariablePresent;
import static com.gradle.Utils.execAndCheckSuccess;
import static com.gradle.Utils.execAndGetStdOut;
import static com.gradle.Utils.isNotEmpty;
import static com.gradle.Utils.readPropertiesFile;
import static com.gradle.Utils.sysProperty;
import static com.gradle.Utils.sysPropertyKeyStartingWith;
import static com.gradle.Utils.sysPropertyPresent;
import static com.gradle.Utils.urlEncode;

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
final class CustomBuildScanEnhancements {

    static void configureBuildScan(BuildScanApi buildScan) {
        captureOs(buildScan);
        captureIde(buildScan);
        captureCiOrLocal(buildScan);
        captureCiMetadata(buildScan);
        captureGitMetadata(buildScan);
    }

    private static void captureOs(BuildScanApi buildScan) {
        buildScan.tag(sysProperty("os.name"));
    }

    private static void captureIde(BuildScanApi buildScan) {
        if (sysPropertyPresent("idea.version") || sysPropertyKeyStartingWith("idea.version")) {
            buildScan.tag("IntelliJ IDEA");
        } else if (sysPropertyPresent("eclipse.buildId")) {
            buildScan.tag("Eclipse");
        } else if (!isCi()) {
            buildScan.tag("Cmd Line");
        }
    }

    private static void captureCiOrLocal(BuildScanApi buildScan) {
        buildScan.tag(isCi() ? "CI" : "LOCAL");
    }

    private static void captureCiMetadata(BuildScanApi buildScan) {
        if (isJenkins() || isHudson()) {
            if (envVariablePresent("BUILD_URL")) {
                buildScan.link(isJenkins() ? "Jenkins build" : "Hudson build", envVariable("BUILD_URL"));
            }
            if (envVariablePresent("BUILD_NUMBER")) {
                buildScan.value("CI build number", envVariable("BUILD_NUMBER"));
            }
            if (envVariablePresent("NODE_NAME")) {
                addCustomValueAndSearchLink(buildScan, "CI node", envVariable("NODE_NAME"));
            }
            if (envVariablePresent("JOB_NAME")) {
                addCustomValueAndSearchLink(buildScan, "CI job", envVariable("JOB_NAME"));
            }
            if (envVariablePresent("STAGE_NAME")) {
                addCustomValueAndSearchLink(buildScan, "CI stage", envVariable("STAGE_NAME"));
            }
        }

        if (isTeamCity()) {
            if (envVariablePresent("BUILD_URL")) {
                buildScan.link("TeamCity build", envVariable("BUILD_URL"));
            }
            if (envVariablePresent("BUILD_NUMBER")) {
                buildScan.value("CI build number", envVariable("BUILD_NUMBER"));
            }
            if (envVariablePresent("BUILD_AGENT_NAME")) {
                addCustomValueAndSearchLink(buildScan, "CI agent", envVariable("BUILD_AGENT_NAME"));
            }
        }

        if (isCircleCI()) {
            if (envVariablePresent("CIRCLE_BUILD_URL")) {
                buildScan.link("CircleCI build", envVariable("CIRCLE_BUILD_URL"));
            }
            if (envVariablePresent("CIRCLE_BUILD_NUM")) {
                buildScan.value("CI build number", envVariable("CIRCLE_BUILD_NUM"));
            }
            if (envVariablePresent("CIRCLE_JOB")) {
                addCustomValueAndSearchLink(buildScan, "CI job", envVariable("CIRCLE_JOB"));
            }
            if (envVariablePresent("CIRCLE_WORKFLOW_ID")) {
                addCustomValueAndSearchLink(buildScan, "CI workflow", envVariable("CIRCLE_WORKFLOW_ID"));
            }
        }

        if (isBamboo()) {
            if (envVariablePresent("bamboo_resultsUrl")) {
                buildScan.link("Bamboo build", envVariable("bamboo_resultsUrl"));
            }
            if (envVariablePresent("bamboo_buildNumber")) {
                buildScan.value("CI build number", envVariable("bamboo_buildNumber"));
            }
            if (envVariablePresent("bamboo_planName")) {
                addCustomValueAndSearchLink(buildScan, "CI plan", envVariable("bamboo_planName"));
            }
            if (envVariablePresent("bamboo_buildPlanName")) {
                addCustomValueAndSearchLink(buildScan, "CI build plan", envVariable("bamboo_buildPlanName"));
            }
            if (envVariablePresent("bamboo_agentId")) {
                addCustomValueAndSearchLink(buildScan, "CI agent", envVariable("bamboo_agentId"));
            }
        }

        if (isGitHubActions()) {
            if (envVariablePresent("GITHUB_REPOSITORY") && envVariablePresent("GITHUB_RUN_ID")) {
                buildScan.link("GitHub Actions build", "https://github.com/" + envVariable("GITHUB_REPOSITORY") + "/actions/runs/" + envVariable("GITHUB_RUN_ID"));
            }
            if (envVariablePresent("GITHUB_WORKFLOW")) {
                addCustomValueAndSearchLink(buildScan, "GitHub workflow", envVariable("GITHUB_WORKFLOW"));
            }
        }

        if (isGitLab()) {
            if (envVariablePresent("CI_JOB_URL")) {
                buildScan.link("GitLab build", envVariable("CI_JOB_URL"));
            }
            if (envVariablePresent("CI_PIPELINE_URL")) {
                buildScan.link("GitLab pipeline", envVariable("CI_PIPELINE_URL"));
            }
            if (envVariablePresent("CI_JOB_NAME")) {
                addCustomValueAndSearchLink(buildScan, "CI job", envVariable("CI_JOB_NAME"));
            }
            if (envVariablePresent("CI_JOB_STAGE")) {
                addCustomValueAndSearchLink(buildScan, "CI stage", envVariable("CI_JOB_STAGE"));
            }
        }

        if (isTravis()) {
            if (envVariablePresent("TRAVIS_BUILD_WEB_URL")) {
                buildScan.link("Travis build", envVariable("TRAVIS_BUILD_WEB_URL"));
            }
            if (envVariablePresent("TRAVIS_BUILD_NUMBER")) {
                buildScan.value("CI build number", envVariable("TRAVIS_BUILD_NUMBER"));
            }
            if (envVariablePresent("TRAVIS_JOB_NAME")) {
                addCustomValueAndSearchLink(buildScan, "CI job", envVariable("TRAVIS_JOB_NAME"));
            }
            if (envVariablePresent("TRAVIS_EVENT_TYPE")) {
                buildScan.tag(envVariable("TRAVIS_EVENT_TYPE"));
            }
        }

        if (isBitrise()) {
            if (envVariablePresent("BITRISE_BUILD_URL")) {
                buildScan.link("Bitrise build", envVariable("BITRISE_BUILD_URL"));
            }
            if (envVariablePresent("BITRISE_BUILD_NUMBER")) {
                buildScan.value("CI build number", envVariable("BITRISE_BUILD_NUMBER"));
            }
        }
    }

    private static boolean isCi() {
        return isGenericCI() || isJenkins() || isTeamCity() || isCircleCI() || isBamboo() || isGitHubActions() || isGitLab() || isTravis() || isBitrise();
    }

    private static boolean isGenericCI() {
        return envVariablePresent("CI") || sysPropertyPresent("CI");
    }

    private static boolean isJenkins() {
        return envVariablePresent("JENKINS_URL");
    }

    private static boolean isHudson() {
        return envVariablePresent("HUDSON_URL");
    }

    private static boolean isTeamCity() {
        return envVariablePresent("TEAMCITY_VERSION");
    }

    private static boolean isCircleCI() {
        return envVariablePresent("CIRCLE_BUILD_URL");
    }

    private static boolean isBamboo() {
        return envVariablePresent("bamboo_resultsUrl");
    }

    private static boolean isGitHubActions() {
        return envVariablePresent("GITHUB_ACTIONS");
    }

    private static boolean isGitLab() {
        return envVariablePresent("GITLAB_CI");
    }

    private static boolean isTravis() {
        return envVariablePresent("TRAVIS_JOB_ID");
    }

    private static boolean isBitrise() {
        return envVariablePresent("BITRISE_BUILD_URL");
    }

    static void captureGitMetadata(BuildScanApi buildScan) {
        buildScan.background(api -> {
            if (!isGitInstalled()) {
                return;
            }

            String gitCommitId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD");
            String gitBranchName = execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD");
            String gitStatus = execAndGetStdOut("git", "status", "--porcelain");

            if (gitCommitId != null) {
                addCustomValueAndSearchLink(api, "Git commit id", gitCommitId);

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

    private static void addCustomValueAndSearchLink(BuildScanApi buildScan, String label, String value) {
        buildScan.value(label, value);
        addCustomLinkWithSearchTerms(buildScan, label + " build scans", label, value);
    }

    private static void addCustomLinkWithSearchTerms(BuildScanApi buildScan, String title, String name, String value) {
        String server = buildScan.getServer();
        if (server != null) {
            String searchParams = "search.names=" + urlEncode(name) + "&search.values=" + urlEncode(value);
            String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
            buildScan.link(title, url);
        }
    }

    private CustomBuildScanEnhancements() {
    }

}
