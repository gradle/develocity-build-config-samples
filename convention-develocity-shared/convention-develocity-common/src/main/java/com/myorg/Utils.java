package com.myorg;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Utils {

    private static final Pattern GIT_REPO_URI_PATTERN = Pattern.compile("^(?:(?:https://|git://)(?:.+:.+@)?|(?:ssh)?.*?@)(.*?(?:github|gitlab).*?)(?:/|:[0-9]*?/|:)(.*?)(?:\\.git)?$");

    private static final Gson GSON = new Gson();

    private Utils() {
    }

    static Optional<OwnerAndRepository> readOwnerAndRepository(Path projectDirectory) throws IOException, InterruptedException {
        String gitRepository = execAndGetStdOut(projectDirectory, "git", "config", "--get", "remote.origin.url");
        if (gitRepository == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(extractOwnerAndRepository(gitRepository));
    }

    static <T> T makeHttpRequest(String url, TypeToken<T> responseType, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        headers.forEach(connection::setRequestProperty);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String responseBody = Utils.readFully(reader);
                return GSON.fromJson(responseBody, responseType);
            }
        }

        throw new IOException(String.format("Received HTTP response code %s from %s.", responseCode, url));
    }

    static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static Optional<String> readStackTrace(Throwable throwable) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return Optional.of(sw.toString());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String execAndGetStdOut(Path workingDirectory, String... args) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(args, null, workingDirectory.toFile());

        try (Reader standard = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            try (Reader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()))) {
                String standardText = readFully(standard);
                String ignore = readFully(error);

                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                return finished && process.exitValue() == 0 ? trimAtEnd(standardText) : null;
            }
        } finally {
            process.destroyForcibly();
        }
    }

    private static String readFully(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int nRead;
        while ((nRead = reader.read(buf)) != -1) {
            sb.append(buf, 0, nRead);
        }
        return sb.toString();
    }

    private static String trimAtEnd(String str) {
        return ('x' + str).trim().substring(1);
    }

    private static OwnerAndRepository extractOwnerAndRepository(String gitRepository) {
        Matcher matcher = GIT_REPO_URI_PATTERN.matcher(gitRepository);
        if (!matcher.matches() || matcher.groupCount() != 2) {
            return null;
        }

        String path = matcher.group(2).startsWith("/") ? matcher.group(2).substring(1) : matcher.group(2);
        String[] parts = path.split("/");

        if (parts.length != 2) {
            return null;
        }

        return new OwnerAndRepository(parts[0], parts[1]);
    }

    static final class OwnerAndRepository {

        final String owner;
        final String repository;

        private OwnerAndRepository(String owner, String repository) {
            this.owner = owner;
            this.repository = repository;
        }
    }

}
