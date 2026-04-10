package net.runelite.client.plugins.bomgetracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("bomgetracker")
public interface BomGETrackerConfig extends Config
{
	@ConfigSection(
		name = "Server",
		description = "Server connection settings",
		position = 0
	)
	String serverSection = "server";

	@ConfigSection(
		name = "Hotkeys",
		description = "Alch price hotkey settings",
		position = 1
	)
	String hotkeySection = "hotkeys";

	// Hidden until overlay/tooltips are working
	// @ConfigSection(
	// 	name = "GE Overlay",
	// 	description = "Overlay settings for GE search results",
	// 	position = 2
	// )
	// String overlaySection = "overlay";

	// Moved to plugin panel - no longer needed in settings
	// @ConfigSection(
	// 	name = "Alchables Filter",
	// 	description = "Filter and sorting options for alchables",
	// 	position = 3
	// )
	// String filterSection = "filter";

	// ============================================================
	// SERVER SECTION
	// ============================================================

	@ConfigItem(
		section = serverSection,
		keyName = "serverUrl",
		name = "Server URL",
		description = "BomGE server URL (e.g. http://localhost:3000)",
		position = 1
	)
	default String serverUrl()
	{
		return "http://localhost:3000";
	}

	@ConfigItem(
		section = serverSection,
		keyName = "apiKey",
		name = "API Key",
		description = "Shared secret for authenticating with the server",
		position = 2,
		secret = true
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		section = serverSection,
		keyName = "alsoLogToFile",
		name = "Also Log to File",
		description = "Write trades to exchange-logger directory as backup",
		position = 3
	)
	default boolean alsoLogToFile()
	{
		return true;
	}

	// ============================================================
	// HOTKEY SECTION
	// ============================================================

	@ConfigItem(
		section = hotkeySection,
		keyName = "alchHotkeyEnabled",
		name = "Enable Alch Hotkey",
		description = "Press hotkey to calculate HA price for GE offers",
		position = 1
	)
	default boolean alchHotkeyEnabled()
	{
		return true;
	}

	@ConfigItem(
		section = hotkeySection,
		keyName = "alchHotkey",
		name = "Alch Hotkey",
		description = "Hotkey to trigger alch price calculation (single character, e.g. 'H')",
		position = 2
	)
	default String alchHotkey()
	{
		return "H";
	}

	@ConfigItem(
		section = hotkeySection,
		keyName = "alchHotkeyOffset",
		name = "Alch Price Offset",
		description = "Amount to add/subtract from HA price (negative = discount, e.g. -500 means HA - 500gp)",
		position = 3
	)
	@Range(min = -10000, max = 10000)
	default int alchHotkeyOffset()
	{
		return -500;
	}

	@ConfigItem(
		section = hotkeySection,
		keyName = "copyToClipboard",
		name = "Copy to Clipboard",
		description = "Copy calculated price to clipboard when auto-injection fails",
		position = 4
	)
	default boolean copyToClipboard()
	{
		return true;
	}

	// ============================================================
	// GE OVERLAY SECTION (HIDDEN - not working yet)
	// ============================================================

	// @ConfigItem(
	// 	section = overlaySection,
	// 	keyName = "showAlchOverlay",
	// 	name = "Enable Alch Overlay",
	// 	description = "Show alch info overlay on GE search results",
	// 	position = 1
	// )
	// default boolean showAlchOverlay()
	// {
	// 	return true;
	// }

	// @ConfigItem(
	// 	section = overlaySection,
	// 	keyName = "showAlchProfit",
	// 	name = "Show Profit",
	// 	description = "Display alch profit on items",
	// 	position = 2
	// )
	// default boolean showAlchProfit()
	// {
	// 	return true;
	// }

	// @ConfigItem(
	// 	section = overlaySection,
	// 	keyName = "showAlchValue",
	// 	name = "Show HA Value",
	// 	description = "Display high alch value on items",
	// 	position = 3
	// )
	// default boolean showAlchValue()
	// {
	// 	return false;
	// }

	// @ConfigItem(
	// 	section = overlaySection,
	// 	keyName = "showGELimit",
	// 	name = "Show GE Limit",
	// 	description = "Display purchased/limit on items",
	// 	position = 4
	// )
	// default boolean showGELimit()
	// {
	// 	return true;
	// }

	// ============================================================
	// FILTER SECTION (HIDDEN - moved to plugin panel)
	// ============================================================

	// @ConfigItem(
	// 	section = filterSection,
	// 	keyName = "minProfit",
	// 	name = "Min Profit",
	// 	description = "Minimum alch profit to show (gp)",
	// 	position = 1
	// )
	// @Range(min = 0, max = 10000)
	// default int minProfit()
	// {
	// 	return 0;
	// }

	// @ConfigItem(
	// 	section = filterSection,
	// 	keyName = "maxBuyPrice",
	// 	name = "Max Buy Price",
	// 	description = "Maximum item buy price (gp)",
	// 	position = 2
	// )
	// @Range(min = 0, max = 1000000)
	// default int maxBuyPrice()
	// {
	// 	return 100000;
	// }

	// @ConfigItem(
	// 	section = filterSection,
	// 	keyName = "sortBy",
	// 	name = "Sort By",
	// 	description = "How to sort alchables in the filter",
	// 	position = 3
	// )
	// default AlchSortMode sortBy()
	// {
	// 	return AlchSortMode.PROFIT;
	// }

	// enum AlchSortMode
	// {
	// 	PROFIT("Profit"),
	// 	ROI("ROI %"),
	// 	GP_PER_HOUR("GP/Hour");

	// 	private final String name;

	// 	AlchSortMode(String name)
	// 	{
	// 		this.name = name;
	// 	}

	// 	@Override
	// 	public String toString()
	// 	{
	// 		return name;
	// 	}
	// }
}
