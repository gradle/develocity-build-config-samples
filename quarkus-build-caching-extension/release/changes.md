### 1.8
- Add support for Quarkus `BuildMojo.attachSboms` parameter introduced in Quarkus 3.14.0

### 1.7
- Allow cross-OS cache hits when using the in-container build strategy

### 1.6
- Support Quarkus extra properties on test goals using Quarkus lower than 3.9.0

### 1.5
- Allow to configure extra outputs
- Allow to disable in-container strategy requirement for native builds
- Remove configuration of ignored properties in Quarkus configuration dump file

### 1.4
- Add configuration of ignored properties in Quarkus configuration dump file

### 1.3
- Add `quarkus-artifact.properties` as goal output 

### 1.2
- Fix cache disabled due to unrecognized Jar package type when deprecated key `quarkus.package.type` is used instead of `quarkus.package.jar.type`

### 1.1
- Add support for Quarkus 3.9 (`quarkus.package.type` is replaced by `quarkus.native.enabled` / `quarkus.package.jar.type`)

### 1.0
- Add compatibility with `develocity-maven-extension:1.21`

---

### 0.12
- First version