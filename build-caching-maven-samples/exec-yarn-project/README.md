The `exec-maven-plugin` can be used to execute arbitrary programs, meaning that the inputs and output of the execution are also arbitrary.
An `exec` goal can be made cacheable by declaring _all_ inputs and outputs for the execution.

In this example, the `exec-maven-plugin` is used to invoke the [`Yarn Package Manager`](https://yarnpkg.com/).
Both `yarn install` and `yarn run build` are invoked, and the Gradle Enterprise POM DSL is used to make `yarn run build` outputs cacheable.
The `build` action is defined in `package.json` to simply copy all files from `src` into `target/out`. As such, `src` is an input to the action, and `target/out` is an output.
