The `maven-enforcer-plugin` cannot be made cacheable in general since it can be extended with rules and we don't know their inputs.
Most rules are rather fast and not worth to made cacheable.

In order to find out which rules are slow, you should first separate each rule into its own goal and look at the resulting build scan.
Once that is done, you can group the fast rules into a single goal and look at the code of the slow rules in detail.

In this example, 4 different enforcer rules have been enabled:
- `Banned Repositories`: a fast rule that does not require caching
- `Ban Distribution Management`: a fast rule that does not require caching
- `Banned Dependencies`: a slower rule that has been made cacheable
- `Enforce Bytecode Version`: a slower rule that has been made cacheable

The first 2 "fast" rules are combined into a single Enforcer execution.
The other 2 rules are run in separate executions, allowing them to be made independently cacheable.

Note that each enforcer execution will require it's own cache configuration, in order to correctly define all of the rule inputs.

### Compatibility

This example is compatible with `maven-enforcer-plugin` version `3.2.1+`.
