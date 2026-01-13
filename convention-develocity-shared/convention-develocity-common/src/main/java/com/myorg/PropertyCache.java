package com.myorg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

final class PropertyCache {

    // CHANGE ME: Apply your desired TTL here.
    private static final Duration ttl = Duration.ofDays(5);

    private static final String DELIMITER = ";";

    private final Path propertyCacheFile;

    PropertyCache(Path propertyCacheFile) {
        this.propertyCacheFile = propertyCacheFile;
    }

    Optional<String> loadProperty() {
        try {
            return loadProperty(propertyCacheFile);
        } catch (IOException ignored) { }
        return Optional.empty();
    }

    void writeProperty(String value) {
        try {
            writeProperty(propertyCacheFile, value);
        } catch (IOException ignored) { }
    }

    private static Optional<String> loadProperty(Path propertyCacheFile) throws IOException {
        if (Files.notExists(propertyCacheFile)) {
            return Optional.empty();
        }

        try {
            String[] content = new String(Files.readAllBytes(propertyCacheFile)).split(DELIMITER);
            if (content.length >= 2) {
                Instant createdOn = Instant.parse(content[0]);
                Duration age = Duration.between(createdOn, Instant.now());
                if (age.compareTo(ttl) < 0) {
                    String value = Arrays.stream(content).skip(1).collect(Collectors.joining(DELIMITER));
                    return Optional.of(value);
                }
            }
        } catch (IOException | DateTimeParseException ignored) { }

        invalidateProperty(propertyCacheFile);
        return Optional.empty();
    }

    private static void writeProperty(Path propertyCacheFile, String value) throws IOException {
        if (Files.notExists(propertyCacheFile.getParent())) {
            Files.createDirectories(propertyCacheFile.getParent());
        }

        String content = Instant.now() + DELIMITER + value;
        Files.write(propertyCacheFile, content.getBytes());
    }

    private static void invalidateProperty(Path propertyCacheFile) {
        try {
            Files.delete(propertyCacheFile);
        } catch (IOException ignored) { }
    }
}
