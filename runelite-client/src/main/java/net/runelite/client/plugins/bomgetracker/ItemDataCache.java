package net.runelite.client.plugins.bomgetracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Unified interface for item data (local + server).
 * Provides high alch prices, GE prices, and item names using RuneLite APIs.
 */
@Slf4j
@Singleton
public class ItemDataCache
{
	@Inject
	private ItemManager itemManager;

	/**
	 * Get high alch price for an item (always available offline).
	 * Uses ItemComposition.getHaPrice() which is 60% of store price.
	 */
	public int getHighAlchPrice(int itemId)
	{
		try
		{
			ItemComposition comp = itemManager.getItemComposition(itemId);
			return comp.getHaPrice();
		}
		catch (Exception e)
		{
			log.warn("Failed to get HA price for item {}: {}", itemId, e.getMessage());
			return 0;
		}
	}

	/**
	 * Get GE price for an item (uses RuneLite's wiki price API).
	 * Returns 0 if price data is unavailable.
	 */
	public int getGePrice(int itemId)
	{
		try
		{
			return itemManager.getItemPrice(itemId);
		}
		catch (Exception e)
		{
			log.warn("Failed to get GE price for item {}: {}", itemId, e.getMessage());
			return 0;
		}
	}

	/**
	 * Get item name (always available).
	 */
	public String getItemName(int itemId)
	{
		try
		{
			return itemManager.getItemComposition(itemId).getName();
		}
		catch (Exception e)
		{
			log.warn("Failed to get item name for {}: {}", itemId, e.getMessage());
			return "Unknown Item";
		}
	}
}
