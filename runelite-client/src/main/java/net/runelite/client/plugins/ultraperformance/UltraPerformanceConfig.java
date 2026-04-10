package net.runelite.client.plugins.ultraperformance;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("ultraperformance")
public interface UltraPerformanceConfig extends Config
{
	@ConfigSection(
		name = "Memory Management",
		description = "Aggressive memory optimization settings",
		position = 0
	)
	String memorySection = "memory";

	@ConfigSection(
		name = "Rendering",
		description = "Graphics and rendering optimizations",
		position = 1
	)
	String renderingSection = "rendering";

	// ============================================================
	// MEMORY SECTION
	// ============================================================

	@ConfigItem(
		section = memorySection,
		keyName = "aggressiveGC",
		name = "Aggressive GC",
		description = "Trigger garbage collection when memory usage exceeds threshold",
		position = 1
	)
	default boolean aggressiveGC()
	{
		return true;
	}

	@ConfigItem(
		section = memorySection,
		keyName = "gcInterval",
		name = "GC Check Interval",
		description = "How often to check memory usage (seconds)",
		position = 2
	)
	@Range(min = 5, max = 60)
	default int gcInterval()
	{
		return 10;
	}

	@ConfigItem(
		section = memorySection,
		keyName = "gcThreshold",
		name = "GC Threshold",
		description = "Trigger GC when memory usage exceeds this percentage",
		position = 3
	)
	@Range(min = 50, max = 95)
	default int gcThreshold()
	{
		return 75;
	}

	// ============================================================
	// RENDERING SECTION
	// ============================================================

	@ConfigItem(
		section = renderingSection,
		keyName = "lowMemoryMode",
		name = "Low Memory Mode",
		description = "Enable OSRS low memory mode (reduces textures/details)",
		position = 1
	)
	default boolean lowMemoryMode()
	{
		return true;
	}

	@ConfigItem(
		section = renderingSection,
		keyName = "minDrawDistance",
		name = "Minimum Draw Distance",
		description = "Force minimum rendering distance",
		position = 2
	)
	default boolean minDrawDistance()
	{
		return true;
	}

	@ConfigItem(
		section = renderingSection,
		keyName = "drawDistance",
		name = "Draw Distance",
		description = "Rendering distance in tiles (lower = better performance)",
		position = 3
	)
	@Range(min = 10, max = 90)
	default int drawDistance()
	{
		return 25;
	}

	@ConfigItem(
		section = renderingSection,
		keyName = "disableAnimations",
		name = "Disable Animations",
		description = "Disable unnecessary animations to save CPU",
		position = 4
	)
	default boolean disableAnimations()
	{
		return false;
	}
}
