package com.myorg;

import org.gradle.internal.classloader.VisitableURLClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * An example Gradle plugin for enabling and configuring Develocity features for
 * Gradle versions 5.x and higher.
 */
@SuppressWarnings({"NullableProblems"})
final class PluginLoader {

    private PluginLoader() {
    }

    static void resolveAndLoadIntoClassPath(List<PluginResolver> pluginsToResolve) {
        List<File> pluginJarFiles = resolveAllPlugins(pluginsToResolve);
        modifyPluginClassLoader(pluginJarFiles);
    }

    private static List<File> resolveAllPlugins(List<PluginResolver> pluginsToResolve) {
        List<File> resolvedPluginFiles = new ArrayList<>();
        pluginsToResolve.forEach(plugin -> resolvedPluginFiles.add(plugin.resolve()));
        return resolvedPluginFiles;
    }

    private static void modifyPluginClassLoader(List<File> pluginJarFiles) {
        VisitableURLClassLoader classLoader = (VisitableURLClassLoader) PluginLoader.class.getClassLoader();
        pluginJarFiles.stream().map(PluginLoader::toURL).forEach(classLoader::addURL);
    }

    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
