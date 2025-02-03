# How to release Open-Source project using GitHub Actions.

All necessary information you can find in [Central Portal](https://central.sonatype.org/register/central-portal/).

---

## 1. Signing Key

Singing Key is needed to sign all the artifacts of your OSS project.  
To do so, we need to generate key and publish it to the public server, so others can verify the artifacts.

### 1.1 Signing Key Generation

To create signing key, use [this manual](https://central.sonatype.org/publish/requirements/gpg/#installing-gnupg) to generate it.

Add Key ID and Pass phrase to _GitHub Actions Secrets_ as `SIGNING_KEY_ID` and `SIGNING_PASSWORD`.

### 1.2 Publishing the Signing Key

To publish Signing key use this command:

```shell
gpg --keyserver keys.openpgp.org --send-keys {long-key-id}
```

NOTES:
In case `gpg` tool does not responds and looks stuck, make sure you don't have any locks.
```shell
rm -rf ~/.gnupg/*.lock
rm -rf ~/.gnupg/public-keys.d/*.lock
```

### 1.3 Extract Secret Key

To extract the Secret Key use this command:

```shell
gpg --list-secret-keys --keyid-format=long
gpg --export-secret-keys --armor {long-key-id}
```

This will generate a text in form:
```
-----BEGIN PGP PRIVATE KEY BLOCK-----

UCZchs7wIbAwUJBaOag...
-----END PGP PRIVATE KEY BLOCK-----
```

This value should be stored in GitHub Actions as a secret (`SIGNING_KEY`).

---

## 2. User Token

To push artifacts to the Central Repository we need to get a _User Token_.  
To do so:
* Go to https://s01.oss.sonatype.org/#profile;User%20Token
* Click to _Access User Token_.

Save `username` and `password` to the _GitHub Actions Secrets_ as `OSSRH_USERNAME` and `OSSRH_PASSWORD`.  

---

## 3. Extend Gradle configuration

To extend Grable build configuration add this line to your `build.gradle.kts`:
```kotlin
apply(from = "https://raw.githubusercontent.com/temofey1989/oss-release-action/main/release.gradle.kts")
```

Your build configuration doesn't have to get this extension all the time. You can add it in release stage while releasing of the OSS project.  
It can be done with simple shell command.
```shell
echo 'apply(from = "https://raw.githubusercontent.com/temofey1989/oss-release-action/main/release.gradle.kts")' >> build.gradle.kts
```

This configuration will add necessary plugins to the build process to **sign** and **publish** your OSS project to Central Repository.

---

## 4. Release Flow

To publish new version of the library with GitHub Action we need to add new workflow.

`.github/workflow/release.yml`:

```yaml
name: Release

on:
  release:
    types: [ published ]

jobs:
  release:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: 'liberica'

      - name: Inject Gradle Script
        run: echo 'apply(from = "https://raw.githubusercontent.com/temofey1989/oss-release-action/main/release.gradle.kts")' >> build.gradle.kts

      - uses: gradle/actions/setup-gradle@v3

      - run: ./gradlew -Pversion=${{github.event.release.name}} build publishToSonatype closeAndReleaseSonatypeStagingRepository
        env:
          PROJECT_DESCRIPTION: ${{github.event.repository.description}}
          LICENSE_NAME: The Apache License, Version 2.0
          LICENSE_URL: https://www.apache.org/licenses/LICENSE-2.0.txt
          SCM_URL: ${{github.server_url}}/${{github.repository}}
          SCM_CONNECTION: scm:git:${{github.repositoryUrl}}
          SCM_DEVELOPER_CONNECTION: scm:git:ssh://github.com:${{github.repository}}
          DEVELOPER_ID: ${{github.event.repository.owner.login}}
          DEVELOPER_NAME: ${{github.event.repository.owner.name}}
          DEVELOPER_EMAIL: ${{github.event.repository.owner.email}}

          ORG_GRADLE_PROJECT_signingKeyId: ${{secrets.SIGNING_KEY_ID}}
          ORG_GRADLE_PROJECT_signingKey: ${{secrets.SIGNING_KEY}}
          ORG_GRADLE_PROJECT_signingPassword: ${{secrets.SIGNING_PASSWORD}}

          ORG_GRADLE_PROJECT_sonatypeNexusUrl: https://s01.oss.sonatype.org/service/local/
          ORG_GRADLE_PROJECT_sonatypeSnapshotRepositoryUrl: https://s01.oss.sonatype.org/content/repositories/snapshots/
          ORG_GRADLE_PROJECT_ossrhUsername: ${{secrets.OSSRH_USERNAME}}
          ORG_GRADLE_PROJECT_ossrhPassword: ${{secrets.OSSRH_PASSWORD}}
```

## 5. Start Releasing

To start a release, create a new Release in GitHub Actions.  
The name of the Release will be used as a version of the Gradle build.
