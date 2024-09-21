package com.gradle;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuarkusBuildCachingDevelocityListener implements DevelocityListener {

    private final Logger LOGGER = LoggerFactory.getLogger(QuarkusBuildCachingDevelocityListener.class);

    private final QuarkusBuildCache quarkusBuildCache = new QuarkusBuildCache();

    @Override
    public void configure(DevelocityApi api, MavenSession session) {
        LOGGER.debug("Executing extension: " + getClass().getSimpleName());
        quarkusBuildCache.configureBuildCache(api.getBuildCache());
    }

}
