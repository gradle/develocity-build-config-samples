package com.myorg;

import com.gradle.develocity.agent.maven.api.scan.BuildScanApi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Utils {

    private Utils() {
    }

    static void captureProperty(BuildScanApi buildScan, String name, String value, Supplier<String> getDevelocityServer) {
        buildScan.tag(value);
        buildScan.value(name, value);
        capturePropertyLink(buildScan, name, value, getDevelocityServer);
    }

    static void capturePropertyError(BuildScanApi buildScan, String name, String message) {
        buildScan.value(name + " error", "Property not captured because: " + message);
    }

    private static void capturePropertyLink(BuildScanApi buildScan, String name, String value, Supplier<String> getDevelocityServer) {
        // Ensure server URL is fully configured by deferring call until the end of the build
        buildScan.buildFinished(result -> {
            String server = getDevelocityServer.get();
            server = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;
            String link = String.format("%s/scans?search.names=%s&search.values=%s", server, urlEncode(name), urlEncode(value));
            buildScan.link(name + " build scans", link);
        });
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
