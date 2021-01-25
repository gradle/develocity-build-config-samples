package com.gradle;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.testing.Test;

import java.util.Properties;
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
final class CustomBuildScanConfig {

    static void configureBuildScan(BuildScanExtension buildScan, Gradle gradle) {
        captureOs(buildScan);
        captureIde(buildScan, gradle);
        captureCiOrLocal(buildScan);
        captureCiMetadata(buildScan);
        captureGitMetadata(buildScan);
        captureTestParallelization(buildScan, gradle);
    }

    private static void captureOs(BuildScanExtension buildScan) {
        buildScan.tag(sysProperty("os.name"));
    }

    private static void captureIde(BuildScanExtension buildScan, Gradle gradle) {
        gradle.projectsEvaluated(g -> {
            Project project = g.getRootProject();
            if (project.hasProperty("android.injected.invoked.from.ide")) {
                buildScan.tag("Android Studio");
                if (project.hasProperty("android.injected.studio.version")) {
                    buildScan.value("Android Studio version", String.valueOf(project.property("android.injected.studio.version")));
                }
            } else if (sysPropertyPresent("idea.version") || sysPropertyKeyStartingWith("idea.version")) {
                buildScan.tag("IntelliJ IDEA");
            } else if (sysPropertyPresent("eclipse.buildId")) {
                buildScan.tag("Eclipse");
            } else if (!isCi()) {
                buildScan.tag("Cmd Line");
            }
        });
    }

    private static void captureCiOrLocal(BuildScanExtension buildScan) {
        buildScan.tag(isCi() ? "CI" : "LOCAL");
    }

    private static void captureCiMetadata(BuildScanExtension buildScan) {
        if (isJenkins()) {
            if (envVariablePresent("BUILD_URL")) {
                buildScan.link("Jenkins build", envVariable("BUILD_URL"));
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
            if (sysPropertyPresent("teamcity.configuration.properties.file")) {
                Properties properties = readPropertiesFile(sysProperty("teamcity.configuration.properties.file"));
                String teamCityServerUrl = properties.getProperty("teamcity.serverUrl");
                if (teamCityServerUrl != null && sysPropertyPresent("build.number") && sysPropertyPresent("teamcity.buildType.id")) {
                    String buildNumber = sysProperty("build.number");
                    String buildTypeId = sysProperty("teamcity.buildType.id");
                    buildScan.link("TeamCity build", appendIfMissing(teamCityServerUrl, "/") + "viewLog.html?buildNumber=" + buildNumber + "&buildTypeId=" + buildTypeId);
                }
            }
            if (sysPropertyPresent("build.number")) {
                buildScan.value("CI build number", sysProperty("build.number"));
            }
            if (sysPropertyPresent("agent.name")) {
                addCustomValueAndSearchLink(buildScan, "CI agent", sysProperty("agent.name"));
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
    }

    private static boolean isCi() {
        return isJenkins() || isTeamCity() || isCircleCI() || isBamboo() || isGitHubActions() || isGitLab() || isTravis();
    }

    private static boolean isJenkins() {
        return envVariablePresent("JENKINS_URL");
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

    static void captureGitMetadata(BuildScanExtension buildScan) {
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
        addCustomLinkWithSearchTerms(buildScan, label + " build scans", label, value);
    }

    private static void addCustomLinkWithSearchTerms(BuildScanExtension buildScan, String title, String name, String value) {
        String server = buildScan.getServer();
        if (server != null) {
            String searchParams = "search.names=" + urlEncode(name) + "&search.values=" + urlEncode(value);
            String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
            buildScan.link(title, url);
        }
    }

    private CustomBuildScanConfig() {
    }

}
