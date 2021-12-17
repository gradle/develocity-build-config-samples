Example configuration for making the `spotbugs` goal of the `spotbugs-maven-plugin` cacheable.

Note that there are some limitations in the caching behaviour, where incorrect cache hits or misses
could result in very particular change scenarios. See comments in the `pom.xml` file for more details.
