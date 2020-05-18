package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import static java.util.Comparator.comparing;

final class BuildScanApiAccessor {

    private static final String PACKAGE = "com.gradle.maven.extension.api.scan";
    private static final String BUILD_SCAN_API_SESSION_OBJECT = PACKAGE + ".BuildScanApi";

    @SuppressWarnings("deprecation")
    static BuildScanApi lookup(MavenSession session, Class<?> extensionClass) throws MavenExecutionException {
        ensureBuildScanApiIsAccessible(extensionClass);
        try {
            return (BuildScanApi) session.lookup(BUILD_SCAN_API_SESSION_OBJECT);
        } catch (ComponentLookupException e) {
            throw new MavenExecutionException(String.format("Cannot look up object in session: %s", BUILD_SCAN_API_SESSION_OBJECT), e);
        }
    }

    /**
     * Workaround for https://issues.apache.org/jira/browse/MNG-6906
     */
    private static void ensureBuildScanApiIsAccessible(Class<?> extensionClass) {
        ClassLoader classLoader = extensionClass.getClassLoader();
        if (classLoader instanceof ClassRealm) {
            ClassRealm extensionRealm = (ClassRealm) classLoader;
            if (!"maven.ext".equals(extensionRealm.getId())) {
                extensionRealm.getWorld().getRealms().stream()
                    .filter(realm -> realm.getId().contains("com.gradle:gradle-enterprise-maven-extension") || realm.getId().equals("maven.ext"))
                    .max(comparing((ClassRealm realm) -> realm.getId().length()))
                    .ifPresent(realm -> {
                        try {
                            extensionRealm.importFrom(realm.getId(), PACKAGE);
                        } catch (Exception e) {
                            throw new RuntimeException("Could not import package from realm with id " + realm.getId(), e);
                        }
                    });
            }
        }
    }

    private BuildScanApiAccessor() {
    }

}
