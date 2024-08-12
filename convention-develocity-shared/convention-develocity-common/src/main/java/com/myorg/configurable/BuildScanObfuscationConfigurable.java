package com.myorg.configurable;

import java.net.InetAddress;
import java.util.List;
import java.util.function.Function;

public interface BuildScanObfuscationConfigurable {

    void username(Function<String, String> obfuscator);

    void hostname(Function<String, String> obfuscator);

    void ipAddresses(Function<List<InetAddress>, List<String>> obfuscator);

}
