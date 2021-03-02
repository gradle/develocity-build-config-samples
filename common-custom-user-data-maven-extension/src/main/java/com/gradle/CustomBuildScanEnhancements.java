package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gradle.Utils.*;

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
final class CustomBuildScanEnhancements {

    static void configureBuildScan(BuildScanApi buildScan) {
        new CustomBuildScanEnhancer(buildScan).enhance();;
    }

    private static class CustomBuildScanEnhancer {
        private final BuildScanApi buildScan;

        private CustomBuildScanEnhancer(BuildScanApi buildScan) {
            this.buildScan = buildScan;
        }

        public void enhance() {
            captureOs();
            captureIde();
            captureCiOrLocal();
            captureCiMetadata();
            captureGitMetadata();
        }

        private void captureOs() {
            buildScan.tag(sysProperty("os.name"));
        }

        private void captureIde() {
            if (sysPropertyPresent("idea.version") || sysPropertyKeyStartingWith("idea.version")) {
                buildScan.tag("IntelliJ IDEA");
            } else if (sysPropertyPresent("eclipse.buildId")) {
                buildScan.tag("Eclipse");
            } else if (!isCi()) {
                buildScan.tag("Cmd Line");
            }
        }

        private void captureCiOrLocal() {
            buildScan.tag(isCi() ? "CI" : "LOCAL");
        }

        private void captureCiMetadata() {
            if (isJenkins() || isHudson()) {
                if (envVariablePresent("BUILD_URL")) {
                    buildScan.link(isJenkins() ? "Jenkins build" : "Hudson build", envVariable("BUILD_URL"));
                }
                if (envVariablePresent("BUILD_NUMBER")) {
                    buildScan.value("CI build number", envVariable("BUILD_NUMBER"));
                }
                if (envVariablePresent("NODE_NAME")) {
                    addCustomValueAndSearchLink("CI node", envVariable("NODE_NAME"));
                }
                if (envVariablePresent("JOB_NAME")) {
                    addCustomValueAndSearchLink("CI job", envVariable("JOB_NAME"));
                }
                if (envVariablePresent("STAGE_NAME")) {
                    addCustomValueAndSearchLink("CI stage", envVariable("STAGE_NAME"));
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
                    addCustomValueAndSearchLink("CI agent", envVariable("BUILD_AGENT_NAME"));
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
                    addCustomValueAndSearchLink("CI job", envVariable("CIRCLE_JOB"));
                }
                if (envVariablePresent("CIRCLE_WORKFLOW_ID")) {
                    addCustomValueAndSearchLink("CI workflow", envVariable("CIRCLE_WORKFLOW_ID"));
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
                    addCustomValueAndSearchLink("CI plan", envVariable("bamboo_planName"));
                }
                if (envVariablePresent("bamboo_buildPlanName")) {
                    addCustomValueAndSearchLink("CI build plan", envVariable("bamboo_buildPlanName"));
                }
                if (envVariablePresent("bamboo_agentId")) {
                    addCustomValueAndSearchLink("CI agent", envVariable("bamboo_agentId"));
                }
            }

            if (isGitHubActions()) {
                if (envVariablePresent("GITHUB_REPOSITORY") && envVariablePresent("GITHUB_RUN_ID")) {
                    buildScan.link("GitHub Actions build", "https://github.com/" + envVariable("GITHUB_REPOSITORY") + "/actions/runs/" + envVariable("GITHUB_RUN_ID"));
                }
                if (envVariablePresent("GITHUB_WORKFLOW")) {
                    addCustomValueAndSearchLink("GitHub workflow", envVariable("GITHUB_WORKFLOW"));
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
                    addCustomValueAndSearchLink("CI job", envVariable("CI_JOB_NAME"));
                }
                if (envVariablePresent("CI_JOB_STAGE")) {
                    addCustomValueAndSearchLink("CI stage", envVariable("CI_JOB_STAGE"));
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
                    addCustomValueAndSearchLink("CI job", envVariable("TRAVIS_JOB_NAME"));
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

        private void captureGitMetadata() {
            buildScan.background(api -> {
                if (!isGitInstalled()) {
                    return;
                }

                String gitCommitId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD");
                String gitBranchName = execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD");
                String gitStatus = execAndGetStdOut("git", "status", "--porcelain");

                if (gitCommitId != null) {
                    addCustomValueAndSearchLink("Git commit id", gitCommitId);

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

        private void addCustomValueAndSearchLink(String label, String value) {
            buildScan.value(label, value);
            addCustomLinkWithSearchTerms(label + " build scans", label, value);
        }

        private void addCustomLinkWithSearchTerms(String title, String name, String value) {
            String server = buildScan.getServer();
            if (server != null) {
                String searchParams = "search.names=" + urlEncode(name) + "&search.values=" + urlEncode(value);
                String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
                buildScan.link(title, url);
            }
        }
    }

    private CustomBuildScanEnhancements() {
    }

}
