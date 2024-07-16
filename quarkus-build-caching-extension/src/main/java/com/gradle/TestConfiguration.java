package com.gradle;

import com.gradle.develocity.agent.maven.api.cache.MojoMetadataProvider;
import org.codehaus.plexus.util.xml.Xpp3Dom;

class TestConfiguration {

    private static final String TEST_GOAL_KEY_ADD_QUARKUS_INPUTS = "addQuarkusInputs";
    private static final String TEST_GOAL_KEY_ADD_QUARKUS_PACKAGE_INPUTS = "addQuarkusPackageInputs";
    private static final String TEST_GOAL_KEY_QUARKUS_PACKAGE_PATTERN = "quarkusPackagePattern";
    private static final String TEST_GOAL_DEFAULT_QUARKUS_PACKAGE_JAR_PATTERN = "*.jar";
    private static final String TEST_GOAL_DEFAULT_QUARKUS_PACKAGE_EXE_PATTERN = "*-runner";

    private boolean addQuarkusInputs;
    private boolean addQuarkusPackageInputs;
    private String quarkusPackagePattern;

    TestConfiguration(MojoMetadataProvider.Context context, QuarkusExtensionConfiguration extensionConfiguration) {
        if (extensionConfiguration.isQuarkusCacheEnabled()) {
            Xpp3Dom properties = context.getMojoExecution().getConfiguration().getChild("properties");
            if (properties != null) {
                Xpp3Dom addQuarkusInputsProperty = properties.getChild(TEST_GOAL_KEY_ADD_QUARKUS_INPUTS);
                if (addQuarkusInputsProperty != null) {
                    addQuarkusInputs = Boolean.parseBoolean(addQuarkusInputsProperty.getValue());
                }
                Xpp3Dom addQuarkusPackageInputsProperty = properties.getChild(TEST_GOAL_KEY_ADD_QUARKUS_PACKAGE_INPUTS);
                if (addQuarkusPackageInputsProperty != null) {
                    addQuarkusPackageInputs = Boolean.parseBoolean(addQuarkusPackageInputsProperty.getValue());
                }
                Xpp3Dom quarkusPackagePatternProperty = properties.getChild(TEST_GOAL_KEY_QUARKUS_PACKAGE_PATTERN);
                if (quarkusPackagePatternProperty != null) {
                    quarkusPackagePattern = quarkusPackagePatternProperty.getValue();
                }
            }
        }
    }

    boolean isAddQuarkusInputs() {
        return addQuarkusInputs;
    }

    boolean isAddQuarkusPackageInputs() {
        return addQuarkusPackageInputs;
    }

    String getQuarkusJarFilePattern() {
        return quarkusPackagePattern != null ? quarkusPackagePattern : TEST_GOAL_DEFAULT_QUARKUS_PACKAGE_JAR_PATTERN;
    }

    String getQuarkusExeFilePattern() {
        return quarkusPackagePattern != null ? quarkusPackagePattern : TEST_GOAL_DEFAULT_QUARKUS_PACKAGE_EXE_PATTERN;
    }

    @Override
    public String toString() {
        return "TestConfiguration{" +
                "addQuarkusInputs=" + addQuarkusInputs +
                ", addQuarkusPackageInputs=" + addQuarkusPackageInputs +
                ", quarkusPackagePattern='" + quarkusPackagePattern + '\'' +
                '}';
    }
}
