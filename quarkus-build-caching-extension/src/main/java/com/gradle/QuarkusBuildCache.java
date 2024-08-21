package com.gradle;

import com.gradle.develocity.agent.maven.api.cache.BuildCacheApi;
import com.gradle.develocity.agent.maven.api.cache.MojoMetadataProvider;
import com.gradle.develocity.agent.maven.api.cache.NormalizationProvider;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
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

    private static final String TARGET_DIR = "target/";

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
    // Quarkus' cacheable package types
    private static final List<String> QUARKUS_CACHEABLE_JAR_TYPES = Arrays.asList("jar", "legacy-jar", "uber-jar");

    // Quarkus' properties which are considered as file inputs
    private static final List<String> QUARKUS_KEYS_AS_FILE_INPUTS = Arrays.asList("quarkus.docker.dockerfile-native-path", "quarkus.docker.dockerfile-jvm-path", "quarkus.openshift.jvm-dockerfile", "quarkus.openshift.native-dockerfile");

    // Quarkus' properties which should be ignored (the JDK / GraalVM version are extra inputs)
    private static final List<String> QUARKUS_IGNORED_PROPERTIES = Arrays.asList(QUARKUS_CONFIG_KEY_GRAALVM_HOME, QUARKUS_CONFIG_KEY_JAVA_HOME);

    // Quarkus artifact descriptor
    private static final String QUARKUS_ARTIFACT_PROPERTIES_FILE_NAME = "quarkus-artifact.properties";

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
                TestConfiguration testConfiguration = new TestConfiguration(context, extensionConfiguration);
                configureQuarkusExtraTestInputs(context, extensionConfiguration, testConfiguration);
            });
            context.withPlugin("maven-failsafe-plugin", () -> {
                TestConfiguration testConfiguration = new TestConfiguration(context, extensionConfiguration);
                configureQuarkusExtraTestInputs(context, extensionConfiguration, testConfiguration);
                configureQuarkusExtraIntegrationTestInputs(context, testConfiguration);
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

    private void configureQuarkusExtraTestInputs(MojoMetadataProvider.Context context, QuarkusExtensionConfiguration extensionConfiguration, TestConfiguration testConfiguration) {
        LOGGER.debug(QuarkusExtensionUtil.getLogMessage(testConfiguration.toString()));
        if (testConfiguration.isAddQuarkusInputs()) {
            LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Adding Quarkus extra test inputs"));
            context.inputs(inputs -> addQuarkusDependencyChecksumsInput(inputs, extensionConfiguration));
            context.inputs(inputs -> addQuarkusDependenciesInputs(inputs, extensionConfiguration));
        }
        if (testConfiguration.isAddQuarkusPackageInputs()) {
            LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Adding Quarkus extra test package inputs"));
            context.inputs(inputs -> addQuarkusJarInput(inputs, testConfiguration));
        }
    }

    private void configureQuarkusExtraIntegrationTestInputs(MojoMetadataProvider.Context context, TestConfiguration testConfiguration) {
        if (testConfiguration.isAddQuarkusInputs()) {
            context.inputs(this::addQuarkusArtifactPropertiesInput);
        }
        if (testConfiguration.isAddQuarkusPackageInputs()) {
            context.inputs(inputs -> addQuarkusExeInput(inputs, testConfiguration));
        }
    }

    private void configureQuarkusBuildGoal(MojoMetadataProvider.Context context, QuarkusExtensionConfiguration extensionConfiguration) {
        if ("build".equals(context.getMojoExecution().getGoal())) {
            LOGGER.debug(QuarkusExtensionUtil.getLogMessage(extensionConfiguration.toString()));
            if (extensionConfiguration.isQuarkusCacheEnabled()) {
                LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Quarkus caching is enabled"));

                // Load Quarkus properties from previous build
                String baseDir = context.getProject().getBasedir().getAbsolutePath();
                Properties quarkusPreviousProperties = QuarkusExtensionUtil.loadProperties(baseDir, extensionConfiguration.getDumpConfigFileName());

                // Load Quarkus properties from current build
                Properties quarkusCurrentProperties = QuarkusExtensionUtil.loadProperties(baseDir, extensionConfiguration.getCurrentConfigFileName());

                // Check required configuration
                if (isQuarkusBuildCacheable(quarkusPreviousProperties, quarkusCurrentProperties, extensionConfiguration.isNativeBuildInContainerRequired())) {
                    LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus build goal marked as cacheable"));
                    configureInputs(context, extensionConfiguration, quarkusCurrentProperties);
                    configureOutputs(context, extensionConfiguration.getExtraOutputDirs(), extensionConfiguration.getExtraOutputFiles());
                } else {
                    LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus build goal marked as not cacheable"));
                }
            } else {
                LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Quarkus caching is disabled"));
            }
        }
    }

    private boolean isQuarkusBuildCacheable(Properties quarkusPreviousProperties, Properties quarkusCurrentProperties, boolean isNativeBuildInContainerRequired) {
        return isQuarkusDumpConfigFilePresent(quarkusPreviousProperties, quarkusCurrentProperties)
                && isJarPackagingTypeSupported(quarkusCurrentProperties)
                && isNotNativeOrInContainerNativeBuild(quarkusCurrentProperties, isNativeBuildInContainerRequired)
                && isQuarkusPropertiesUnchanged(quarkusPreviousProperties, quarkusCurrentProperties);
    }

    private boolean isQuarkusDumpConfigFilePresent(Properties quarkusPreviousProperties, Properties quarkusCurrentProperties) {
        if (quarkusPreviousProperties.isEmpty()) {
            LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus previous configuration not found"));
            return false;
        }
        if (quarkusCurrentProperties.isEmpty()) {
            LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus current configuration not found"));
            return false;
        }

        return true;
    }

    private boolean isQuarkusPropertiesUnchanged(Properties quarkusPreviousProperties, Properties quarkusCurrentProperties) {
        Set<Map.Entry<Object, Object>> quarkusPropertiesCopy = new HashSet<>(quarkusPreviousProperties.entrySet());

        // Remove properties identical between current and previous build
        quarkusPropertiesCopy.removeAll(quarkusCurrentProperties.entrySet());

        // Remove properties which should be ignored
        quarkusPropertiesCopy.removeIf(e -> QUARKUS_IGNORED_PROPERTIES.contains(e.getKey().toString()));

        if (!quarkusPropertiesCopy.isEmpty()) {
            LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus properties have changed"));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(QuarkusExtensionUtil.getLogMessage("[" + quarkusPropertiesCopy.stream().map(e -> e.getKey().toString()).collect(Collectors.joining(", ")) + "]"));
            }
        } else {
            return true;
        }

        return false;
    }

    private boolean isNativeBuild(Properties quarkusCurrentProperties) {
        return Boolean.parseBoolean(quarkusCurrentProperties.getProperty(QUARKUS_CONFIG_KEY_NATIVE)) || PACKAGE_NATIVE.equals(quarkusCurrentProperties.getProperty(QUARKUS_CONFIG_KEY_DEPRECATED_PACKAGE_TYPE));
    }

    private boolean isNotNativeOrInContainerNativeBuild(Properties quarkusCurrentProperties, boolean isNativeBuildInContainerRequired) {
        if (isNativeBuild(quarkusCurrentProperties)) {
            if (isNativeBuildInContainerRequired) {
                return isInContainerWithFixedImageBuild(quarkusCurrentProperties, true);
            } else {
                LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus in-container build strategy is not required"));
            }
        } else {
            LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Quarkus build is not native"));
        }

        return true;
    }

    private boolean isInContainerWithFixedImageBuild(Properties quarkusCurrentProperties, boolean isLoggingRequired) {
        String builderImage = quarkusCurrentProperties.getProperty(QUARKUS_CONFIG_KEY_NATIVE_BUILDER_IMAGE, "");
        if (builderImage.isEmpty()) {
            if(isLoggingRequired) {
                LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus build is not using a fixed image"));
            }
            return false;
        }

        if (QUARKUS_CONFIG_KEY_NATIVE_CONTAINER_BUILD.stream().noneMatch(key -> Boolean.parseBoolean(quarkusCurrentProperties.getProperty(key)))) {
            if(isLoggingRequired) {
                LOGGER.info(QuarkusExtensionUtil.getLogMessage("Quarkus build strategy is not in-container"));
            }
            return false;
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
            if (!isInContainerWithFixedImageBuild(quarkusCurrentProperties, false)) {
                // do not add OS inputs when using the in-container build strategy
                addOsInputs(inputs);
            }
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
                .properties("appArtifact", "closeBootstrappedApp", "finalName", "ignoredEntries", "manifestEntries", "manifestSections", "skip", "skipOriginalJarRename", "systemProperties", "properties", "attachSboms")
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

    private void addQuarkusArtifactPropertiesInput(MojoMetadataProvider.Context.Inputs inputs) {
        inputs.fileSet("quarkusArtifactProperties", new File(TARGET_DIR + QUARKUS_ARTIFACT_PROPERTIES_FILE_NAME), fileSet -> fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH));
    }

    private void addQuarkusJarInput(MojoMetadataProvider.Context.Inputs inputs, TestConfiguration testConfiguration) {
        inputs.fileSet("quarkusJarFile", new File(TARGET_DIR), fileSet -> fileSet.include(testConfiguration.getQuarkusJarFilePattern()).normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.CLASSPATH));
    }

    private void addQuarkusExeInput(MojoMetadataProvider.Context.Inputs inputs, TestConfiguration testConfiguration) {
        inputs.fileSet("quarkusExeFile", new File(TARGET_DIR), fileSet -> fileSet.include(testConfiguration.getQuarkusExeFilePattern()).normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH));
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

    private void configureOutputs(MojoMetadataProvider.Context context, List<String> extraOutputDirs, List<String> extraOutputFiles) {
        context.outputs(outputs -> {
            String quarkusExeFileName = TARGET_DIR + context.getProject().getBuild().getFinalName() + "-runner";
            String quarkusJarFileName = TARGET_DIR + context.getProject().getBuild().getFinalName() + ".jar";
            String quarkusUberJarFileName = TARGET_DIR + context.getProject().getBuild().getFinalName() + "-runner.jar";
            String quarkusArtifactProperties = TARGET_DIR + QUARKUS_ARTIFACT_PROPERTIES_FILE_NAME;

            outputs.cacheable("this plugin has CPU-bound goals with well-defined inputs and outputs");
            outputs.file("quarkusExe", quarkusExeFileName);
            outputs.file("quarkusJar", quarkusJarFileName);
            outputs.file("quarkusUberJar", quarkusUberJarFileName);
            outputs.file("quarkusArtifactProperties", quarkusArtifactProperties);

            extraOutputDirs.forEach(extraOutput -> {
                if(!extraOutput.isEmpty()) {
                    LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Adding extra output dir " + extraOutput));
                    outputs.directory(extraOutput, TARGET_DIR + extraOutput);
                }
            });

            extraOutputFiles.forEach(extraOutput -> {
                if(!extraOutput.isEmpty()) {
                    LOGGER.debug(QuarkusExtensionUtil.getLogMessage("Adding extra output file " + extraOutput));
                    outputs.file(extraOutput, TARGET_DIR + extraOutput);
                }
            });
        });
    }

}
