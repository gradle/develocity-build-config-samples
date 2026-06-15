package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.scan.BuildScanDataObfuscation;

import java.net.InetAddress;
import java.util.List;
import java.util.function.Function;

final class MavenBuildScanObfuscationConfigurable implements BuildScanObfuscationConfigurable {

    private final BuildScanDataObfuscation buildScanObfuscation;

    public MavenBuildScanObfuscationConfigurable(BuildScanDataObfuscation buildScanObfuscation) {
        this.buildScanObfuscation = buildScanObfuscation;
    }

    @Override
    public void username(Function<String, String> obfuscator) {
        buildScanObfuscation.username(obfuscator);
    }

    @Override
    public void hostname(Function<String, String> obfuscator) {
        buildScanObfuscation.hostname(obfuscator);
    }

    @Override
    public void ipAddresses(Function<List<InetAddress>, List<String>> obfuscator) {
        buildScanObfuscation.ipAddresses(obfuscator);
    }

}
