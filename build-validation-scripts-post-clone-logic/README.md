# Develocity Build Validation Scripts post clone logic

Develocity Build Validation Scripts must run builds on a fresh clone of your repository.
This is an example of how to run custom logic after cloning, such as copying dependencies,
setting up configuration files, or any shell command.

Place your logic in a `post-checkout` hook, to be used as a git template dir:

```shell
# Create a new directory for the git template
mkdir -p git-template-dir/hooks
echo "#!/usr/bin/env bash" > git-template-dir/hooks/post-checkout
chmod +x git-template-dir/hooks/post-checkout
```

Place your logic inside the `post-checkout` file. For example,

```shell
echo "[post-checkout] Running project's setup script..."
./setup.sh
echo "[post-checkout] Done!"
```

When running a build validation script, tell it to use Git's `--template` option to copy over the hooks:

```shell
./01-validate-incremental-building.sh --git-options '--template ~/git-template-dir'
```

This will copy the hooks to the `.git` directory of the new clone, and the `post-checkout` script will run automatically after cloning and any checkout. The example logic above would produce this output:

```
Cloning into '.data/01-validate-incremental-building/20250313T144734-67d2f006/build_my-app'...
remote: Enumerating objects: 10980, done.
[...]
[post-checkout] Running project's setup script...
[...]
[post-checkout] Done!
```
