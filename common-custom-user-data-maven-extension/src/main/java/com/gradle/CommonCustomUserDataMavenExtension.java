package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;

@Component(
    role = AbstractMavenLifecycleParticipant.class,
    hint = "common-custom-user-data",
    description = "Captures common custom user data in Maven build scans"
)
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

        GradleEnterpriseApi gradleEnterprise = ApiAccessor.lookupGradleEnterpriseApi(container, getClass());
        if (gradleEnterprise != null) {
            CustomGradleEnterpriseConfig customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig();

            logger.debug("Configuring Gradle Enterprise");
            customGradleEnterpriseConfig.configureGradleEnterprise(gradleEnterprise);
            logger.debug("Finished configuring Gradle Enterprise");

            logger.debug("Configuring build scan publishing and applying build scan enhancements");
            BuildScanApi buildScan = gradleEnterprise.getBuildScan();
            customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
            new CustomBuildScanEnhancements(buildScan, session).apply();
            logger.debug("Finished configuring build scan publishing and applying build scan enhancements");

            logger.debug("Configuring build cache");
            BuildCacheApi buildCache = gradleEnterprise.getBuildCache();
            customGradleEnterpriseConfig.configureBuildCache(buildCache);
            SystemPropertyOverrides.configureBuildCache(buildCache);
            logger.debug("Finished configuring build cache");

            GroovyScriptUserData.evaluate(session, gradleEnterprise, logger);
        } else {
            logger.debug("Could not find GradleEnterpriseApi component in Plexus container");
        }
    }

}
