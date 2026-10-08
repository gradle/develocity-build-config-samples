package com.myorg;

import org.gradle.api.IsolatedAction;
import org.gradle.api.Project;
import org.gradle.api.configuration.BuildFeatures;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

final class IsolatedTestRetryAction implements IsolatedAction<Project> {

    static boolean isIsolatedProjectsActive(ObjectFactory objects) {
        return objects.newInstance(BuildFeaturesService.class).getBuildFeatures().getIsolatedProjects().getActive().get();
    }

    static void register(Gradle gradle) {
        gradle.getLifecycle().beforeProject(new IsolatedTestRetryAction());
    }

    @Override
    public void execute(Project project) {
        ConventionDevelocityGradlePlugin.applyTestRetry(project);
    }

    public abstract static class BuildFeaturesService {

        @Inject
        public abstract BuildFeatures getBuildFeatures();

    }

}
