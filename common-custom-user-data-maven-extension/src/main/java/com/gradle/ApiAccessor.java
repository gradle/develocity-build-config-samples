package com.gradle;

import org.apache.maven.MavenExecutionException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.Optional;

import static java.util.Comparator.comparing;

final class ApiAccessor {

    static <T> T lookup(Class<T> componentClass, String componentPackage, String componentRole, PlexusContainer container, Class<?> extensionClass) throws MavenExecutionException {
        ensureClassIsAccessible(extensionClass, componentPackage);
        return lookupClass(componentClass, componentRole, container);
    }

    /**
     * Workaround for https://issues.apache.org/jira/browse/MNG-6906
     */
    private static void ensureClassIsAccessible(Class<?> extensionClass, String componentPackage) throws MavenExecutionException {
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
                        extensionRealm.importFrom(sourceRealmId, componentPackage);
                    } catch (Exception e) {
                        throw new MavenExecutionException("Could not import package from realm with id " + sourceRealmId, e);
                    }
                }
            }
        }
    }

    private static <T> T lookupClass(Class<T> componentClass, String component, PlexusContainer container) throws MavenExecutionException {
        if (!container.hasComponent(component)) {
            return null;
        } else {
            try {
                return componentClass.cast(container.lookup(component));
            } catch (ComponentLookupException e) {
                throw new MavenExecutionException(String.format("Cannot look up object in container: %s", component), e);
            }
        }
    }

    private ApiAccessor(){}

}
