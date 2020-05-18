package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Component(role = AbstractMavenLifecycleParticipant.class)
public final class CommonCustomUserDataMavenExtension extends AbstractMavenLifecycleParticipant {

    private static final Logger LOG = LoggerFactory.getLogger(CommonCustomUserDataMavenExtension.class);

    private final PlexusContainer container;

    @Inject
    public CommonCustomUserDataMavenExtension(PlexusContainer container) {
        this.container = container;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        LOG.debug("Executing extension: {}", getClass().getSimpleName());
        BuildScanApi buildScan = BuildScanApiAccessor.lookup(container, getClass());
        if (buildScan != null) {
            LOG.debug("Capturing custom user data in build scan");
            CustomUserData.addToBuildScan(buildScan);
            LOG.debug("Finished capturing custom user data in build scans");
        }
    }

}
