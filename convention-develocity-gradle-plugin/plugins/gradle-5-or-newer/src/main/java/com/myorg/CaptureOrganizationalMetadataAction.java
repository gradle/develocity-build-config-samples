package com.myorg;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.Action;
import org.gradle.api.provider.Provider;

final class CaptureOrganizationalMetadataAction implements Action<BuildScanConfiguration> {

    private static final String MY_ORGANIZATION_ID_NAME = "My Organization ID";

    private final Provider<String> develocityServer;

    CaptureOrganizationalMetadataAction(Provider<String> develocityServer) {
        this.develocityServer = develocityServer;
    }

    @Override
    public void execute(BuildScanConfiguration buildScan) {
        /* Example of reading an organization ID and capturing it in the published Build Scan
        try {
            String organizationIdValue = retrieveOrganizationId(...);
            Utils.captureProperty(buildScan, MY_ORGANIZATION_ID_NAME, organizationIdValue, develocityServer);
        } catch (Exception e) {
            Utils.capturePropertyError(buildScan, MY_ORGANIZATION_ID_NAME, e.getMessage());
        }
        */
    }

}
