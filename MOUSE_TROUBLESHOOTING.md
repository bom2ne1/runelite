# Mouse Coordinate Troubleshooting Guide

## Recent Changes

1. **Reverted Constants.java** - Game canvas back to 765x503 (not 50x50)
2. **Modified ClientPanel** - Allows minimum window size of 50x50
3. **Added debug logging** - TranslateMouseListener now logs dimensions

## How to Debug

### Step 1: Run RuneLite with Logging

1. Launch RuneLite via IntelliJ
2. Open RuneLite Console/Logs
3. Resize window to small size (e.g., 300x200)
4. Move mouse around
5. Watch for log messages like:

```
[StretchedMode] Dimensions - Stretched: 300x200, Real: 765x503, Mouse: (150, 100)
```

### Step 2: Interpret the Output

The log shows three key values:

- **Stretched**: Your actual window size
- **Real**: What the game canvas thinks its size is
- **Mouse**: Raw mouse coordinates (before translation)

### Expected vs Actual

**Scenario 1: Small window WITHOUT Stretched Mode**

Expected:
```
Stretched: 300x200, Real: 300x200, Mouse: (150, 100)
```

If you see:
```
Stretched: 300x200, Real: 765x503, Mouse: (150, 100)  ❌ PROBLEM!
```

**Problem**: Game canvas thinks it's still 765x503 even though window is 300x200

**Scenario 2: Small window WITH Stretched Mode 50%**

Expected:
```
Stretched: 300x200, Real: 150x100, Mouse: (150, 100)
```

If you see something different, that's the issue.

## Common Issues & Solutions

### Issue 1: Real dimensions don't match window size (without Stretched Mode)

**Symptom**:
- Window: 300x200
- Real: 765x503
- Mouse clicks are way off

**Cause**: Game canvas not resizing properly

**Solution**: The game needs to be in **resizable mode**, not fixed mode.

**Fix**:
1. In-game → Settings → Display
2. Select **Resizable** mode (not Fixed)
3. The game canvas should now match your window size

### Issue 2: Canvas offset/position

**Symptom**:
- Dimensions look correct but clicks still offset

**Cause**: Canvas might not be at (0,0) in window

**Solution**: Need to account for canvas offset

Let me know what the debug logs show and we can diagnose further!

## Next Steps

Please run RuneLite and share:
1. Your window size
2. Whether Stretched Mode is ON/OFF
3. The debug log output showing Stretched/Real dimensions
4. Description of where you click vs where it registers

With this info I can pinpoint the exact issue and fix it!

## Advanced: Manual Mouse Translation

If the automatic translation isn't working, we can create a custom plugin that:

1. Detects actual window size
2. Detects actual canvas size and position
3. Calculates correct scale factor
4. Manually translates all mouse events

This would bypass the existing Stretched Mode translation entirely.

Let me know if you want to go this route!
