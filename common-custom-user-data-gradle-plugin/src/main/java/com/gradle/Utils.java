package com.gradle;

import org.gradle.api.file.RegularFile;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.util.GradleVersion;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

final class Utils {

    static Optional<String> envVariable(String name, ProviderFactory providers) {
        if (isGradle65OrNewer()) {
            Provider<String> variable = providers.environmentVariable(name).forUseAtConfigurationTime();
            return Optional.ofNullable(variable.getOrNull());
        }
        return Optional.ofNullable(System.getenv(name));
    }

    static Optional<String> projectProperty(String name, ProviderFactory providers, Gradle gradle) {
        if (isGradle65OrNewer()) {
            // invalidate configuration cache if different Gradle property value is set on the cmd line,
            // but in any case access Gradle property directly since project properties set in a build script or
            // init script are not fetched by ProviderFactory.gradleProperty
            providers.gradleProperty(name).forUseAtConfigurationTime();
        }
        return Optional.ofNullable((String) gradle.getRootProject().findProperty(name));
    }

    static Optional<String> sysProperty(String name, ProviderFactory providers) {
        if (isGradle65OrNewer()) {
            Provider<String> property = providers.systemProperty(name).forUseAtConfigurationTime();
            return Optional.ofNullable(property.getOrNull());
        }
        return Optional.ofNullable(System.getProperty(name));
    }

    static Optional<Boolean> booleanSysProperty(String name, ProviderFactory providers) {
        return sysProperty(name, providers).map(Boolean::parseBoolean);
    }

    static Optional<Duration> durationSysProperty(String name, ProviderFactory providers) {
        return sysProperty(name, providers).map(Duration::parse);
    }

    static Optional<String> firstSysPropertyKeyStartingWith(String keyPrefix, ProviderFactory providers) {
        Optional<String> key = firstKeyStartingWith(keyPrefix, System.getProperties());
        if (isGradle65OrNewer()) {
            key.ifPresent(k -> providers.systemProperty(k).forUseAtConfigurationTime());
        }
        return key;
    }

    private static Optional<String> firstKeyStartingWith(String keyPrefix, Properties properties) {
        return properties.keySet().stream()
            .filter(s -> s instanceof String)
            .map(s -> (String) s)
            .filter(s -> s.startsWith(keyPrefix))
            .findFirst();
    }

    static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    static String stripPrefix(String prefix, String string) {
        return string.startsWith(prefix) ? string.substring(prefix.length()) : string;
    }

    static String appendIfMissing(String str, String suffix) {
        return str.endsWith(suffix) ? str : str + suffix;
    }

    static URI appendPathAndTrailingSlash(URI baseUri, String path) {
        if (isNotEmpty(path)) {
            String normalizedBasePath = appendIfMissing(baseUri.getPath(), "/");
            String normalizedPath = appendIfMissing(stripPrefix("/", path), "/");
            return baseUri.resolve(normalizedBasePath).resolve(normalizedPath);
        }
        return baseUri;
    }

    static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static String redactUserInfo(String url) {
        try {
            String userInfo = new URI(url).getUserInfo();
            return userInfo == null
                ? url
                : url.replace(userInfo + '@', "******@");
        } catch (URISyntaxException e) {
            return url;
        }
    }

    static Properties readPropertiesFile(String name, ProviderFactory providers, Gradle gradle) {
        try (InputStream input = readFile(name, providers, gradle)) {
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static InputStream readFile(String name, ProviderFactory providers, Gradle gradle) throws FileNotFoundException {
        if (isGradle65OrNewer()) {
            RegularFile file = gradle.getRootProject().getLayout().getProjectDirectory().file(name);
            Provider<byte[]> fileContent = providers.fileContents(file).getAsBytes().forUseAtConfigurationTime();
            return new ByteArrayInputStream(fileContent.getOrElse(new byte[0]));
        }
        return new FileInputStream(name);
    }

    static boolean execAndCheckSuccess(String... args) {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec(args);
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

    static String execAndGetStdOut(String... args) {
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Reader standard = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            try (Reader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()))) {
                String standardText = readFully(standard);
                String ignore = readFully(error);

                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                return finished && process.exitValue() == 0 ? trimAtEnd(standardText) : null;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
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

    private static boolean isGradle65OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.5")) >= 0;
    }

    private Utils() {
    }

}
