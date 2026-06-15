Depending on the contracts, the `spring-cloud-contracts:generateTests` goal may produce irreproducible test cases, due to randomization when picking values from the contracts. This will cause downstream goals such as test compilation and execution to have different inputs, and break caching for these goals.

This app demonstrates how the `generateTests` goal can be made cacheable, resulting in more consistent outputs for a fixed set of inputs.
