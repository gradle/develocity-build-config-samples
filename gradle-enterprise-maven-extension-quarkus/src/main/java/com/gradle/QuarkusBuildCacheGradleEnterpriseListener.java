package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.GradleEnterpriseListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
@Component(
        role = GradleEnterpriseListener.class,
        hint = "quarkus-build-cache",
        description = "Make the Quarkus build goal cacheable"
)
public final class QuarkusBuildCacheGradleEnterpriseListener implements GradleEnterpriseListener {

    private final Logger LOGGER = LoggerFactory.getLogger(QuarkusBuildCacheGradleEnterpriseListener.class);

    private final QuarkusBuildCache quarkusBuildCache = new QuarkusBuildCache();

    @Override
    public void configure(GradleEnterpriseApi api, MavenSession session) {
        LOGGER.info("Executing extension: " + getClass().getSimpleName());
        quarkusBuildCache.configureBuildCache(api.getBuildCache());
    }

}
