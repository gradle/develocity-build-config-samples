# Develocity

API: https://docs.develocity.ai/2026.3/reference/develocity-api/openapi-spec/

## Call the API

Read data with `GET <server>/api/...` and `Authorization: Bearer <key>`.
Take the key the build already uses: the `DEVELOCITY_ACCESS_KEY` environment variable, else the keys file in `references/<tool>.md`.
Both hold `<host>=<key>` entries, separated by `;` in the variable: send only the key of this server's host.
Never print a key or write it to a file.

Make every API call this way, extracting the key in the same shell command, since variables do not carry over between commands:

- `SERVER`: the Develocity server URL (it can be collected in the console output of a build), `HOST`: its host name
- `KEYS_FILE`: the keys file path from `references/<tool>.md`
- `ENDPOINT`: the path to call, e.g. `/api/builds?maxBuilds=1`

```
KEY=$(printf '%s' "$DEVELOCITY_ACCESS_KEY" | tr ';' '\n' | sed -n "s/^$HOST=//p" | head -n 1)
[ -n "$KEY" ] || KEY=$(sed -n "s/^$HOST=//p" "$KEYS_FILE" | head -n 1)
printf 'Authorization: Bearer %s\n' "$KEY" | curl -s -H @- "$SERVER$ENDPOINT"
```

The header goes through stdin, so the key does not show in the command or the process list.

When the request has `with-localhost-develocity=<url>`, make every API call to `<url>` with a plain `curl -s "<url>$ENDPOINT"`, without a key.
When a build prints no Build Scan link, use its tag (e.g. `td-baseline`, `td-exp1`) as its Build Scan ID.
The builds cannot connect TD to that server: ignore the related errors, and tell whether agents ran the tests from the API only.

## Verify the API can be accessed

Call `/api/builds?maxBuilds=1`, adding `-o /dev/null -w '%{http_code}\n'` to `curl`.
Access is granted when the status code is 200. A 404 can also mean the key lacks the permission to access build data via the API.

## Inspect the TD agent configuration

Needs admin access: tell the user first. Call the API as shown above.

1. List the agent pools: `GET /api/test-distribution/agent-pools`.
2. Read the status of each: `GET /api/test-distribution/agent-pools/{poolId}/status`.
3. Check that a pool has the capabilities given in the requirements input and `connectedAgents > 0`. Tell the user if agents should be provisioned.

If access is rejected, skip the check and say so.
