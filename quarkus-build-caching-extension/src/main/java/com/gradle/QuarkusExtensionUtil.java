package com.gradle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

final class QuarkusExtensionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusBuildCache.class);
    private static final String LOG_PREFIX = "[quarkus-build-caching-extension] ";

    static Properties loadProperties(String baseDir, String propertyFile) {
        Properties props = new Properties();
        File configFile = new File(baseDir, propertyFile);

        if (configFile.exists()) {
            try (InputStream input = Files.newInputStream(configFile.toPath())) {
                props.load(input);
            } catch (IOException e) {
                LOGGER.error(getLogMessage("Error while loading " + propertyFile), e);
            }
        } else {
            LOGGER.debug(getLogMessage(propertyFile + " not found"));
        }

        return props;
    }

    static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    static String getLogMessage(String msg) {
        return LOG_PREFIX + msg;
    }

}
