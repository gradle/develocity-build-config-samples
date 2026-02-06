package com.myorg;

import com.myorg.configurable.BuildScanConfigurable;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class CaptureOrganizationalMetadataAction implements Consumer<BuildScanConfigurable> {

    private static final String MY_ORGANIZATION_ID_NAME = "My Organization ID";

    private final Supplier<String> getDevelocityServer;

    CaptureOrganizationalMetadataAction(Supplier<String> getDevelocityServer) {
        this.getDevelocityServer = getDevelocityServer;
    }

    @Override
    public void accept(BuildScanConfigurable buildScan) {
        /* Example of reading an organization ID and capturing it in the published Build Scan
        try {
            String organizationIdValue = retrieveOrganizationId(...);
            Utils.captureProperty(buildScan, MY_ORGANIZATION_ID_NAME, organizationIdValue, getDevelocityServer);
        } catch (Exception e) {
            Utils.capturePropertyError(buildScan, MY_ORGANIZATION_ID_NAME, e.getMessage());
        }
        */
    }

}
