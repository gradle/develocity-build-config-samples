package com.myorg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

final class GitRepositoryProjectId {

    // CHANGE ME: the remote that points at the canonical repository
    private static final String GIT_REMOTE = "origin";

    // CHANGE ME: the separator placed between the path segments of the project ID
    private static final String SEPARATOR = "/";

    private static final int MAX_PROJECT_ID_LENGTH = 256;

    // scheme://[user@]host[:port]/path, e.g. https://github.com/acme-inc/my-project.git
    private static final Pattern URL = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*)://([^/]*)(.*)$");

    // [user@]host:path, e.g. git@github.com:acme-inc/my-project.git, where a single letter host is a Windows drive
    private static final Pattern SCP_LIKE = Pattern.compile("^(?:[^@/]+@)?([^@/:]{2,}):(.*)$");

    // user@host/path, e.g. git@github.com/acme-inc/my-project.git
    private static final Pattern USER_HOST_PATH = Pattern.compile("^[^@/:]+@([^/:]+)(/.*)$");

    private GitRepositoryProjectId() {
    }

    static Optional<String> fromGitRepository(File directory) {
        return readRemoteUrl(directory).flatMap(GitRepositoryProjectId::fromRemoteUrl);
    }

    private static Optional<String> readRemoteUrl(File directory) {
        Process process;
        try {
            process = new ProcessBuilder("git", "config", "--get", "remote." + GIT_REMOTE + ".url").directory(directory).start();
        } catch (IOException e) {
            return Optional.empty();
        }

        try (InputStream stdout = process.getInputStream()) {
            String remoteUrl = readFully(stdout);
            boolean succeeded = process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
            return succeeded ? Optional.of(remoteUrl) : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            process.destroyForcibly();
        }
    }

    private static Optional<String> fromRemoteUrl(String remoteUrl) {
        String url = remoteUrl.trim();
        Matcher matcher = URL.matcher(url);
        if (matcher.matches()) {
            return matcher.group(1).equalsIgnoreCase("file")
                    ? Optional.empty()
                    : fromHostAndPath(stripUserInfoAndPort(matcher.group(2)), stripQueryAndFragment(matcher.group(3)));
        }
        matcher = SCP_LIKE.matcher(url);
        if (matcher.matches()) {
            return fromHostAndPath(matcher.group(1), matcher.group(2));
        }
        matcher = USER_HOST_PATH.matcher(url);
        if (matcher.matches()) {
            return fromHostAndPath(matcher.group(1), matcher.group(2));
        }
        return Optional.empty();
    }

    private static Optional<String> fromHostAndPath(String host, String path) {
        if (path.contains("\\")) {
            return Optional.empty();
        }

        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        if (!segments.isEmpty()) {
            int last = segments.size() - 1;
            String repository = stripGitSuffix(segments.get(last));
            if (repository.isEmpty()) {
                segments.remove(last);
            } else {
                segments.set(last, repository);
            }
        }

        applyHostingConventions(host.toLowerCase(Locale.ROOT), segments);
        if (segments.isEmpty()) {
            return Optional.empty();
        }

        String projectId = String.join(SEPARATOR, segments).toLowerCase(Locale.ROOT);
        return isValid(projectId) ? Optional.of(projectId) : Optional.empty();
    }

    private static void applyHostingConventions(String host, List<String> segments) {
        if (segments.size() >= 2 && segments.get(segments.size() - 2).equals("_git")) {
            segments.remove(segments.size() - 2);
        }

        if (host.equals("ssh.dev.azure.com") || host.equals("vs-ssh.visualstudio.com")) {
            if (!segments.isEmpty() && segments.get(0).equalsIgnoreCase("v3")) {
                segments.remove(0);
            }
        } else if (host.endsWith(".visualstudio.com")) {
            segments.removeIf(segment -> segment.equalsIgnoreCase("DefaultCollection"));
            segments.add(0, host.substring(0, host.indexOf('.')));
        }

        if (segments.size() >= 3 && segments.get(segments.size() - 3).equals("scm")) {
            segments.subList(0, segments.size() - 2).clear();
        }
    }

    private static boolean isValid(String projectId) {
        return projectId.length() <= MAX_PROJECT_ID_LENGTH && projectId.chars().noneMatch(Character::isWhitespace);
    }

    private static String stripUserInfoAndPort(String authority) {
        String hostAndPort = authority.substring(authority.lastIndexOf('@') + 1);
        int portSeparator = hostAndPort.lastIndexOf(':');
        return portSeparator < 0 || hostAndPort.endsWith("]") ? hostAndPort : hostAndPort.substring(0, portSeparator);
    }

    private static String stripQueryAndFragment(String path) {
        int end = path.length();
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        if (query >= 0) {
            end = Math.min(end, query);
        }
        if (fragment >= 0) {
            end = Math.min(end, fragment);
        }
        return path.substring(0, end);
    }

    private static String stripGitSuffix(String repository) {
        return repository.endsWith(".git") ? repository.substring(0, repository.length() - ".git".length()) : repository;
    }

    private static String readFully(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), UTF_8);
    }

}
