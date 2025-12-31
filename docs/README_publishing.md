# Publishing to Maven Central

This document describes how to publish the `libopenmpt-kmp` library to Maven Central via the new **Central Portal** (https://central.sonatype.com/).

## Overview

The project uses the [Vanniktech Maven Publish Plugin](https://github.com/vanniktech/gradle-maven-publish-plugin) for publishing to Maven Central. This plugin handles:

- Automatic POM generation with metadata
- GPG signing of all artifacts
- Publishing to Maven Central via the new Central Portal API

## Configuration Files

### 1. `gradle.properties` - Publishing Metadata

Contains the library coordinates and POM metadata:

- Group ID: `com.beyond-eye`
- Artifact ID: `libopenmpt-kmp`
- Version: `1.0.0`
- POM metadata (SCM, license, developer info)

### 2. `shared/build.gradle.kts` - Plugin Configuration

Uses the Vanniktech Maven Publish plugin configured for:

- Central Portal publishing (`SonatypeHost.CENTRAL_PORTAL`)
- Automatic GPG signing
- POM metadata from gradle.properties

## Publishing Targets

All Kotlin Multiplatform targets are published:

- `kotlinMultiplatform` (main metadata)
- `androidRelease` (Android AAR)
- `desktop` (Desktop JVM)
- `iosArm64` (iOS device)
- `iosSimulatorArm64` (iOS simulator)
- `wasmJs` (WebAssembly)

## Prerequisites

### 1. Central Portal Account

1. Create an account at https://central.sonatype.com/
2. Verify your namespace (e.g., `com.beyond-eye`) via GitHub or DNS verification
3. Generate a User Token:
   - Go to https://central.sonatype.com/account
   - Click "Generate User Token"
   - Save both the username and password

### 2. GPG Key for Signing

Generate and export your GPG key:

```bash
# Install GPG (macOS)
brew install gpg

# Generate a new key
gpg --full-generate-key

# List keys to find your KEY_ID
gpg --list-keys --keyid-format LONG

# Upload key to public keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# Export private key in ASCII-armored format
gpg --armor --export-secret-keys YOUR_KEY_ID > gpg-key.asc
```

**Important:** The GitHub workflow requires the ASCII-armored format for the GPG key.
Copy the entire output (including `-----BEGIN PGP PRIVATE KEY BLOCK-----` and `-----END PGP PRIVATE KEY BLOCK-----`) into the GitHub secret.

### 3. Configure GitHub Repository Secrets

Add these secrets to your repository (Settings → Secrets and variables → Actions):

| Secret Name | Description |
|-------------|-------------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal token password |
| `GPG_PRIVATE_KEY` | ASCII-armored GPG private key |
| `GPG_PASSPHRASE` | GPG key passphrase |

## Publishing

### Option 1: Automatic via GitHub Release

1. Create a new GitHub Release
2. The workflow will automatically:
   - Build all targets
   - Sign artifacts
   - Publish to Maven Central

### Option 2: Manual GitHub Workflow

1. Go to Actions → "Publish to Maven Central"
2. Click "Run workflow"
3. Set `publish_to_maven` to `true`
4. Click "Run workflow"

### Option 3: Local Publishing (for testing)

Configure credentials in `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=your-token-username
mavenCentralPassword=your-token-password
signing.keyId=LAST_8_CHARS_OF_KEY_ID
signing.password=your-gpg-passphrase
signing.secretKeyRingFile=/path/to/secring.gpg
```

Then run:

```bash
./gradlew :shared:publishAllPublicationsToMavenCentral
```

## Maven Coordinates (After Publishing)

Once published, consumers can add the dependency:

```kotlin
// build.gradle.kts
implementation("com.beyond-eye:libopenmpt-kmp:1.0.0")
```

Or for specific platforms:

```kotlin
// Android only
implementation("com.beyond-eye:libopenmpt-kmp-android:1.0.0")

// Desktop JVM only
implementation("com.beyond-eye:libopenmpt-kmp-desktop:1.0.0")

// iOS (via CocoaPods or direct klib)
implementation("com.beyond-eye:libopenmpt-kmp-iosarm64:1.0.0")
```

## Troubleshooting

### "Could not read PGP secret key"

- Ensure the GPG key is in ASCII-armored format (starts with `-----BEGIN PGP PRIVATE KEY BLOCK-----`)
- Do NOT base64-encode the key; use it as-is

### "401 Unauthorized" / "Content access is protected by token"

- Verify you're using Central Portal credentials (not legacy OSSRH)
- Regenerate your User Token at https://central.sonatype.com/account
- Ensure `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` secrets are set correctly

### "Namespace not verified"

- Verify your namespace ownership at https://central.sonatype.com/
- The namespace verification can take a few minutes to propagate

## Migration from Legacy OSSRH

If you previously used the legacy OSSRH (s01.oss.sonatype.org), note these changes:

| Old (Legacy OSSRH) | New (Central Portal) |
|--------------------|---------------------|
| `OSSRH_USERNAME` | `MAVEN_CENTRAL_USERNAME` |
| `OSSRH_PASSWORD` | `MAVEN_CENTRAL_PASSWORD` |
| `s01.oss.sonatype.org` | `central.sonatype.com` |
| `maven-publish` + `signing` plugins | `com.vanniktech.maven.publish` plugin |
| `publishAllPublicationsToSonatypeRepository` | `publishAllPublicationsToMavenCentral` |

## References

- [Vanniktech Maven Publish Plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
- [Central Portal Documentation](https://central.sonatype.org/publish/)
- [OSSRH End of Life Announcement](https://central.sonatype.org/pages/ossrh-eol/)
