# Adding External Plugins to RuneLite Build

This document explains how to add external RuneLite plugins directly into the build (instead of sideloading).

## Why Add to Build Instead of Sideload?

When developing a custom RuneLite build, external plugins often fail to load via the plugin hub or sideloading. Adding them directly to the build ensures:
- Plugins are always available
- No download/verification issues
- Full integration with IntelliJ debugging
- Works in dev mode (`gradlew run`)

## Steps to Add a Plugin

### 1. Clone the Plugin Repository

```bash
cd /tmp
git clone --depth 1 https://github.com/PLUGIN_AUTHOR/PLUGIN_NAME.git
```

### 2. Copy Plugin Source Code

**CRITICAL:** Plugins MUST be in the `net.runelite.client.plugins` package to be discovered!

```bash
# Copy to the plugins directory
cp -r /tmp/PLUGIN_NAME/src/main/java/com/pluginname runelite/runelite-client/src/main/java/net/runelite/client/plugins/
```

### 3. Update Package Declarations

All plugin files must use `net.runelite.client.plugins.pluginname` package:

```bash
# Update package declarations
find runelite/runelite-client/src/main/java/net/runelite/client/plugins/pluginname -name "*.java" \
  -exec sed -i 's/package com\.pluginname/package net.runelite.client.plugins.pluginname/g' {} \;

# Update import statements
find runelite/runelite-client/src/main/java/net/runelite/client/plugins/pluginname -name "*.java" \
  -exec sed -i 's/import com\.pluginname/import net.runelite.client.plugins.pluginname/g' {} \;
```

### 4. Copy Resources

```bash
mkdir -p runelite/runelite-client/src/main/resources/net/runelite/client/plugins/pluginname
cp -r /tmp/PLUGIN_NAME/src/main/resources/* \
  runelite/runelite-client/src/main/resources/net/runelite/client/plugins/pluginname/
```

### 5. Add Plugin Dependencies

Check the plugin's `build.gradle` for dependencies:

```bash
cat /tmp/PLUGIN_NAME/build.gradle | grep -A 10 "dependencies"
```

Add any required dependencies to `runelite/runelite-client/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...
    api("org.apache.commons:commons-csv:1.10.0")  // Example
}
```

### 6. Update Gradle Verification Metadata

RuneLite uses dependency verification. After adding new dependencies:

```bash
cd runelite
./gradlew --write-verification-metadata sha256 help
```

This updates `gradle/verification-metadata.xml` with checksums for the new dependencies.

### 7. Rebuild in IntelliJ

1. **File** → **Invalidate Caches / Restart**
2. After restart:
   - **Gradle** tab → Click **Reload** (circular arrows)
   - **Build** → **Rebuild Project**
3. Run RuneLite (green play button)
4. Check Configuration → Plugin should appear!

## Current Plugins Added to Build

### BomGE Tracker
- **Location**: `runelite-client/src/main/java/net/runelite/client/plugins/bomgetracker/`
- **Purpose**: Custom GE tracking with HTTP integration
- **Dependencies**: None (uses bundled OkHttp/Gson)

### Flipping Utilities
- **Location**: `runelite-client/src/main/java/net/runelite/client/plugins/flippingutilities/`
- **Source**: https://github.com/Flipping-Utilities/rl-plugin
- **Dependencies**: `commons-csv:1.10.0`
- **Purpose**: GE flipping tools, price tracking

## Troubleshooting

### Plugin Not Appearing in RuneLite

**Cause:** Plugin is in wrong package
**Fix:** Must be in `net.runelite.client.plugins.*` package

```bash
# Check current location
ls runelite/runelite-client/src/main/java/net/runelite/client/plugins/

# Should see: bomgetracker, flippingutilities, etc.
```

### Duplicate Class Errors

**Cause:** Plugin files copied to wrong location (e.g., `com/` instead of `net/runelite/client/plugins/`)
**Fix:** Remove duplicates and clean build

```bash
# Remove wrong location
rm -rf runelite/runelite-client/src/main/java/com/pluginname

# Clean build
rm -rf runelite/runelite-client/build/

# In IntelliJ: Build → Clean Project
```

### Dependency Verification Failed

**Cause:** New dependency not in verification metadata
**Fix:** Run verification metadata update

```bash
cd runelite
./gradlew --write-verification-metadata sha256 help
```

### Import Errors After Moving Plugin

**Cause:** Package names not updated
**Fix:** Run sed commands from Step 3 to update all package/import statements

## Notes

- **DO NOT** manually edit `gradle/verification-metadata.xml` - always use the Gradle command
- **ALWAYS** invalidate IntelliJ caches after adding plugins
- **REMEMBER** to update package names from `com.*` to `net.runelite.client.plugins.*`
- Plugin resources should mirror the Java package structure

## Example: Adding Flipping Utilities (What We Did)

```bash
# 1. Clone
cd /tmp
git clone --depth 1 https://github.com/Flipping-Utilities/rl-plugin.git flipping-utilities

# 2. Copy source (to correct location)
cp -r /tmp/flipping-utilities/src/main/java/com/flippingutilities \
  runelite/runelite-client/src/main/java/net/runelite/client/plugins/

# 3. Update packages
find runelite/runelite-client/src/main/java/net/runelite/client/plugins/flippingutilities -name "*.java" \
  -exec sed -i 's/package com\.flippingutilities/package net.runelite.client.plugins.flippingutilities/g' {} \;

find runelite/runelite-client/src/main/java/net/runelite/client/plugins/flippingutilities -name "*.java" \
  -exec sed -i 's/import com\.flippingutilities/import net.runelite.client.plugins.flippingutilities/g' {} \;

# 4. Copy resources
mkdir -p runelite/runelite-client/src/main/resources/net/runelite/client/plugins/flippingutilities
cp -r /tmp/flipping-utilities/src/main/resources/* \
  runelite/runelite-client/src/main/resources/net/runelite/client/plugins/flippingutilities/

# 5. Add commons-csv to build.gradle.kts
# (Edit file manually or via script)

# 6. Update verification
cd runelite
./gradlew --write-verification-metadata sha256 help

# 7. IntelliJ: Invalidate Caches → Reload Gradle → Rebuild
```

## Common Mistakes to Avoid

1. ❌ Copying to `com/` directory
   ✅ Copy to `net/runelite/client/plugins/`

2. ❌ Forgetting to update package declarations
   ✅ Run sed commands to update all .java files

3. ❌ Adding dependency without updating verification metadata
   ✅ Run `--write-verification-metadata` after editing build.gradle.kts

4. ❌ Not invalidating IntelliJ caches
   ✅ Always invalidate caches after structural changes

5. ❌ Expecting plugin hub to work in custom build
   ✅ Add plugins directly to source code instead
