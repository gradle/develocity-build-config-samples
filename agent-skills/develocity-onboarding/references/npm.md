# npm: `develocity.config.js`

Build Scans only. The agent has no build cache and no background-upload setting.

Use the provider's variable for the `CI`/`Local` tag: `process.env['<CI_VAR>'] !== undefined`, or
`['A', 'B'].some(v => process.env[v] !== undefined)` with several providers.
The sample's block of `GITHUB_*` custom values only applies when the provider *is* GitHub Actions; drop it otherwise rather than translating it.

Name the file `develocity.config.cjs` when `package.json` has `"type": "module"`: the agent only
loads CommonJS configuration, and a `.js` file in an ES module package is read as ESM.

Set the `package.json` search boundary to the configuration's own directory:

```js
packageJson: {
  searchBoundary: __dirname,
},
```

The agent reads the project name from `package.json`, but by default refuses any `package.json`
outside the user's home directory. Builds in a container (`/app` with home `/root`) or on an agent
whose workspace sits outside home then show the project as `N/A`, and the log says
`package.json is not within the search boundary`. `__dirname` keeps the boundary at the repository
rather than lifting it altogether.

## The dependency

Add `@gradle-tech/develocity-agent` to `devDependencies` with the package manager the lockfile
belongs to, and commit the updated lockfile, otherwise the frozen install on CI fails:

| Lockfile | Command |
| --- | --- |
| `package-lock.json` | `npm install --save-dev @gradle-tech/develocity-agent` |
| `pnpm-lock.yaml` | `pnpm add -D @gradle-tech/develocity-agent` |
| `yarn.lock` | `yarn add -D @gradle-tech/develocity-agent` |

Never run `npm install` in a pnpm or yarn repository: it writes a stray `package-lock.json` and
leaves the real lockfile stale. The `packageManager` field of `package.json` settles the version
(through Corepack) when it is set.

## Test results need a reporter

The agent captures scripts and dependencies on its own, but test
results only reach the Build Scan through the framework's Develocity reporter. Find the test
framework in `devDependencies` and the `test` scripts, and configure it:

| Framework | Minimum version | Where | Add |
| --- | --- | --- | --- |
| Jest | 29.6 | `reporters` in the Jest config (`jest.config.*` or the `jest` key of `package.json`) | `require.resolve('@gradle-tech/develocity-agent/jest-reporter')` |
| Mocha | 7.0.1 | `reporter` in `.mocharc.*` | `require.resolve('@gradle-tech/develocity-agent/mocha-reporter')` |
| Cypress | 13.17.0 | `reporter` in `cypress.config.*` | `require.resolve('@gradle-tech/develocity-agent/cypress-reporter')` |

- **Jest**: append to the existing `reporters` and keep the ones already there. A config with no
  `reporters` key uses Jest's default reporter, so write `['default', develocityReporter]`, not
  just the Develocity one, or the console output disappears. In an ES module config,
  `require.resolve` comes from `createRequire(import.meta.url)`.
- **Mocha and Cypress**: the Develocity reporter replaces the configured one. Keep the existing
  reporter through a multi-reporter (`cypress-multi-reporters` with `reporterEnabled`) when the
  project already depends on one. Otherwise say in the pull request that the console reporter is
  replaced. Mocha in parallel mode reports nothing.
- **Anything else** (Vitest, Playwright, node:test): no reporter exists. Say in the pull request
  that test results are not captured.

## Scripts that set `NODE_OPTIONS` drop the agent

A script such as
`cross-env NODE_OPTIONS="--max-old-space-size=8192" jest` replaces the preload the agent was
loaded through, so that process and everything under it, the test run in particular, goes
unrecorded. Grep the `scripts` of `package.json` for `NODE_OPTIONS=` and prepend the inherited
value: `cross-env NODE_OPTIONS="$NODE_OPTIONS --max-old-space-size=8192" jest`. List every
script changed in the pull request.

## CI wiring (step 5)

The agent loads through `NODE_OPTIONS=-r @gradle-tech/develocity-agent/preload`, which must be
scoped to the step that runs the build. Set at job level it is inherited by every step, including
the checkout, whose Node process starts before the agent is installed and dies on `Cannot find module`.

## Check (step 7)

Install with the lockfile's package manager, then:

1. `node -e "console.log(require('./develocity.config.js'))"` (or `.cjs`): the file parses.
2. For Jest, `npx jest --showConfig` must list the Develocity reporter.
3. Run the `test` script once with `NODE_OPTIONS=-r @gradle-tech/develocity-agent/preload`. Parsing
   the file never loads the agent; this run does, through the same scripts CI uses, and shows
   whether the reporter is wired and whether a script drops the preload. The agent's log should
   name the test framework; a credential or connection warning at the end is expected.
