package com.myorg;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.provider.Provider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Utils {

    private Utils() {
    }

    static void captureProperty(BuildScanConfiguration buildScan, String name, String value, Provider<String> develocityServer) {
        buildScan.tag(value);
        buildScan.value(name, value);
        capturePropertyLink(buildScan, name, value, develocityServer);
    }

    static void capturePropertyError(BuildScanConfiguration buildScan, String name, String message) {
        buildScan.value(name + " error", "Property not captured because: " + message);
    }

    private static void capturePropertyLink(BuildScanConfiguration buildScan, String name, String value, Provider<String> develocityServer) {
        // Ensure server URL is fully configured by deferring call until the end of the build
        buildScan.buildFinished(result -> {
            String server = develocityServer.get();
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
