# Bazel: `.bazelrc`

The sample covers the server and the scan, but it puts `--remote_cache` in the `develocity` config
that every build uses. Bazel treats an unreachable or unauthenticated remote cache as a build
failure (`Failed to query remote execution capabilities`, exit 34), and no flag turns that into a
warning: `--remote_local_fallback` and the circuit-breaker flags do not cover the capability query.
So for Bazel the remote cache is opt-in, which means **reading from it is not on for everyone**,
unlike the other tools. Move the sample's `--remote_cache` line into a `remote-cache` config and add:

```
# A failed Build Event Protocol upload fails the build unless Bazel is told not to wait for it.
# Without this, every build breaks until the access key exists.
common:develocity --bes_upload_mode=nowait_for_upload_complete

# The remote cache is only switched on where credentials exist: CI adds --config=remote-cache with
# the Authorization headers, a developer does the same in develocity.bazelrc. Bazel fails the build
# on an unreachable or unauthenticated cache, so it cannot be on by default.
common:remote-cache --remote_cache=grpcs://<server host>

# .bazelrc cannot evaluate expressions, so the CI write gate is a config: read-only by default, and
# CI adds --config=ci on top to write.
build:remote-cache --remote_upload_local_results=false
common:ci --remote_upload_local_results=true

# Where a developer keeps their --config=remote-cache and Authorization headers locally.
# Gitignored, never committed.
try-import %workspace%/develocity.bazelrc
```

`test` inherits every `build` line, so do not add `test` variants of `--config=develocity` or
`--remote_upload_local_results`: the first makes Bazel warn that the config was expanded twice.
Add `develocity.bazelrc` to `.gitignore`.

There is no CI variable to test here: CI is whatever passes `--config=ci`. With several CI
providers, each one's pipeline gets the flags from *CI wiring* below.

## Merging

Append to an existing `.bazelrc`; never replace it. Keep its own configs and `try-import` lines,
and add the sample's lines and the block above after them. An existing `--remote_cache`,
`--bes_backend` or `--bes_results_url` is covered by step 1: stop and ask.

## CI wiring (step 5)

The build command needs `--config=remote-cache --config=ci` and `--remote_cache_header` /
`--bes_header` set to `Authorization=Bearer $DEVELOCITY_ACCESS_KEY`, **only when the key is
non-empty**. Passed unconditionally, a fork's build or any build before the secret exists sends an
empty token to a cache it cannot authenticate against, and fails. Guard it in the shell that runs
Bazel, which may be a script the pipeline calls rather than the pipeline file:

```bash
BAZEL_DV_FLAGS=()
if [[ -n "${DEVELOCITY_ACCESS_KEY:-}" ]]; then
  BAZEL_DV_FLAGS=(--config=remote-cache --config=ci
    "--remote_cache_header=Authorization=Bearer ${DEVELOCITY_ACCESS_KEY}"
    "--bes_header=Authorization=Bearer ${DEVELOCITY_ACCESS_KEY}")
fi
bazel test "${BAZEL_DV_FLAGS[@]}" //...
```

Passing the key as a header on the command line is fine: the `Authorization` value is redacted in
Build Scans. A `--credential_helper` would keep it off the command line, but needs a helper
installed on every agent; use one only when the repository already does.
