package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.GradleEnterpriseListener;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@SuppressWarnings("unused")
@Component(
        role = GradleEnterpriseListener.class,
        hint = "common-custom-user-data",
        description = "Captures common custom user data in Maven build scans"
)
public final class CommonCustomUserDataGradleEnterpriseListener implements GradleEnterpriseListener {

    @Requirement
    private Logger logger;

    @Override
    public void configure(GradleEnterpriseApi api, MavenSession session) throws Exception {
        logger.debug("Executing extension: " + getClass().getSimpleName());
        CustomGradleEnterpriseConfig customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig();

        logger.debug("Configuring Gradle Enterprise");
        customGradleEnterpriseConfig.configureGradleEnterprise(api);
        logger.debug("Finished configuring Gradle Enterprise");

        logger.debug("Configuring build scan publishing and applying build scan enhancements");
        BuildScanApi buildScan = api.getBuildScan();
        customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
        new CustomBuildScanEnhancements(buildScan, session).apply();
        logger.debug("Finished configuring build scan publishing and applying build scan enhancements");

        logger.debug("Configuring build cache");
        BuildCacheApi buildCache = api.getBuildCache();
        customGradleEnterpriseConfig.configureBuildCache(buildCache);
        logger.debug("Finished configuring build cache");

        GroovyScriptUserData.evaluate(session, api, logger);
        SystemPropertyOverrides.configureBuildCache(buildCache);
    }

}
