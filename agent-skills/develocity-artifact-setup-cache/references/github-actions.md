# GitHub Actions

Use the official Action, `gradle/develocity-artifact-cache-github-action`. It downloads and verifies
the CLI, restores in its own step, and stores in a post step when the job succeeded. Read its
`action.yml` and README at the ref you pin for the current inputs.

Take the steps from the samples, which are verified against a real Edge:

```
https://raw.githubusercontent.com/gradle/develocity-build-config-samples/main/artifact-cache-github-actions-gradle/README.md
https://raw.githubusercontent.com/gradle/develocity-build-config-samples/main/artifact-cache-github-actions-maven/README.md
```

Their *Workflow steps* snippet is the shape to adapt: step order, inputs, comments. Image names
come from `image-names.md`, which may differ from the sample's. If the samples cannot be fetched, this
file holds what the steps must do.

Use the manual flow (`generic-ci.md`) instead only where the Action cannot run: an organization
policy that does not allow it (the first run fails with "not allowed"), or a runner it does not
support. It supports GitHub-hosted Linux, macOS and Windows runners and self-hosted Linux x64.

## Version and pin

- **Ref**: the current major tag (`v1` at the time of writing). If the repository pins its other
  actions by commit SHA, pin this one the same way, with the release in a comment:
  `uses: gradle/develocity-artifact-cache-github-action@<sha> # v<release>`. Resolve both with
  `git ls-remote https://github.com/gradle/develocity-artifact-cache-github-action 'refs/tags/v*'`:
  the commit the major tag points at, and the release tag on the same commit (not a `-rc-` one).
- **CLI version that will run**: the `cli-version` input if set, else `version` in `manifest.json`
  at that ref:
  `curl -s https://raw.githubusercontent.com/gradle/develocity-artifact-cache-github-action/<ref>/manifest.json`.
  It decides the image naming (`image-names.md`). Leave `cli-version` unset: the Action's default is
  tested and its checksum shipped; a version the Action does not know also needs `cli-sha256`.

## Placement

An illustration of the order, not a recipe to copy:

```yaml
    steps:
      - uses: actions/checkout@<ref>
      - uses: actions/setup-java@<ref>        # the build's JDK, as before, without its cache: input
      - uses: gradle/actions/setup-gradle@<ref>
        with:
          cache-provider: external
      - uses: gradle/develocity-artifact-cache-github-action@<ref>
        with:
          develocity-url: https://develocity.example.com
          develocity-access-key: ${{ secrets.DEVELOCITY_ACCESS_KEY }}
      - run: ./gradlew build
```

- **After the checkout**: the Setup Cache restores into it.
- **After `setup-gradle`**: it sets `GRADLE_USER_HOME`, which the CLI reads.
- **After `docker/setup-buildx-action`** when `enable-docker-builders` is on: post steps run in
  reverse, so the store then runs while the builder still exists.
- **Before every step that downloads** what the CLI caches: `npm ci`, `pip install`,
  `playwright install`, `docker pull`, the build.

One Action step per job. A job defined in a reusable workflow is wired there, and its caller must
pass the secret (`secrets: inherit` or an explicit mapping).

## Endpoint and credentials

Use the credential the workflow already uses for Develocity (step 1 of `SKILL.md`).

**Workload identity.** The workflow already exchanges a GitHub OIDC token for Develocity: a step
that requests one from `ACTIONS_ID_TOKEN_REQUEST_URL` with an `audience`, or an action that does. The
Action requests its own token, so add no secret:

| Edge (step 3) | Inputs |
| --- | --- |
| Dedicated Edge | `develocity-edge-url: <Edge URL>`, `develocity-auth: workload-identity`, `develocity-oidc-audience:` the `audience` the existing step requests |
| Built-in Edge | `develocity-edge-url: <server URL>`, `develocity-auth: workload-identity`, no `develocity-oidc-audience` |

Workload identity needs `develocity-edge-url`: discovery through `develocity-url` rejects its token.
The job needs `id-token: write` in its `permissions`, which a job that already requests OIDC tokens
has. The rule must grant cache read and write; list it under *Before merging* if that is not
confirmed. Keep the existing token step: the Action's token serves only the cache, and the build
still needs its own for Build Scans.

**Access key.** `develocity-access-key` takes `<host>=<key>` entries only, and the Action uses the one
for the endpoint's host. It reads no other key source, so this input is the whole wiring: pass the
secret the workflows already pass as `DEVELOCITY_ACCESS_KEY`, and add none. The Action exchanges the
key for a short-lived token and exports nothing to other steps.

| Edge (step 3) | Inputs |
| --- | --- |
| Discovery | `develocity-url: <server URL>`, `develocity-access-key:` the existing secret |
| Built-in Edge | `develocity-edge-url: <server URL>`, the existing secret |
| Dedicated Edge | `develocity-edge-url: <Edge URL>`, the existing secret, which needs an entry for the Edge host |

A secret holding only `<server host>=<key>` does not cover a dedicated Edge on its own host. Do not
derive the entry in a step: anything a step writes to `$GITHUB_ENV` is visible to every later step,
which undoes the Action's isolation of the key. Put under *Before merging*: append
`;<edge host>=<the same key>` to the secret's value. Until then the Action warns and skips, and the
build runs as before.

## Conflicting caches

Remove these, and name each in the pull request:

- the `cache:` input of `actions/setup-java`, of `actions/setup-node` when it is `npm`, of
  `actions/setup-python` when it is `pip`
- `actions/cache` (and `actions/cache/restore`, `actions/cache/save`) steps whose paths are a home
  the CLI covers: `~/.gradle`, `~/.m2`, `~/.npm`, `~/.cache/pip`, `~/.sonar`, `~/.cache/ms-playwright`,
  `~/.cache/Cypress`, or a moved home of those tools
- `setup-gradle`'s own caching: `cache-provider: external` on `gradle/actions` 6.4.0 or later, else
  `cache-disabled: true`. Read the version from the `uses:` ref. A commit SHA, or a major tag such as
  `@v6` or `# v6`, does not say which release it is: resolve it with
  `git ls-remote https://github.com/gradle/actions 'refs/tags/v*'` and take the release tag on that
  commit (`refs/tags/v6.4.1^{}` for the commit `@v6` points at, say).

`node_modules`, yarn and pnpm stores, build outputs and anything else stay cached as before.

## Other inputs

- `image-names`: see `image-names.md`.
- `additional-cli-args`, one argument per line: `--disable-artifact-cache`, `--disable-setup-cache`,
  `--no-autodetect=DOCKER`, `--docker-exclude=<pattern>`, a home override. The Action fails the step
  on a flag it manages (`--dv-server`, `--dv-edge`, `--image-name`, `--cache-metrics-file`,
  `--allow-untrusted-server`).
- `enable-docker-builders: true`: also caches the base images a buildx `docker-container` builder
  pulls.
- `cache-read-only`: leave it off. Each branch and pull request stores to its own image, and only
  default-branch builds write the image others fall back to.
- `cli-version`: leave it unset in new wiring. In existing wiring, a pin below the version the
  Action would pick is a review finding (step 5): propose removing it, and check what the pinned
  version lacks in the Release History (the generated fallback name needs 1.9.0, Docker caching 1.9.0).

## Runners

- **GitHub-hosted**: the Action finds a Java 21 through the runner's `JAVA_HOME_21_*` variables; the
  build's own JDK setup stays as it is.
- **Self-hosted Linux x64**: needs Java 21 under `JAVA_HOME_21_X64`, which `actions/setup-java` sets.
  When the build needs another JDK, install both in one step and list the build's last, so it stays
  the default `JAVA_HOME`:
  ```yaml
          java-version: |
            21
            17
  ```
  Self-hosted runners also need a runner version that runs Node 24 actions; the Configuring GitHub
  Actions page gives the minimum.
- **Self-hosted macOS, Windows or ARM64**: not supported by the Action: use the manual flow.

## Forks

Pull requests from forks get neither secrets nor OIDC tokens, so the Action warns and skips both
phases. Nothing to add. Verification then moves to the default branch (`verification.md`).

## Results

The Action adds a table to the job summary: one row for the restore and one for the store, with the
outcome, detected tools, entries, size and duration. A read-only cache has no store row.
