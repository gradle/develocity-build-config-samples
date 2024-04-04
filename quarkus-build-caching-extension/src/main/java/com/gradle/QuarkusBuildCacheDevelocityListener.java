package com.gradle;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
@Component(
        role = DevelocityListener.class,
        hint = "quarkus-build-cache",
        description = "Make the Quarkus build goal cacheable"
)
public final class QuarkusBuildCacheDevelocityListener implements DevelocityListener {

    private final Logger LOGGER = LoggerFactory.getLogger(QuarkusBuildCacheDevelocityListener.class);

    private final QuarkusBuildCache quarkusBuildCache = new QuarkusBuildCache();

    @Override
    public void configure(DevelocityApi api, MavenSession session) {
        LOGGER.debug("Executing extension: " + getClass().getSimpleName());
        quarkusBuildCache.configureBuildCache(api.getBuildCache());
    }

}
