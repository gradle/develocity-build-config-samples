Example configuration for making the `pmd` and `cpd` goals of the `maven-pmd-plugin` cacheable.

Note that there are some limitations in the caching behaviour, where incorrect cache hits or misses
could result in very particular change scenarios. See comments in the `pom.xml` file for more details.
