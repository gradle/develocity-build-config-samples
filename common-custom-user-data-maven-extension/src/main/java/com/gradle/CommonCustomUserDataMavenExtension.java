package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;

@Component(role = AbstractMavenLifecycleParticipant.class)
public final class CommonCustomUserDataMavenExtension extends AbstractMavenLifecycleParticipant {

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
        BuildScanApi buildScan = BuildScanApiAccessor.lookup(container, getClass());
        if (buildScan != null) {
            logger.debug("Capturing custom user data in build scan");
            CustomUserData.addToBuildScan(buildScan);
            GroovyScriptUserData.addToBuildScan(session, buildScan, logger);
            logger.debug("Finished capturing custom user data in build scans");
        }
    }

}
