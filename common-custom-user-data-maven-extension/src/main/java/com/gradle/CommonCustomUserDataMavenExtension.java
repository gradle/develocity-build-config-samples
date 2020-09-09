package com.gradle;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "common-custom-user-data")
public final class CommonCustomUserDataMavenExtension extends AbstractMavenLifecycleParticipant {

    private static final String BUILD_CACHE_API_PACKAGE = "com.gradle.maven.extension.api.cache";
    private static final String BUILD_CACHE_API_CONTAINER_OBJECT = BUILD_CACHE_API_PACKAGE + ".BuildCacheApi";

    private static final String BUILD_SCAN_API_PACKAGE = "com.gradle.maven.extension.api.scan";
    private static final String BUILD_SCAN_API_CONTAINER_OBJECT = BUILD_SCAN_API_PACKAGE + ".BuildScanApi";

    private final PlexusContainer container;
    private final Logger logger;

    @Inject
    public CommonCustomUserDataMavenExtension(PlexusContainer container, Logger logger) {
        this.container = container;
        this.logger = logger;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        logger.debug("Executing extension: " + getClass().getSimpleName());
        BuildScanApi buildScan = ApiAccessor.lookup(BuildScanApi.class, BUILD_SCAN_API_PACKAGE, BUILD_SCAN_API_CONTAINER_OBJECT, container, getClass());
        if (buildScan != null) {
            logger.debug("Capturing custom user data in build scan");
            CustomUserData.addToBuildScan(buildScan);
            logger.debug("Finished capturing custom user data in build scans");
        }
        BuildCacheApi buildCache = ApiAccessor.lookup(BuildCacheApi.class, BUILD_CACHE_API_PACKAGE, BUILD_CACHE_API_CONTAINER_OBJECT, container, getClass());
        GroovyScriptUserData.addToApis(session, buildScan, buildCache, logger);
    }

}
