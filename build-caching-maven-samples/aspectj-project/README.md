Example configuration for making the `aspectj:compile` and `aspectj:test-compile` goals of the `aspectj-maven-plugin` cacheable.

Note that the compiler plugin is disabled on the `impl` module which applies the AspectJ plugin, as the AspectJ plugin overrides the outputs of the compile goal and this is not supported with build caching.
