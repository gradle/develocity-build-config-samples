
The `maven-enforcer-plugin` cannot be made cacheable in general since it can be extended with rules and we don't know their inputs.
Most rules are rather fast and not worth to made cacheable.

In order to find out which rules are slow, you should first separate each rule into its own goal and look at the resulting build scan.
Once that is done, you can group the fast rules into a single goal and look at the code of the slow rules in detail.

For example, at one of our prospective customers the `Enforce Bytecode Version` rule was identified as being slow.
It was separated into its own execution, using the `enforce-bytecode-version` id, and made cacheable via the POM DSL.

Note that each enforcer rule will require it's own cache configuration, in order to correctly define all of the rule inputs.
