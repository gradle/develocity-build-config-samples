# Maven: `.mvn/develocity.xml`, `.mvn/extensions.xml`

```xml
<backgroundBuildScanUpload>#{env['<CI_VAR>'] == null}</backgroundBuildScanUpload>
<storeEnabled>#{env['<CI_VAR>'] != null and env['DEVELOCITY_ACCESS_KEY'] != null and env['DEVELOCITY_ACCESS_KEY'] != ''}</storeEnabled>
```

With several CI providers, `env['<CI_VAR>'] != null` becomes `(env['A'] != null or env['B'] != null)`, and
`== null` becomes `(env['A'] == null and env['B'] == null)`.

The sample's `isTrue(...)` / `isFalse(...)` helpers read the *value* of `CI`; presence is what is wanted, so use the comparisons above.

## `clean` in CI

The extension only stores to the build cache when the `clean` lifecycle is part of the invocation;
otherwise it logs "This build will only read from the build cache". If the CI command runs `verify`
or `install` without `clean`, the store gate above never takes effect and CI never populates the
remote cache. So add `clean` to the Maven invocations in the CI pipeline that build (`./mvnw -B
verify` becomes `./mvnw -B clean verify`) and list each one changed in the pull request.

Skip an invocation that reuses the output of an earlier step in the same job, such as a `deploy` or
`site` run after a separate `package` step: `clean` would delete what it needs. Name it in the pull
request instead. This applies to Maven only; no other tool needs it.

## Merging

Merge into an existing `.mvn/extensions.xml` rather than overwriting it.
Add `.mvn/.develocity/` to `.gitignore`: this directory must not be checked in.

## Pull request

The description lists each CI invocation that gained `clean`, and each one skipped with the reason.
