# RuneLite Development Guide - Lessons Learned

This document captures all the important fixes, gotchas, and best practices discovered while developing custom RuneLite plugins.

## Table of Contents
1. [Development Environment Setup](#development-environment-setup)
2. [Plugin Development](#plugin-development)
3. [Adding External Plugins](#adding-external-plugins)
4. [Common Issues & Solutions](#common-issues--solutions)
5. [IntelliJ IDEA Specific Issues](#intellij-idea-specific-issues)
6. [API Differences & Compatibility](#api-differences--compatibility)
7. [Testing & Debugging](#testing--debugging)

---

## Development Environment Setup

### Prerequisites
- **Java 11 JDK** (NOT Java 17 or higher - RuneLite requires JDK 11)
- **IntelliJ IDEA** (Community or Ultimate)
- **Git**

### Initial Setup

1. **Clone RuneLite source:**
   ```bash
   git clone https://github.com/runelite/runelite.git
   cd runelite
   ```

2. **Open in IntelliJ:**
   - File → Open → Select `runelite` directory
   - Wait for Gradle sync to complete (can take 5-10 minutes)

3. **CRITICAL: Do NOT use `gradlew run` from command line**
   - Gradle run often fails with verification errors
   - Use IntelliJ's run configuration instead

4. **Set up IntelliJ run configuration:**
   - Main class: `net.runelite.client.RuneLite`
   - VM options: `-ea`
   - Program arguments: `--developer-mode`
   - Module: `runelite.client.main`

### Why IntelliJ Instead of Gradle CLI?

**Issue:** Running `gradlew run` often fails with dependency verification errors
**Solution:** IntelliJ handles the build process more reliably and allows debugging

---

## Plugin Development

### Plugin Location - CRITICAL!

**❌ WRONG:**
```
runelite/runelite-client/src/main/java/com/yourplugin/
```

**✅ CORRECT:**
```
runelite/runelite-client/src/main/java/net/runelite/client/plugins/yourplugin/
```

**Why:** RuneLite only scans `net.runelite.client.plugins.*` package for plugins. Plugins in other packages will **not be discovered**.

### Package Declaration

All plugin files MUST use the correct package:

```java
package net.runelite.client.plugins.yourplugin;
```

NOT:
```java
package com.yourplugin;
```

### Plugin Structure

```
net/runelite/client/plugins/yourplugin/
├── YourPlugin.java          (main class with @PluginDescriptor)
├── YourPluginConfig.java    (configuration interface)
├── SomeHelper.java          (helper classes)
└── ...
```

### Resources Location

Resources should mirror the package structure:

```
runelite-client/src/main/resources/net/runelite/client/plugins/yourplugin/
├── icon.png
├── settings.json
└── ...
```

---

## Adding External Plugins

See [ADDING_PLUGINS.md](./ADDING_PLUGINS.md) for full details. Key points:

### Quick Reference

1. **Clone plugin source**
2. **Copy to correct location** (`net/runelite/client/plugins/pluginname/`)
3. **Update package declarations** (sed command to replace `com.*` with `net.runelite.client.plugins.*`)
4. **Update import statements** (sed command)
5. **Copy resources**
6. **Add dependencies to build.gradle.kts**
7. **Update Gradle verification metadata**
8. **Invalidate IntelliJ caches**
9. **Rebuild**

### CRITICAL: Fully Qualified Class Names

**Issue:** Some plugins use fully qualified class names in method signatures:

```java
// This will NOT be updated by sed
public void doSomething(com.oldplugin.model.Thing thing) {
```

**Solution:** Manually search for remaining references:

```bash
grep -r "com\.oldpluginname" runelite/runelite-client/src/main/java/net/runelite/client/plugins/newpluginname/
```

Then manually fix each occurrence.

---

## Common Issues & Solutions

### 1. Duplicate Class Errors

**Error:**
```
error: duplicate class: com.plugin.SomeClass
```

**Cause:** Plugin files copied to wrong location AND correct location

**How it happens:**
- Used `cp -r` to copy plugin
- Files ended up in both `com/plugin/` and `net/runelite/client/plugins/plugin/`

**Solution:**
```bash
# Remove wrong location
rm -rf runelite/runelite-client/src/main/java/com/

# Clean build
rm -rf runelite/runelite-client/build/

# In IntelliJ
Build → Clean Project
File → Invalidate Caches / Restart
```

### 2. Dependency Verification Failed

**Error:**
```
Dependency verification failed for configuration ':client:compileClasspath'
One artifact failed verification: somelib-1.0.0.jar
```

**Cause:** Added new dependency to `build.gradle.kts` without updating verification metadata

**Solution:**
```bash
cd runelite
./gradlew --write-verification-metadata sha256 help
```

**Then in IntelliJ:**
1. Gradle tab → Reload (circular arrows)
2. Build → Rebuild Project

### 3. Plugin Not Appearing in RuneLite

**Symptoms:**
- Plugin compiles successfully
- No errors in console
- Plugin doesn't appear in Configuration panel

**Cause:** Plugin is in wrong package (not `net.runelite.client.plugins.*`)

**Solution:**
Move plugin to correct location and update packages:

```bash
# Move to correct location
mv runelite/runelite-client/src/main/java/com/yourplugin \
   runelite/runelite-client/src/main/java/net/runelite/client/plugins/

# Update package declarations
find runelite/runelite-client/src/main/java/net/runelite/client/plugins/yourplugin -name "*.java" \
  -exec sed -i 's/package com\.yourplugin/package net.runelite.client.plugins.yourplugin/g' {} \;

# Update imports
find runelite/runelite-client/src/main/java/net/runelite/client/plugins/yourplugin -name "*.java" \
  -exec sed -i 's/import com\.yourplugin/import net.runelite.client.plugins.yourplugin/g' {} \;
```

### 4. "Cannot Find Symbol" Errors After Adding Plugin

**Error:**
```
error: cannot find symbol
import com.flippingutilities.model.Thing;
```

**Cause:** Missed a fully qualified class name or import statement

**Solution:**
```bash
# Search for remaining old package references
grep -r "com\.oldpackage" runelite/runelite-client/src/main/java/net/runelite/client/plugins/newpackage/

# Manually fix each one
```

### 5. OkHttp API Differences

**Error:**
```
error: no suitable method found for create(String,MediaType)
RequestBody.create(json, JSON)
```

**Cause:** Standalone plugins use OkHttp 4.x, RuneLite build uses OkHttp 3.x

**Solution:** Parameter order is reversed in OkHttp 3.x:

```java
// OkHttp 4.x (standalone plugin)
RequestBody.create(json, JSON)

// OkHttp 3.x (RuneLite build)
RequestBody.create(JSON, json)
```

### 6. InterfaceID / VarPlayerID Import Path

**Error:**
```
error: cannot find symbol
import net.runelite.api.VarPlayerID;
```

**Cause:** Constants are in `gameval` subpackage in RuneLite source

**Solution:**
```java
// Wrong
import net.runelite.api.VarPlayerID;
import net.runelite.api.widgets.InterfaceID;

// Correct
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.InterfaceID;
```

### 7. Client Thread Assertion Errors

**Error (in console):**
```
java.lang.AssertionError: must be called on client thread
    at Widget.isHidden(Widget.java:2212)
```

**Cause:** Calling RuneLite client APIs from AWT event thread (KeyListener)

**Solution:** Wrap client API calls in `clientThread.invoke()`:

```java
@Override
public void keyPressed(KeyEvent e) {
    // Check hotkey (safe on AWT thread)
    if (!matchesHotkey(e)) return;

    // Schedule client API calls on client thread
    clientThread.invoke(() -> {
        // Now safe to call client.getWidget(), etc.
        Widget widget = client.getWidget(...);
    });
}
```

---

## IntelliJ IDEA Specific Issues

### Cache Issues

**When to invalidate caches:**
- After adding new plugins
- After moving packages
- After changing Gradle dependencies
- When seeing "duplicate class" errors

**How:**
```
File → Invalidate Caches / Restart
```

### Gradle Sync Issues

**Symptoms:**
- Build errors that don't make sense
- Dependencies not found
- Stale compilation errors

**Solution:**
1. Gradle tab → Reload (circular arrows icon)
2. Build → Clean Project
3. Build → Rebuild Project

### Build Directory Corruption

**Symptoms:**
- Persistent "duplicate class" errors
- Old code still running after changes

**Solution:**
```bash
# Delete build directories
rm -rf runelite/runelite-client/build/
rm -rf runelite/build/

# In IntelliJ
Build → Clean Project
Build → Rebuild Project
```

---

## API Differences & Compatibility

### RuneLite Version Differences

Our build uses **RuneLite 1.10.44**. Standalone plugins may use different versions.

### Common API Differences

#### 1. OkHttp Version
- **Standalone plugins:** OkHttp 4.x
- **RuneLite build:** OkHttp 3.x
- **Fix:** Reverse parameter order in `RequestBody.create()`

#### 2. Widget Constants
- **Standalone plugins:** May use different widget IDs
- **RuneLite build:** Use constants from `InterfaceID.Chatbox.*`
- **Fix:** Check actual constant definitions in source

#### 3. Package Locations
```java
// Common differences:
VarPlayerID        → net.runelite.api.gameval.VarPlayerID
InterfaceID        → net.runelite.api.gameval.InterfaceID
ComponentID        → net.runelite.api.widgets.ComponentID (if exists)
```

### How to Find Correct APIs

1. **Use IntelliJ auto-complete** - It knows the available APIs
2. **Check RuneLite source:** Look at similar plugins in `runelite-client/src/main/java/net/runelite/client/plugins/`
3. **Search for usage:** Use IntelliJ "Find Usages" on API classes

---

## Testing & Debugging

### Running in Development Mode

**Always use IntelliJ run configuration, NOT `gradlew run`**

Run configuration:
- Main class: `net.runelite.client.RuneLite`
- VM options: `-ea`
- Arguments: `--developer-mode`

### Debugging

1. **Set breakpoints** in your plugin code
2. Click **Debug** button (green bug icon) instead of Run
3. Trigger your plugin's functionality in-game
4. Breakpoint will pause execution

### Viewing Logs

**In IntelliJ Console:**
All `log.debug()`, `log.info()`, `log.warn()` messages appear here

**Enable debug logging for your plugin:**
```java
@Slf4j  // Lombok annotation for logger
public class YourPlugin extends Plugin {
    @Override
    protected void startUp() {
        log.debug("Plugin started!");  // Will appear in console
    }
}
```

### Testing Changes

1. **Make code changes**
2. **Build → Build Project** (Ctrl+F9) - Faster than Rebuild
3. **Stop RuneLite** (if running)
4. **Run again** - Changes will be applied

**Note:** Some changes require **Rebuild Project** instead of Build:
- Adding new classes
- Changing package structure
- Modifying `@PluginDescriptor`

---

## Project-Specific Notes

### BomGE Tracker Plugin

**Location:** `net.runelite.client.plugins.bomgetracker`

**Key Features:**
- HTTP trade event posting to Node.js server
- Alch price hotkey (Press 'H' in GE)
- Clickable alch price buttons in GE chatbox
- Offline capable (uses `ItemComposition.getHaPrice()`)

**Important:**
- Positioned alch buttons at **bottom left** of chatbox to avoid conflict with Flipping Utilities (top left)
- Uses `VarPlayerID.TRADINGPOST_SEARCH` to get highlighted item ID
- Uses `VarClientInt.INPUT_TYPE == 7` to detect GE chatbox open
- Uses `clientThread.invoke()` for all client API calls from KeyListener

### Flipping Utilities Plugin

**Location:** `net.runelite.client.plugins.flippingutilities`

**Source:** https://github.com/Flipping-Utilities/rl-plugin

**Dependencies:**
- `commons-csv:1.10.0` (added to build.gradle.kts)

**Package Migration:**
- Original: `com.flippingutilities`
- Migrated to: `net.runelite.client.plugins.flippingutilities`
- **Watch for:** Fully qualified class names in method signatures

---

## Checklist for Adding a New Plugin

Use this checklist to avoid common mistakes:

- [ ] Clone plugin repository
- [ ] Copy source to `net/runelite/client/plugins/pluginname/`
- [ ] Update package declarations (`sed` command)
- [ ] Update import statements (`sed` command)
- [ ] **Search for fully qualified class names** and fix manually
- [ ] Copy resources to `src/main/resources/net/runelite/client/plugins/pluginname/`
- [ ] Check plugin's `build.gradle` for dependencies
- [ ] Add dependencies to `runelite-client/build.gradle.kts`
- [ ] Run `./gradlew --write-verification-metadata sha256 help`
- [ ] In IntelliJ: Invalidate Caches / Restart
- [ ] After restart: Gradle Reload
- [ ] Build → Rebuild Project
- [ ] Run RuneLite and verify plugin appears in Configuration

---

## Quick Command Reference

### Update Package Names
```bash
find path/to/plugin -name "*.java" \
  -exec sed -i 's/package com\.oldname/package net.runelite.client.plugins.newname/g' {} \;

find path/to/plugin -name "*.java" \
  -exec sed -i 's/import com\.oldname/import net.runelite.client.plugins.newname/g' {} \;
```

### Search for Old Package References
```bash
grep -r "com\.oldpackagename" runelite/runelite-client/src/main/java/net/runelite/client/plugins/newpackagename/
```

### Update Gradle Verification
```bash
cd runelite
./gradlew --write-verification-metadata sha256 help
```

### Clean Build
```bash
rm -rf runelite/runelite-client/build/
rm -rf runelite/build/
```

---

## Resources

- **RuneLite API Docs:** https://static.runelite.net/api/runelite-api/
- **RuneLite GitHub:** https://github.com/runelite/runelite
- **Gradle Verification:** https://docs.gradle.org/current/userguide/dependency_verification.html

---

## Troubleshooting Flowchart

```
Plugin not appearing?
├─ Is it in net.runelite.client.plugins.* package?
│  ├─ No → Move it there, update packages
│  └─ Yes → Continue
├─ Did you invalidate IntelliJ caches?
│  ├─ No → File → Invalidate Caches / Restart
│  └─ Yes → Continue
└─ Check console for errors

Build errors?
├─ "Duplicate class" error?
│  └─ Clean build directories, invalidate caches
├─ "Cannot find symbol" error?
│  ├─ Check import paths (gameval subpackage)
│  └─ Search for old package references
├─ "Dependency verification failed"?
│  └─ Run gradlew --write-verification-metadata
└─ Other error?
   └─ Check this guide for similar issues

Runtime errors?
├─ "must be called on client thread"?
│  └─ Wrap in clientThread.invoke()
├─ Plugin works in standalone but not in build?
│  └─ Check API Differences section
└─ Widget not found?
   └─ Check InterfaceID constants, may differ by version
```

---

## Final Notes

- **Always use IntelliJ** for building and running
- **Always invalidate caches** after structural changes
- **Always check package locations** - 90% of issues are wrong packages
- **Don't forget fully qualified class names** - sed won't catch them
- **Keep this guide updated** when discovering new issues

Good luck! 🚀
