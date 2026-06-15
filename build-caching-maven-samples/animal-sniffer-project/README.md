Example configuration for making the `check` goal of the `animal-sniffer-maven-plugin` cacheable.

Starting with Java 9, the animal sniffer plugin is usually no longer necessary. Instead, you should configure the `release` parameter of the `maven-compiler-plugin`. However, if you're still on Java 8, you can make the plugin cacheable using the POM DSL.

The bundled configuration tracks all main Java source files (compileSourceRoots), the class files (outputDirectory is actually the output directory of the maven-compiler-plugin and used as an input for animal sniffer), and the GAV of all configured signatures. If you're using your own artifacts for the signatures and use snapshots or version ranges this will not be sufficient and you shouldn't use the example config.
