# Develocity Build Validation Scripts post clone logic

Develocity Build Validation Scripts must run builds on a fresh clone of your repository.
It might be the case that your project requires additional setup after cloning, such as copying dependencies, setting up configuration files, or any shell command.
To run custom logic after cloning, you can tell Git to include files from a [template directory][1] when cloning.
This directory can contain any git hooks you need, including a `post-checkout` script.

Create a dir with a `hooks` directory and a `post-checkout` hook:

```shell
# Create a new directory for the git template
mkdir -p git-template-dir/hooks
echo "#!/usr/bin/env bash" > git-template-dir/hooks/post-checkout
chmod +x git-template-dir/hooks/post-checkout
```

The `post-checkout` file is a regular shell script, in which you can run any shell command.
Place your logic in that file.
For example:

```shell
echo "[post-checkout] Running project's setup script..."
./setup.sh
echo "[post-checkout] Done!"
```

When running a build validation script, tell it to use Git's `--template` option to copy over the hooks:

```shell
./01-validate-incremental-building.sh --git-options '--template ~/git-template-dir'
```

The `post-checkout` script will run automatically after cloning and any checkout. The example logic above would produce this output:

```
Cloning into '.data/01-validate-incremental-building/20250313T144734-67d2f006/build_my-app'...
remote: Enumerating objects: 10980, done.
[...]
[post-checkout] Running project's setup script...
[...]
[post-checkout] Done!
```

[1]: https://git-scm.com/docs/git-init#_template_directory
