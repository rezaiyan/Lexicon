fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android build

```sh
[bundle exec] fastlane android build
```

Build the release AAB

### android beta

```sh
[bundle exec] fastlane android beta
```

Upload a new build to closed testing (alpha track)

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Build and deploy to a closed testing track locally. Use `track` param to specify the track name (default: alpha).

### android release

```sh
[bundle exec] fastlane android release
```

Promote closed testing build to production

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
