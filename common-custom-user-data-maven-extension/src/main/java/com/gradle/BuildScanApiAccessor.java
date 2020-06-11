package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.MavenExecutionException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.Optional;

import static java.util.Comparator.comparing;

final class BuildScanApiAccessor {

    private static final String PACKAGE = "com.gradle.maven.extension.api.scan";
    private static final String BUILD_SCAN_API_CONTAINER_OBJECT = PACKAGE + ".BuildScanApi";

    static BuildScanApi lookup(PlexusContainer container, Class<?> extensionClass) throws MavenExecutionException {
        ensureBuildScanApiIsAccessible(extensionClass);
        return lookupBuildScanApi(container);
    }

    /**
     * Workaround for https://issues.apache.org/jira/browse/MNG-6906
     */
    private static void ensureBuildScanApiIsAccessible(Class<?> extensionClass) throws MavenExecutionException {
        ClassLoader classLoader = extensionClass.getClassLoader();
        if (classLoader instanceof ClassRealm) {
            ClassRealm extensionRealm = (ClassRealm) classLoader;
            if (!"maven.ext".equals(extensionRealm.getId())) {
                Optional<ClassRealm> sourceRealm = extensionRealm.getWorld().getRealms().stream()
                    .filter(realm -> realm.getId().contains("com.gradle:gradle-enterprise-maven-extension") || realm.getId().equals("maven.ext"))
                    .max(comparing((ClassRealm realm) -> realm.getId().length()));
                if (sourceRealm.isPresent()) {
                    String sourceRealmId = sourceRealm.get().getId();
                    try {
                        extensionRealm.importFrom(sourceRealmId, PACKAGE);
                    } catch (Exception e) {
                        throw new MavenExecutionException("Could not import package from realm with id " + sourceRealmId, e);
                    }
                }
            }
        }
    }

    private static BuildScanApi lookupBuildScanApi(PlexusContainer container) throws MavenExecutionException {
        if (!container.hasComponent(BUILD_SCAN_API_CONTAINER_OBJECT)) {
            return null;
        } else {
            try {
                return (BuildScanApi) container.lookup(BUILD_SCAN_API_CONTAINER_OBJECT);
            } catch (ComponentLookupException e) {
                throw new MavenExecutionException(String.format("Cannot look up object in container: %s", BUILD_SCAN_API_CONTAINER_OBJECT), e);
            }
        }
    }

    private BuildScanApiAccessor() {
    }

}
