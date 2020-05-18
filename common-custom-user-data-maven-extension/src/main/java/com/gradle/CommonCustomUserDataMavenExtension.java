package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(role = AbstractMavenLifecycleParticipant.class)
public final class CommonCustomUserDataMavenExtension extends AbstractMavenLifecycleParticipant {

    private static final Logger LOG = LoggerFactory.getLogger(CommonCustomUserDataMavenExtension.class);

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        LOG.info("Executing extension: {}", getClass().getSimpleName());
        BuildScanApi buildScan = BuildScanApiAccessor.lookup(session, getClass());
        if (buildScan != null) {
            LOG.debug("Capturing custom user data in build scan");
            CustomUserData.addToBuildScan(buildScan);
            LOG.debug("Finished capturing custom user data in build scans");
        }
    }

}
