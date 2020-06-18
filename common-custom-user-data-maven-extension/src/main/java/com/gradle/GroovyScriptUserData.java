package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;

class GroovyScriptUserData {

    static void addToBuildScan(MavenSession session, BuildScanApi buildScan, Logger logger) throws MavenExecutionException {
        File scriptFile = getScriptFile(session);
        if (scriptFile.exists()) {
            logger.debug("Evaluating custom user data Groovy script: " + scriptFile);
            evaluateGroovyScript(session, buildScan, logger, scriptFile);
        } else {
            logger.debug("Skipping evaluation of custom user data Groovy script because it does not exist: " + scriptFile);
        }
    }

    private static File getScriptFile(MavenSession session) {
        File rootDir = session.getRequest().getMultiModuleProjectDirectory();
        return new File(rootDir, ".mvn/gradle-enterprise-custom-user-data.groovy");
    }

    private static void evaluateGroovyScript(MavenSession session, BuildScanApi buildScan, Logger logger, File scriptFile) throws MavenExecutionException {
        try {
            Binding binding = prepareBinding(session, buildScan, logger);
            new GroovyShell(GroovyScriptUserData.class.getClassLoader(), binding).evaluate(scriptFile);
        } catch (IOException e) {
            throw new MavenExecutionException("Failed to evaluate custom user data Groovy script: " + scriptFile, e);
        }
    }

    private static Binding prepareBinding(MavenSession session, BuildScanApi buildScan, Logger logger) {
        Binding binding = new Binding();
        binding.setVariable("project", session.getTopLevelProject());
        binding.setVariable("session", session);
        binding.setVariable("buildScan", buildScan);
        binding.setVariable("log", new DefaultLog(logger));
        return binding;
    }
}
