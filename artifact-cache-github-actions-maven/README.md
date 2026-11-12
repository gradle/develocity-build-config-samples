## Artifact Cache with GitHub Actions and Maven

This sample demonstrates how to set up the [Develocity Artifact Cache][artifact-cache] for a Maven build on GitHub Actions, using the [Develocity Artifact Cache GitHub Action][action].
The Artifact Cache restores the Maven local repository and Maven wrapper distribution before the build, and stores new content after it.

### Requirements

- Develocity 2026.3 or later, an Edge node at version 2.3.0 or later, and the Artifact Cache entitlement on the license
- Maven 3.6.2 or later, and the Develocity Maven extension 2.4.0 or later to keep dependencies the build no longer uses out of the cache
- A [workload identity rule][workload-identity] on Develocity that accepts the repository's GitHub OIDC tokens

### Workflow steps

Add these steps to a job, after `actions/checkout`, and grant the job the `id-token: write` permission, along with `contents: read` for the checkout:

```yaml
      - name: Set up JDK 21
        uses: actions/setup-java@v6
        with:
          java-version: '21'
          distribution: 'temurin' # no cache input: the Artifact Cache replaces it
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
      - name: Build with Maven
        run: ./mvnw verify -B
```

- The action restores in its own step, and stores in a post step at the end of the job, only when the job succeeds.
- `develocity-edge-url` targets the Edge directly, which workload identity requires.
  With the built-in Edge of Develocity, use the Develocity server URL and drop `develocity-oidc-audience`.
  Otherwise, set `develocity-oidc-audience` to the audience of the workload identity rule.
- `image-names` replaces the auto-generated image names, which consist of the workflow, job, ref and runner platform, but not the repository.
  Without it, repositories with the same workflow and job names, such as `CI` and `build`, read and write the same image on a shared Edge, and restore each other's content.
  The first name is the one stored and restored, the second one a restore fallback to the image of the pull request's base branch, or of the default branch.
- A build that moves the local repository, for example with `-Dmaven.repo.local`, needs `additional-cli-args: --maven-repository=<path>`.
- To authenticate with an access key instead, replace `develocity-auth` and `develocity-oidc-audience` with `develocity-access-key: ${{ secrets.DEVELOCITY_ACCESS_KEY }}`, holding the key in `<edge-host>=<key>` form.
- The action uses its credential only for the cache. Builds that publish Build Scans need their own, for example through the `DEVELOCITY_ACCESS_KEY` environment variable of the build step.

The cache activity shows in the job summary, and in the Build Scan through the `ARTIFACT_CACHE` tag and the restore metrics captured as custom values.

### Verification

The [verification workflow](../.github/workflows/artifact-cache-github-actions-maven-verification.yml) runs these steps against the Develocity Solutions instance, and fails when they differ from the snippet above in anything but the placeholder URLs and the pinned action versions.

[artifact-cache]: https://docs.develocity.ai/artifact-cache/current/
[action]: https://github.com/gradle/develocity-artifact-cache-github-action
[workload-identity]: https://develocity.ai/help/workload-identity
