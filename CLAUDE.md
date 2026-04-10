# RuneLite Custom Build - Development Guide

## Critical Information for AI Assistants

### Plugin Package Requirements ⚠️ CRITICAL

**ALL RuneLite plugins MUST be in the `net.runelite.client.plugins` package to be auto-discovered.**

RuneLite's plugin system only scans the `net.runelite.client.plugins.*` package for plugins. Plugins in other packages (e.g., `com.salverrs`, `org.example`) will compile successfully but **will not appear in the plugin list at runtime**.

#### Correct Plugin Structure
```
runelite-client/src/main/java/
└── net/
    └── runelite/
        └── client/
            └── plugins/
                ├── bomgetracker/          ✅ Correct
                │   └── BomGETrackerPlugin.java
                ├── gefilters/             ✅ Correct
                │   └── GEFiltersPlugin.java
                └── mycoolthing/           ✅ Correct
                    └── MyCoolThingPlugin.java
```

#### Incorrect Plugin Structure (Will NOT Work)
```
runelite-client/src/main/java/
└── com/
    └── salverrs/
        └── GEFilters/             ❌ WRONG - Will compile but not load
            └── GEFiltersPlugin.java
```

### Package Declaration Format

Every plugin must start with:
```java
package net.runelite.client.plugins.<pluginname>;

@PluginDescriptor(
    name = "Plugin Name",
    description = "Plugin description",
    tags = {"tag1", "tag2"},
    enabledByDefault = true  // Optional: auto-enable on first run
)
public class YourPlugin extends Plugin {
    // ...
}
```

### Plugin Hub vs Internal Plugins

- **Plugin Hub plugins**: External JARs loaded at runtime (separate repo)
- **Internal plugins**: Built into RuneLite client (this repo)
- **We are building internal plugins** - they must follow the package rules above

### Verification Checklist

When adding a new plugin to the build:

1. ✅ Plugin is in `net.runelite.client.plugins.<name>` package
2. ✅ All imports reference the correct package
3. ✅ `@PluginDescriptor` annotation is present
4. ✅ Class extends `Plugin`
5. ✅ Compilation succeeds: `./gradlew :client:compileJava`
6. ✅ Plugin appears in RuneLite settings after running

### Current Custom Plugins

#### BomGE Tracker
- **Package**: `net.runelite.client.plugins.bomgetracker`
- **Main Class**: `BomGETrackerPlugin.java`
- **Status**: ✅ Working
- **Enabled by default**: Yes

#### GE Filters
- **Package**: `net.runelite.client.plugins.gefilters`
- **Main Class**: `GEFiltersPlugin.java`
- **Status**: ✅ Working
- **Enabled by default**: Yes
- **Source**: https://github.com/Nick2bad4u/GE-Filters (integrated as internal plugin)
- **Note**: Originally `com.salverrs.GEFilters` - relocated to `net.runelite.client.plugins.gefilters`

#### Flipping Utilities
- **Package**: `net.runelite.client.plugins.flippingutilities`
- **Main Class**: `FlippingPlugin.java`
- **Status**: ✅ Working
- **Note**: Pre-existing in this build

### Build System

#### Compilation
```bash
cd runelite
./gradlew :client:compileJava  # Compile only (fast)
./gradlew build                # Full build with tests (slow)
```

#### Running in IntelliJ IDEA (Recommended)
**DO NOT use `./gradlew run` - it fails with verification errors**

**Run Configuration**:
- Main class: `net.runelite.client.RuneLite`
- VM options: `-ea`
- Program arguments: `--developer-mode`
- Module: `runelite.client.main`
- Working directory: `<project-root>/runelite`

**Steps**:
1. Open `runelite` folder in IntelliJ IDEA
2. Let Gradle import complete
3. Create/edit run configuration with settings above
4. Click green "Run" button
5. No rebuild needed if you just changed code - IntelliJ auto-compiles

### Common Issues

#### Issue: Plugin doesn't appear in RuneLite after compilation
**Cause**: Plugin is in wrong package (not `net.runelite.client.plugins.*`)

**Solution**:
1. Move plugin to `net.runelite.client.plugins/<pluginname>/`
2. Update package declaration in all `.java` files
3. Update all import statements referencing the plugin
4. Recompile and run

**Example Fix**:
```bash
# Move files
mkdir -p runelite-client/src/main/java/net/runelite/client/plugins/gefilters
cp -r runelite-client/src/main/java/com/salverrs/GEFilters/* \
      runelite-client/src/main/java/net/runelite/client/plugins/gefilters/

# Update package declarations
find runelite-client/src/main/java/net/runelite/client/plugins/gefilters -name "*.java" \
     -exec sed -i 's/package com\.salverrs\.GEFilters/package net.runelite.client.plugins.gefilters/g' {} \;

# Update imports
find runelite-client/src/main/java/net/runelite/client/plugins/gefilters -name "*.java" \
     -exec sed -i 's/import com\.salverrs\.GEFilters/import net.runelite.client.plugins.gefilters/g' {} \;

# Remove old location
rm -rf runelite-client/src/main/java/com/salverrs

# Recompile
./gradlew :client:compileJava
```

#### Issue: `./gradlew run` fails with verification errors
**Solution**: Use IntelliJ IDEA run configuration instead (see above)

#### Issue: Plugin compiles but crashes at runtime
**Common Causes**:
- Missing `@PluginDescriptor` annotation
- Missing dependency injection (`@Inject` annotations)
- Trying to access RuneLite API on wrong thread
- Missing plugin dependencies (use `@PluginDependency`)

### Cross-Plugin Communication

When one plugin needs to access another plugin's data:

#### Option 1: Public Methods (Used by GE Filters ↔ BomGE Tracker)
```java
// In BomGETrackerPlugin.java
public HttpDataClient getHttpDataClient() {
    return dataClient;  // Make internal data accessible
}

// In AlchablesSearchFilter.java (GE Filters)
@Inject
private PluginManager pluginManager;

private HttpDataClient getHttpDataClient() {
    Collection<Plugin> plugins = pluginManager.getPlugins();
    for (Plugin plugin : plugins) {
        if ("BomGE Tracker".equals(plugin.getName())) {
            if (!pluginManager.isPluginEnabled(plugin)) {
                return null;
            }
            // Use reflection to call public method
            Method method = plugin.getClass().getMethod("getHttpDataClient");
            return (HttpDataClient) method.invoke(plugin);
        }
    }
    return null;
}
```

#### Option 2: Dependency Injection
```java
@PluginDependency(OtherPlugin.class)
public class MyPlugin extends Plugin {
    @Inject
    private OtherPlugin otherPlugin;  // Auto-injected by RuneLite
}
```

### Plugin Resources

Icons and other resources go in:
```
runelite-client/src/main/resources/net/runelite/client/plugins/<pluginname>/
```

Example:
```
runelite-client/src/main/resources/net/runelite/client/plugins/bomgetracker/
└── grand_exchange_icon.png
```

Load resources with:
```java
ImageUtil.loadImageResource(getClass(),
    "/net/runelite/client/plugins/bomgetracker/icon.png");
```

### Development Workflow

1. **Make code changes** in `runelite-client/src/main/java/net/runelite/client/plugins/<plugin>/`
2. **Click Run** in IntelliJ (auto-compiles changed files)
3. **Test** in running RuneLite instance
4. **Iterate** - IntelliJ hot-reloads most changes

No manual compilation needed when using IntelliJ!

### Documentation

- Main docs: `runelite/RUNELITE_DEVELOPMENT_GUIDE.md`
- Adding plugins: `runelite/ADDING_PLUGINS.md`
- Sprint docs: `runelite-plugin/SPRINT*.md`
- Integration docs: `runelite-plugin/ALCHABLES_FILTER_INTEGRATION.md`

### Git Ignore

The following are auto-generated and should NOT be committed:
```
runelite/build/
runelite/.gradle/
runelite-client/build/
*.class
*.jar (except dependencies)
```

### Environment Variables

- `BOMGE_API_KEY`: API key for BomGE server (defaults to `"dev-key-change-me"`)

Set in IntelliJ run configuration environment variables or shell:
```bash
export BOMGE_API_KEY="your-api-key-here"
```

---

## Quick Reference

### Add New Plugin Checklist
- [ ] Create package: `net.runelite.client.plugins.<pluginname>`
- [ ] Create plugin class extending `Plugin`
- [ ] Add `@PluginDescriptor` annotation
- [ ] Compile: `./gradlew :client:compileJava`
- [ ] Run in IntelliJ and verify plugin appears
- [ ] Test functionality
- [ ] Document in this file

### Package Rule (NEVER FORGET)
```
✅ net.runelite.client.plugins.*
❌ com.anything.else
❌ org.anything.else
```

**If your plugin compiles but doesn't appear → CHECK THE PACKAGE!**
