package com.gradle;

import com.gradle.develocity.agent.maven.api.cache.BuildCacheApi;
import com.gradle.develocity.agent.maven.api.cache.MojoMetadataProvider;
import com.gradle.develocity.agent.maven.api.cache.NormalizationProvider;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Caching instructions for the Quarkus build goal.
 */
final class QuarkusBuildCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusBuildCache.class);

    // Quarkus' configuration keys
    private static final List<String> QUARKUS_CONFIG_KEY_NATIVE_CONTAINER_BUILD = Arrays.asList("quarkus.native.container-build", "quarkus.native.remote-container-build");
    private static final String QUARKUS_CONFIG_KEY_NATIVE_BUILDER_IMAGE = "quarkus.native.builder-image";
    // quarkus.package.type is replaced by quarkus.native.enabled / quarkus.package.jar.type in Quarkus 3.9
    private static final String QUARKUS_CONFIG_KEY_DEPRECATED_PACKAGE_TYPE = "quarkus.package.type";
    private static final String QUARKUS_CONFIG_KEY_NATIVE = "quarkus.native.enabled";
    private static final String QUARKUS_CONFIG_KEY_JAR_TYPE = "quarkus.package.jar.type";
    private static final String QUARKUS_CONFIG_KEY_GRAALVM_HOME = "quarkus.native.graalvm-home";
    private static final String QUARKUS_CONFIG_KEY_JAVA_HOME = "quarkus.native.java-home";
    private static final String PACKAGE_NATIVE = "native";
    private static final String TEST_GOAL_KEY_ADD_QUARKUS_INPUTS = "addQuarkusInputs";
    // Quarkus' cacheable package types
    private static final List<String> QUARKUS_CACHEABLE_JAR_TYPES = Arrays.asList("jar", "legacy-jar", "uber-jar");

    // Quarkus' properties which are considered as file inputs
    private static final List<String> QUARKUS_KEYS_AS_FILE_INPUTS = Arrays.asList("quarkus.docker.dockerfile-native-path", "quarkus.docker.dockerfile-jvm-path", "quarkus.openshift.jvm-dockerfile", "quarkus.openshift.native-dockerfile");

    // Quarkus' properties which should be ignored (the JDK / GraalVM version are extra inputs)
    private static final List<String> QUARKUS_IGNORED_PROPERTIES = Arrays.asList(QUARKUS_CONFIG_KEY_GRAALVM_HOME, QUARKUS_CONFIG_KEY_JAVA_HOME);

    void configureBuildCache(BuildCacheApi buildCache) {
        buildCache.registerNormalizationProvider(context -> {
            QuarkusExtensionConfiguration extensionConfiguration = new QuarkusExtensionConfiguration(context.getProject());
            configureNormalization(context, extensionConfiguration);
        });
        buildCache.registerMojoMetadataProvider(context -> {
            QuarkusExtensionConfiguration extensionConfiguration = new QuarkusExtensionConfiguration(context.getProject());

            context.withPlugin("quarkus-maven-plugin", () -> {
                configureQuarkusBuildGoal(context, extensionConfiguration);
            });
            context.withPlugin("maven-surefire-plugin", () -> {
                configureQuarkusExtraTestInputs(context, extensionConfiguration);
            });
            context.withPlugin("maven-failsafe-plugin", () -> {
                configureQuarkusExtraTestInputs(context, extensionConfiguration);
            });
        });
    }

    private void configureNormalization(NormalizationProvider.Context context, QuarkusExtensionConfiguration extensionConfiguration) {
        if (extensionConfiguration.isQuarkusCacheEnabled()) {
            context.configureRuntimeClasspathNormalization(
                    normalization -> normalization.addPropertiesNormalization(extensionConfiguration.getCurrentConfigFileName(), QUARKUS_IGNORED_PROPERTIES)
            );
        }
    }

    private void configureQuarkusExtraTestInputs(MojoMetadataProvider.Context context, QuarkusExtensionConfiguration extensionConfiguration) {
        if (isQuarkusExtraTestInputsExpected(context, extensionConfiguration)) {
            context.inputs(inputs -> addQuarkusDependencyChecksumsInput(inputs, extensionConfiguration));
            context.inputs(inputs -> addQuarkusDependenciesInputs(inputs, extensionConfiguration));
        }
    }

    private boolean isQuarkusExtraTestInputsExpected(MojoMetadataProvider.Context context, QuarkusExtensionConfiguration extensionConfiguration) {
        if (extensionConfiguration.isQuarkusCacheEnabled()) {
            Xpp3Dom properties = context.getMojoExecution().getConfiguration().getChild("properties");
            if (properties != null) {
                Xpp3Dom addQuarkusInputs = properties.getChild(TEST_GOAL_KEY_ADD_QUARKUS_INPUTS);
                if (addQuarkusInputs != null) {
                    return Boolean.parseBoolean(addQuarkusInputs.getValue());
                }
            }
        }
        return false;
    }

    private void configureQuarkusBuildGoal(MojoMetadataProvider.Context context, QuarkusExtensionConfiguration extensionConfiguration) {
        if ("build".equals(context.getMojoExecution().getGoal())) {
            if (extensionConfiguration.isQuarkusCacheEnabled()) {
                LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Quarkus caching is enabled"));

                // Load Quarkus properties for current build
                String baseDir = context.getProject().getBasedir().getAbsolutePath();
                Properties quarkusCurrentProperties = QuarkusExtensionUtil.loadProperties(context.getProject().getBasedir().getAbsolutePath(), extensionConfiguration.getCurrentConfigFileName());

                // Check required configuration
                if (isQuarkusBuildCacheable(baseDir, extensionConfiguration, quarkusCurrentProperties)) {
                    LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus build goal marked as cacheable"));
                    configureInputs(context, extensionConfiguration, quarkusCurrentProperties);
                    configureOutputs(context);
                } else {
                    LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus build goal marked as not cacheable"));
                }
            } else {
                LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Quarkus caching is disabled"));
            }
        }
    }

    private boolean isQuarkusBuildCacheable(String baseDir, QuarkusExtensionConfiguration extensionConfiguration, Properties quarkusCurrentProperties) {
        return isJarPackagingTypeSupported(quarkusCurrentProperties)
                && isNotNativeOrInContainerNativeBuild(quarkusCurrentProperties)
                && isQuarkusPropertiesUnchanged(baseDir, extensionConfiguration, quarkusCurrentProperties);
    }

    private boolean isQuarkusPropertiesUnchanged(String baseDir, QuarkusExtensionConfiguration extensionConfiguration, Properties quarkusCurrentProperties) {
        // Load Quarkus properties for previous build
        Properties quarkusPreviousProperties = QuarkusExtensionUtil.loadProperties(baseDir, extensionConfiguration.getDumpConfigFileName());
        if (quarkusPreviousProperties.size() == 0) {
            LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Quarkus previous configuration not found"));
            return false;
        }
        if (quarkusCurrentProperties.size() == 0) {
            LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Quarkus current configuration not found"));
            return false;
        }

        Set<Map.Entry<Object, Object>> quarkusPropertiesCopy = new HashSet<>(quarkusPreviousProperties.entrySet());

        // Remove properties identical between current and previous build
        quarkusPropertiesCopy.removeAll(quarkusCurrentProperties.entrySet());

        // Remove properties which should be ignored
        quarkusPropertiesCopy.removeIf(e -> QUARKUS_IGNORED_PROPERTIES.contains(e.getKey().toString()));

        if (quarkusPropertiesCopy.size() > 0) {
            LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Quarkus properties have changed [" + quarkusPropertiesCopy.stream().map(e -> e.getKey().toString()).collect(Collectors.joining(", ")) + "]"));
        } else {
            return true;
        }

        return false;
    }

    private boolean isNativeBuild(Properties quarkusCurrentProperties) {
        return Boolean.parseBoolean(quarkusCurrentProperties.getProperty(QUARKUS_CONFIG_KEY_NATIVE)) || PACKAGE_NATIVE.equals(quarkusCurrentProperties.getProperty(QUARKUS_CONFIG_KEY_DEPRECATED_PACKAGE_TYPE));
    }

    private boolean isNotNativeOrInContainerNativeBuild(Properties quarkusCurrentProperties) {
        if (isNativeBuild(quarkusCurrentProperties)) {
            String builderImage = quarkusCurrentProperties.getProperty(QUARKUS_CONFIG_KEY_NATIVE_BUILDER_IMAGE, "");
            if (builderImage.isEmpty()) {
                LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus build is not using a fixed image"));
                return false;
            }

            if (QUARKUS_CONFIG_KEY_NATIVE_CONTAINER_BUILD.stream().noneMatch(key -> Boolean.parseBoolean(quarkusCurrentProperties.getProperty(key)))) {
                LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus build strategy is not in-container"));
                return false;
            }
        }

        return true;
    }

    private boolean isJarPackagingTypeSupported(Properties quarkusCurrentProperties) {
        if (!isNativeBuild(quarkusCurrentProperties)) {
            String packageType = getJarPackageType(quarkusCurrentProperties);
            if (packageType == null || !QUARKUS_CACHEABLE_JAR_TYPES.contains(packageType)) {
                LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus package type " + packageType + " is not cacheable"));
                return false;
            }
        }

        return true;
    }

    private String getJarPackageType(Properties quarkusCurrentProperties) {
        String jarPackageType = quarkusCurrentProperties.getProperty(QUARKUS_CONFIG_KEY_JAR_TYPE);
        if (jarPackageType == null) {
            jarPackageType = quarkusCurrentProperties.getProperty(QUARKUS_CONFIG_KEY_DEPRECATED_PACKAGE_TYPE);
        }
        return jarPackageType;
    }

    private void configureInputs(MojoMetadataProvider.Context context, QuarkusExtensionConfiguration extensionConfiguration, Properties quarkusCurrentProperties) {
        context.inputs(inputs -> {
            addOsInputs(inputs);
            addCompilerInputs(inputs);
            addClasspathInput(context, inputs);
            addMojoInputs(inputs);
            addQuarkusPropertiesInput(inputs, extensionConfiguration);
            addQuarkusConfigurationFilesInputs(inputs, quarkusCurrentProperties);
            addQuarkusDependenciesInputs(inputs, extensionConfiguration);
            addQuarkusDependencyChecksumsInput(inputs, extensionConfiguration);
        });
    }

    private void addOsInputs(MojoMetadataProvider.Context.Inputs inputs) {
        inputs.property("osName", System.getProperty("os.name"))
                .property("osVersion", System.getProperty("os.version"))
                .property("osArch", System.getProperty("os.arch"));
    }

    private void addCompilerInputs(MojoMetadataProvider.Context.Inputs inputs) {
        inputs.property("javaVersion", System.getProperty("java.version"));
    }

    private void addClasspathInput(MojoMetadataProvider.Context context, MojoMetadataProvider.Context.Inputs inputs) {
        try {
            List<String> compileClasspathElements = context.getProject().getCompileClasspathElements();
            inputs.fileSet("quarkusCompileClasspath", compileClasspathElements, fileSet -> fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.CLASSPATH));
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException("Classpath can't be resolved");
        }
    }

    private void addMojoInputs(MojoMetadataProvider.Context.Inputs inputs) {
        inputs
                .fileSet("generatedSourcesDirectory", fileSet -> {
                })
                .properties("appArtifact", "closeBootstrappedApp", "finalName", "ignoredEntries", "manifestEntries", "manifestSections", "skip", "skipOriginalJarRename", "systemProperties", "properties")
                .ignore("project", "buildDir", "mojoExecution", "session", "repoSession", "repos", "pluginRepos", "attachRunnerAsMainArtifact", "bootstrapId", "buildDirectory");
    }

    private void addQuarkusPropertiesInput(MojoMetadataProvider.Context.Inputs inputs, QuarkusExtensionConfiguration extensionConfiguration) {
        inputs.fileSet("quarkusConfigCheck", new File(extensionConfiguration.getCurrentConfigFileName()), fileSet -> fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH));
        inputs.fileSet("generatedSourcesDirectory", fileSet -> {
        });
    }

    private void addQuarkusConfigurationFilesInputs(MojoMetadataProvider.Context.Inputs inputs, Properties quarkusCurrentProperties) {
        for (String quarkusFilePropertyKey : QUARKUS_KEYS_AS_FILE_INPUTS) {
            String quarkusFilePropertyValue = quarkusCurrentProperties.getProperty(quarkusFilePropertyKey);
            if (QuarkusExtensionUtil.isNotEmpty(quarkusFilePropertyValue)) {
                inputs.fileSet(quarkusFilePropertyKey, new File(quarkusFilePropertyValue), fileSet -> fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH));
            }
        }
    }

    /**
     * This method is deprecated and kept for compatibility reasons @see {@link #addQuarkusDependenciesInputs} for replacement
     */
    @Deprecated
    private void addQuarkusDependencyChecksumsInput(MojoMetadataProvider.Context.Inputs inputs, QuarkusExtensionConfiguration extensionConfiguration) {
        inputs.fileSet("quarkusDependencyChecksums", new File(extensionConfiguration.getCurrentDependencyChecksumsFileName()), fileSet -> fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH));
    }

    private void addQuarkusDependenciesInputs(MojoMetadataProvider.Context.Inputs inputs, QuarkusExtensionConfiguration extensionConfiguration) {
        File quarkusDependencyFile = new File(extensionConfiguration.getCurrentDependencyFileName());
        if (quarkusDependencyFile.exists()) {
            try {
                List<String> quarkusDependencies = Files.readAllLines(quarkusDependencyFile.toPath(), Charset.defaultCharset());
                inputs.fileSet("quarkusDependencies", quarkusDependencies, fileSet -> fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.CLASSPATH));
            } catch (IOException e) {
                LOGGER.error(QuarkusExtensionUtil.getLogMessage("Error while loading " + quarkusDependencyFile), e);
            }
        } else {
            LOGGER.debug(QuarkusExtensionUtil.getLogMessage(quarkusDependencyFile + " not found"));
        }
    }

    private void configureOutputs(MojoMetadataProvider.Context context) {
        context.outputs(outputs -> {
            String quarkusExeFileName = "target/" + context.getProject().getBuild().getFinalName() + "-runner";
            String quarkusJarFileName = "target/" + context.getProject().getBuild().getFinalName() + ".jar";
            String quarkusUberJarFileName = "target/" + context.getProject().getBuild().getFinalName() + "-runner.jar";

            outputs.cacheable("this plugin has CPU-bound goals with well-defined inputs and outputs");
            outputs.file("quarkusExe", quarkusExeFileName);
            outputs.file("quarkusJar", quarkusJarFileName);
            outputs.file("quarkusUberJar", quarkusUberJarFileName);
        });
    }

}
