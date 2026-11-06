package com.myorg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class GradlePluginPortalPluginResolver implements PluginResolver {

    private static final Path pluginJarCacheDirectory = Paths.get(System.getProperty("user.home"), ".develocity-jars");

    private final String pluginJarUrl;

    public GradlePluginPortalPluginResolver(String pluginJarUrl) {
        this.pluginJarUrl = pluginJarUrl;
    }

    @Override
    public File resolve() {
        Path destinationPath = getDestinationPath();
        createParentDirectories(destinationPath);
        if (!Files.exists(destinationPath)) {
            performDownload(destinationPath);
        }
        return destinationPath.toFile();
    }

    private Path getDestinationPath() {
        URL url = toURL(pluginJarUrl);
        String urlPath = url.getPath();
        String filePath = trimM2Prefix(urlPath);
        return Paths.get(pluginJarCacheDirectory.toString(), filePath);
    }

    private static String trimM2Prefix(String path) {
        if (path.startsWith("/m2")) {
            return path.substring(3);
        }
        return path;
    }

    private static void createParentDirectories(Path path) {
        Path parent = path.getParent();
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new RuntimeException("Could not create parent directory: " + parent, e);
        }
    }

    private void performDownload(Path destinationPath) {
        try {
            try (InputStream in = new URL(pluginJarUrl).openStream()) {
                Files.copy(in, destinationPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not download plugin JAR file: " + pluginJarUrl, e);
        }
    }

    private static URL toURL(String url) {
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
