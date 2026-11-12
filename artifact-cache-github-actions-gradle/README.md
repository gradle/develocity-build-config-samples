## Artifact Cache with GitHub Actions and Gradle

This sample demonstrates how to set up the [Develocity Artifact Cache and Setup Cache][artifact-cache] for a Gradle build on GitHub Actions, using the [Develocity Artifact Cache GitHub Action][action] together with [`setup-gradle`][setup-gradle].
The Artifact Cache restores the dependencies in the Gradle User Home before the build, and stores new ones after it.
The Setup Cache does the same for build initialization data, such as the compiled build logic of `buildSrc` and included builds like [`build-logic`](build-logic).

### Requirements

- Develocity 2026.3 or later, an Edge node at version 2.3.0 or later, and the Artifact Cache and Setup Cache entitlements on the license
- Gradle 8.1 or later, and the Develocity Gradle plugin 4.4.1 or later for the Setup Cache to cache build logic
- A [workload identity rule][workload-identity] on Develocity that accepts the repository's GitHub OIDC tokens

### Workflow steps

Add these steps to a job, after `actions/checkout`, and grant the job the `id-token: write` permission, along with `contents: read` for the checkout:

```yaml
      - name: Set up JDK 21
        uses: actions/setup-java@v6
        with:
          java-version: '21'
          distribution: 'temurin' # no cache input: the Artifact Cache replaces it
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6
        with:
          cache-provider: external # the Artifact Cache action caches the Gradle User Home instead
      - name: Set up Develocity Artifact Cache
        uses: gradle/develocity-artifact-cache-github-action@v1
        with:
          develocity-edge-url: https://edge.example.com
          develocity-auth: workload-identity
          develocity-oidc-audience: https://develocity.example.com
          # Auto-generated image names omit the repository, so they collide across repositories on the same Edge
          image-names: |
            ${{ github.repository }}~${{ github.workflow }}~${{ github.job }}~${{ github.ref }}~${{ runner.os }}~${{ runner.arch }}
            ${{ github.repository }}~${{ github.workflow }}~${{ github.job }}~refs/heads/${{ github.base_ref || github.event.repository.default_branch }}~${{ runner.os }}~${{ runner.arch }}
      - name: Build with Gradle
        run: ./gradlew build
```

- `setup-gradle` runs before the action, so that the action caches the Gradle User Home that `setup-gradle` configures.
- The action restores in its own step, and stores in a post step at the end of the job, only when the job succeeds.
  The Setup Cache needs no configuration of its own: the same restore and store cover it.
- `develocity-edge-url` targets the Edge directly, which workload identity requires.
  With the built-in Edge of Develocity, use the Develocity server URL and drop `develocity-oidc-audience`.
  Otherwise, set `develocity-oidc-audience` to the audience of the workload identity rule.
- `image-names` replaces the auto-generated image names, which consist of the workflow, job, ref and runner platform, but not the repository.
  Without it, repositories with the same workflow and job names, such as `CI` and `build`, read and write the same image on a shared Edge, and restore each other's content.
  The first name is the one stored and restored, the second one a restore fallback to the image of the pull request's base branch, or of the default branch.
- To authenticate with an access key instead, replace `develocity-auth` and `develocity-oidc-audience` with `develocity-access-key: ${{ secrets.DEVELOCITY_ACCESS_KEY }}`, holding the key in `<edge-host>=<key>` form.
- The action uses its credential only for the cache. Builds that publish Build Scans need their own, for example through the `develocity-access-key` input of `setup-gradle`.

The cache activity shows in the job summary, and in the Build Scan through the `ARTIFACT_CACHE` and `SETUP_CACHE` tags and the restore metrics captured as custom values.

### Verification

The [verification workflow](../.github/workflows/artifact-cache-github-actions-gradle-verification.yml) runs these steps against the Develocity Solutions instance, and fails when they differ from the snippet above in anything but the placeholder URLs and the pinned action versions.

[artifact-cache]: https://docs.develocity.ai/artifact-cache/current/
[action]: https://github.com/gradle/develocity-artifact-cache-github-action
[setup-gradle]: https://github.com/gradle/actions/tree/main/setup-gradle
[workload-identity]: https://develocity.ai/help/workload-identity
