package com.gradle;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.UncheckedException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class BuildScanEnhancer {
    private final MessageDigest messageDigest;
    private final BuildScanExtension buildScan;
    private final Project rootProject;

    public BuildScanEnhancer(BuildScanExtension buildScanExtension, Project rootProject) {
        this.buildScan = buildScanExtension;
        this.rootProject = rootProject;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    public BuildScanExtension getExtension() {
        return buildScan;
    }

    public void tagOs() {
        buildScan.tag(System.getProperty("os.name"));
    }

    public void tagIde() {
        if (rootProject.hasProperty("android.injected.invoked.from.ide")) {
            buildScan.tag("Android Studio");
            if (rootProject.hasProperty("android.injected.studio.version")) {
                buildScan.value("Android Studio version", String.valueOf(rootProject.property("android.injected.studio.version")));
            }

        } else if (hasSystemProp("idea.version")) {
            buildScan.tag("IntelliJ IDEA");
        } else if (hasSystemProp("eclipse.buildId")) {
            buildScan.tag("Eclipse");
        } else if (!isCi()) {
            buildScan.tag("Cmd Line");
        }
    }

    public void tagCiOrLocal() {
        buildScan.tag(isCi() ? "CI" : "LOCAL");
    }

    public void addCiMetadata() {
        if (isJenkins()) {
            if (hasEnv("BUILD_URL")) {
                buildScan.link("Jenkins build", System.getenv("BUILD_URL"));
            }

            if (hasEnv("BUILD_NUMBER")) {
                buildScan.value("CI build number", System.getenv("BUILD_NUMBER"));
            }

            if (hasEnv("NODE_NAME")) {
                String nodeNameLabel = "CI node";
                String nodeName = System.getenv("NODE_NAME");
                buildScan.value(nodeNameLabel, nodeName);
                addCustomLinkWithSearchTerms("CI node build scans", nodeNameLabel, nodeName);
            }

            if (hasEnv("JOB_NAME")) {
                String jobNameLabel = "CI job";
                String jobName = System.getenv("JOB_NAME");
                buildScan.value(jobNameLabel, jobName);
                addCustomLinkWithSearchTerms("CI job build scans", jobNameLabel, jobName);
            }

            if (hasEnv("STAGE_NAME")) {
                String stageNameLabel = "CI stage";
                String stageName = System.getenv("STAGE_NAME");
                buildScan.value(stageNameLabel, stageName);
                addCustomLinkWithSearchTerms("CI stage build scans", stageNameLabel, stageName);
            }
        }


        if (isTeamCity()) {
            if (hasSystemProp("teamcity.configuration.properties.file")) {
                Properties properties = readPropertiesFile(System.getProperty("teamcity.configuration.properties.file"));
                final String teamCityServerUrl = properties.getProperty("teamcity.serverUrl");
                if (notEmpty(teamCityServerUrl) && hasSystemProp("build.number") && hasSystemProp("teamcity.buildType.id")) {
                    final String buildNumber = System.getProperty("build.number");
                    final String buildTypeId = System.getProperty("teamcity.buildType.id");
                    buildScan.link("TeamCity build", appendIfMissing(teamCityServerUrl, "/") + "viewLog.html?buildNumber=" + buildNumber + "&buildTypeId=" + buildTypeId);
                }
            }

            if (hasSystemProp("build.number")) {
                buildScan.value("CI build number", System.getProperty("build.number"));
            }

            if (hasSystemProp("agent.name")) {
                String agentNameLabel = "CI agent";
                String agentName = System.getProperty("agent.name");
                buildScan.value(agentNameLabel, agentName);
                addCustomLinkWithSearchTerms("CI agent build scans", agentNameLabel, agentName);
            }
        }

        if (isCircleCI()) {
            if (hasEnv("CIRCLE_BUILD_URL")) {
                buildScan.link("CircleCI build", System.getenv("CIRCLE_BUILD_URL"));
            }

            if (hasEnv("CIRCLE_BUILD_NUM")) {
                buildScan.value("CI build number", System.getenv("CIRCLE_BUILD_NUM"));
            }

            if (hasEnv("CIRCLE_JOB")) {
                String jobNameLabel = "CI job";
                String jobName = System.getenv("CIRCLE_JOB");
                buildScan.value(jobNameLabel, jobName);
                addCustomLinkWithSearchTerms("CI job build scans", jobNameLabel, jobName);
            }

            if (hasEnv("CIRCLE_WORKFLOW_ID")) {
                String workflowIdLabel = "CI workflow";
                String workflowId = System.getenv("CIRCLE_WORKFLOW_ID");
                buildScan.value(workflowIdLabel, workflowId);
                addCustomLinkWithSearchTerms("CI workflow build scans", workflowIdLabel, workflowId);
            }
        }

        if (isBamboo()) {
            if (hasEnv("bamboo_resultsUrl")) {
                buildScan.link("Bamboo build", System.getenv("bamboo_resultsUrl"));
            }

            if (hasEnv("bamboo_buildNumber")) {
                buildScan.value("CI build number", System.getenv("bamboo_buildNumber"));
            }

            if (hasEnv("bamboo_planName")) {
                String planNameLabel = "CI plan";
                String planName = System.getenv("bamboo_planName");
                buildScan.value(planNameLabel, planName);
                addCustomLinkWithSearchTerms("CI plan build scans", planNameLabel, planName);
            }

            if (hasEnv("bamboo_buildPlanName")) {
                String buildPlanNameLabel = "CI build plan";
                String buildPlanName = System.getenv("bamboo_buildPlanName");
                buildScan.value(buildPlanNameLabel, buildPlanName);
                addCustomLinkWithSearchTerms("CI build plan build scans", buildPlanNameLabel, buildPlanName);
            }

            if (hasEnv("bamboo_agentId")) {
                String agentIdLabel = "CI agent";
                String agentId = System.getenv("bamboo_agentId");
                buildScan.value(agentIdLabel, agentId);
                addCustomLinkWithSearchTerms("CI agent build scans", agentIdLabel, agentId);
            }
        }

        if (isGitHubActions()) {
            if (hasEnv("GITHUB_REPOSITORY") && hasEnv("GITHUB_RUN_ID")) {
                buildScan.link("GitHub Actions build", "https://github.com/" + System.getenv("GITHUB_REPOSITORY") + "/actions/runs/" + System.getenv("GITHUB_RUN_ID"));
            }

            if (hasEnv("GITHUB_WORKFLOW")) {
                String workflowNameLabel = "GitHub workflow";
                String workflowName = System.getenv("GITHUB_WORKFLOW");
                buildScan.value(workflowNameLabel, workflowName);
                addCustomLinkWithSearchTerms("GitHub workflow build scans", workflowNameLabel, workflowName);
            }
        }

        if (isGitLab()) {
            if (hasEnv("CI_JOB_URL")) {
                buildScan.link("GitLab build", System.getenv("CI_JOB_URL"));
            }

            if (hasEnv("CI_PIPELINE_URL")) {
                buildScan.link("GitLab pipeline", System.getenv("CI_PIPELINE_URL"));
            }

            if (hasEnv("CI_JOB_NAME")) {
                String jobNameLabel = "CI job";
                String jobName = System.getenv("CI_JOB_NAME");
                buildScan.value(jobNameLabel, jobName);
                addCustomLinkWithSearchTerms("CI job build scans", jobNameLabel, jobName);
            }

            if (hasEnv("CI_JOB_STAGE")) {
                String stageNameLabel = "CI stage";
                String stageName = System.getenv("CI_JOB_STAGE");
                buildScan.value(stageNameLabel, stageName);
                addCustomLinkWithSearchTerms("CI stage build scans", stageNameLabel, stageName);
            }
        }

        if (isTravis()) {
            if (hasEnv("TRAVIS_BUILD_WEB_URL")) {
                buildScan.link("Travis build", System.getenv("TRAVIS_BUILD_WEB_URL"));
            }

            if (hasEnv("TRAVIS_BUILD_NUMBER")) {
                buildScan.value("CI build number", System.getenv("TRAVIS_BUILD_NUMBER"));
            }

            if (hasEnv("TRAVIS_JOB_NAME")) {
                String jobNameLabel = "CI job";
                String jobName = System.getenv("TRAVIS_JOB_NAME");
                buildScan.value(jobNameLabel, jobName);
                addCustomLinkWithSearchTerms("CI job build scans", jobNameLabel, jobName);
            }

            if (hasEnv("TRAVIS_EVENT_TYPE")) {
                buildScan.tag(System.getenv("TRAVIS_EVENT_TYPE"));
            }
        }
    }

    public static boolean isCi() {
        return isJenkins() || isTeamCity() || isCircleCI() || isBamboo() || isGitHubActions() || isGitLab() || isTravis();
    }

    public static boolean isJenkins() {
        return hasEnv("JENKINS_URL");
    }

    public static boolean isTeamCity() {
        return hasEnv("TEAMCITY_VERSION");
    }

    public static boolean isCircleCI() {
        return hasEnv("CIRCLE_BUILD_URL");
    }

    public static boolean isBamboo() {
        return hasEnv("bamboo_resultsUrl");
    }

    public static boolean isGitHubActions() {
        return hasEnv("GITHUB_ACTIONS");
    }

    public static boolean isGitLab() {
        return hasEnv("GITLAB_CI");
    }

    public static boolean isTravis() {
        return hasEnv("TRAVIS_JOB_ID");
    }

    public void addGitMetadata() {
        buildScan.background(buildScan -> {
            if (!isGitInstalled()) {
                return;
            }

            String gitCommitId = execAndGetStdout("git", "rev-parse", "--short=8", "--verify", "HEAD");
            String gitBranchName = execAndGetStdout("git", "rev-parse", "--abbrev-ref", "HEAD");
            String gitStatus = execAndGetStdout("git", "status", "--porcelain");

            if (notEmpty(gitCommitId)) {
                String gitCommitIdLabel = "Git commit id";
                buildScan.value(gitCommitIdLabel, gitCommitId);
                addCustomLinkWithSearchTerms("Git commit id build scans", gitCommitIdLabel, gitCommitId);

                String originUrl = execAndGetStdout("git", "config", "--get", "remote.origin.url");
                if (notEmpty(originUrl)) {
                    if (originUrl.contains("github.com/") || originUrl.contains("github.com:")) {
                        String repoPath = originUrl.substring(originUrl.indexOf("github.com") + 11);
                        if (repoPath.endsWith(".git")) {
                            repoPath = repoPath.substring(0, repoPath.length() - 4);
                        }
                        buildScan.link("Github Source", "https://github.com/" + repoPath + "/tree/" + gitCommitId);
                    } else if (originUrl.contains("gitlab.com/") || originUrl.contains("gitlab.com:")) {
                        String repoPath = originUrl.substring(originUrl.indexOf("gitlab.com") + 11);
                        if (repoPath.endsWith(".git")) {
                            repoPath = repoPath.substring(0, repoPath.length() - 4);
                        }
                        buildScan.link("GitLab Source", "https://gitlab.com/" + repoPath + "/-/commit/" + gitCommitId);
                    }
                }
            }

            if (notEmpty(gitBranchName)) {
                buildScan.tag(gitBranchName);
                buildScan.value("Git branch", gitBranchName);
            }

            if (notEmpty(gitStatus)) {
                buildScan.tag("Dirty");
                buildScan.value("Git status", gitStatus);
            }
        });
    }

    public static boolean isGitInstalled() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("git --version");
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException ignored) {
            return false;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    public static String execAndGetStdout(String... args) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(args);
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            String stdout = readFully(process.getInputStream());
            String stderr = readFully(process.getErrorStream());
            if (finished && process.exitValue() == 0) {
                return trimAtEnd(stdout);
            }
            return null;
        } catch (InterruptedException | IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private static String readFully(InputStream is) {
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
    }

    void addTestParallelization() {
        allProjects(p ->
                p.getTasks().withType(Test.class).configureEach(test ->
                        test.doFirst(__ ->
                                buildScan.value(test.getIdentityPath() + "#maxParallelForks", String.valueOf(test.getMaxParallelForks())))));
    }

    void addTestSystemProperties() {
        allProjects(p ->
                p.getTasks().withType(Test.class).configureEach(test ->
                        test.doFirst(__ ->
                                test.getSystemProperties().forEach((key, val) ->
                                        buildScan.value(test.getIdentityPath() + "#sysProps-" + key, hash(val)))
                        )));
    }

    private void allProjects(Action<Project> action) {
        rootProject.allprojects(action);
    }

    private static boolean hasEnv(String var) {
        return notEmpty(System.getenv(var));
    }

    private boolean hasSystemProp(String property) {
        return notEmpty(System.getProperty(property));
    }

    private static boolean notEmpty(String value) {
        return value != null && value.length() > 0;
    }

    public void addCustomLinkWithSearchTerms(String title, String name, String value) {
        // This fails with Build Scan plugin v1.16.
        // As a workaround, you can specify the BuildScan server URL directly: e.g. `def server = "http://localhost"`
        final String server = buildScan.getServer();

        if (notEmpty(server)) {
            String searchParams = customValueSearchParams(Map.of(name, value));
            String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
            buildScan.link(title, url);
        }

    }

    public static String customValueSearchParams(Map<String, String> search) {
        return search.entrySet().stream()
                .map(entry -> "search.names=" + urlEncode(entry.getKey()) + "&search.values=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    public static String appendIfMissing(String str, String suffix) {
        return str.endsWith(suffix) ? str : str + suffix;
    }

    public static String trimAtEnd(String str) {
        return ("x" + str).trim().substring(1);
    }

    public static String urlEncode(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    public static Properties readPropertiesFile(String name) {
        final Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(name)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
        return properties;
    }

    private String hash(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value);
        byte[] encodedHash = messageDigest.digest(string.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < encodedHash.length / 4; i++) {
            String hex = Integer.toHexString(0xff & encodedHash[i]);
            if (hex.length() == 1) {
                hexString.append("0");
            }
            hexString.append(hex);
        }
        hexString.append("...");
        return hexString.toString();
    }
}
