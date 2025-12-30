1. __`gradle.properties`__ - Added publishing configuration:

    - Group ID: `com.beyond-eye`
    - Artifact ID: `libopenmpt-kmp`
    - Version: `1.0.0`
    - POM metadata (SCM, license, developer info)

2. __`shared/build.gradle.kts`__ - Added:

    - `maven-publish` and `signing` plugins
    - Publishing configuration with Sonatype OSSRH repositories
    - POM metadata customization
    - GPG signing for Maven Central compliance
    - Android Release variant publishing enabled

## Publishing Tasks Available

All publications are properly configured:

- `kotlinMultiplatform` (main metadata)
- `androidRelease` (Android AAR)
- `desktop` (Desktop JVM)
- `iosArm64` (iOS device)
- `iosSimulatorArm64` (iOS simulator)
- `wasmJs` (WebAssembly)

### HOW Generate GPG Keys

```bash
brew install gpg
gpg --full-generate-key
gpg --list-keys --keyid-format LONG
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --armor --export-secret-keys YOUR_KEY_ID | base64 > gpg-key-base64.txt
```
### Obtain sonatype username and password:
https://central.sonatype.com/usertoken
### 3. Configure GitHub Repository Secrets

Add these secrets to your repository (Settings → Secrets and variables → Actions):

- `OSSRH_USERNAME` - Sonatype username
- `OSSRH_PASSWORD` - Sonatype password
- `GPG_PRIVATE_KEY` - Base64-encoded GPG key
- `GPG_PASSPHRASE` - GPG key passphrase

### 4. Publish

Either:

- Create a GitHub Release to trigger automatic publishing
- Run manually: `./gradlew :shared:publishAllPublicationsToSonatypeRepository`

### 5. Test Locally First (Optional)

```bash
./gradlew :shared:publishAllPublicationsToLocalRepository
# Artifacts will be in shared/build/repo/
```

## Maven Coordinates (After Publishing)

```kotlin
implementation("com.beyond-eye:libopenmpt-kmp:1.0.0")
```
