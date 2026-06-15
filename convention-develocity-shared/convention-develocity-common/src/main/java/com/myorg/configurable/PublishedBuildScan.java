package com.myorg.configurable;

import java.net.URI;

public final class PublishedBuildScan {

    private final String buildScanId;
    private final URI buildScanUri;

    PublishedBuildScan(String buildScanId, URI buildScanUri) {
        this.buildScanId = buildScanId;
        this.buildScanUri = buildScanUri;
    }

    public String getBuildScanId() {
        return buildScanId;
    }

    public URI getBuildScanUri() {
        return buildScanUri;
    }

}
