package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

import java.io.File;

final class GroovyScriptUserData {

    static void evaluate(MavenSession session, GradleEnterpriseApi gradleEnterprise, Logger logger) throws MavenExecutionException {
        File groovyScript = getGroovyScript(session);
        if (groovyScript.exists()) {
            logger.debug("Evaluating custom user data Groovy script: " + groovyScript);
            evaluateGroovyScript(session, gradleEnterprise, logger, groovyScript);
        } else {
            logger.debug("Skipping evaluation of custom user data Groovy script because it does not exist: " + groovyScript);
        }
    }

    private static File getGroovyScript(MavenSession session) {
        File rootDir = session.getRequest().getMultiModuleProjectDirectory();
        return new File(rootDir, ".mvn/gradle-enterprise-custom-user-data.groovy");
    }

    private static void evaluateGroovyScript(MavenSession session, GradleEnterpriseApi gradleEnterprise, Logger logger, File groovyScript) throws MavenExecutionException {
        try {
            Binding binding = prepareBinding(session, gradleEnterprise, logger);
            new GroovyShell(GroovyScriptUserData.class.getClassLoader(), binding).evaluate(groovyScript);
        } catch (Exception e) {
            throw new MavenExecutionException("Failed to evaluate custom user data Groovy script: " + groovyScript, e);
        }
    }

    private static Binding prepareBinding(MavenSession session, GradleEnterpriseApi gradleEnterprise, Logger logger) {
        Binding binding = new Binding();
        binding.setVariable("project", session.getTopLevelProject());
        binding.setVariable("session", session);
        binding.setVariable("gradleEnterprise", gradleEnterprise);
        binding.setVariable("buildScan", gradleEnterprise.getBuildScan());
        binding.setVariable("buildCache", gradleEnterprise.getBuildCache());
        binding.setVariable("log", new MavenLogger(logger));
        return binding;
    }

    private GroovyScriptUserData() {
    }

    /**
     * Since there's no consistent implementation type across different versions of Maven,
     * we use an internal implementation of Maven {@link Log} that wraps the Plexus {@link Logger}.
     */
    private static final class MavenLogger implements Log {
        private final Logger plexusLogger;

        private MavenLogger(Logger plexusLogger) {
            this.plexusLogger = plexusLogger;
        }

        public void debug(CharSequence content) {
            plexusLogger.debug(toString(content));
        }

        private String toString(CharSequence content) {
            return content == null ? "" : content.toString();
        }

        public void debug(CharSequence content, Throwable error) {
            plexusLogger.debug(toString(content), error);
        }

        public void debug(Throwable error) {
            plexusLogger.debug("", error);
        }

        public void info(CharSequence content) {
            plexusLogger.info(toString(content));
        }

        public void info(CharSequence content, Throwable error) {
            plexusLogger.info(toString(content), error);
        }

        public void info(Throwable error) {
            plexusLogger.info("", error);
        }

        public void warn(CharSequence content) {
            plexusLogger.warn(toString(content));
        }

        public void warn(CharSequence content, Throwable error) {
            plexusLogger.warn(toString(content), error);
        }

        public void warn(Throwable error) {
            plexusLogger.warn("", error);
        }

        public void error(CharSequence content) {
            plexusLogger.error(toString(content));
        }

        public void error(CharSequence content, Throwable error) {
            plexusLogger.error(toString(content), error);
        }

        public void error(Throwable error) {
            plexusLogger.error("", error);
        }

        public boolean isDebugEnabled() {
            return plexusLogger.isDebugEnabled();
        }

        public boolean isInfoEnabled() {
            return plexusLogger.isInfoEnabled();
        }

        public boolean isWarnEnabled() {
            return plexusLogger.isWarnEnabled();
        }

        public boolean isErrorEnabled() {
            return plexusLogger.isErrorEnabled();
        }
    }

}
