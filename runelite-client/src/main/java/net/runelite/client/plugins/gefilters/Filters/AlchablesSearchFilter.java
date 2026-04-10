package net.runelite.client.plugins.gefilters.Filters;

import net.runelite.client.plugins.gefilters.Filters.Model.FilterOption;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.bomgetracker.AlchablesOverlay;
import net.runelite.client.plugins.bomgetracker.BomGETrackerPlugin;
import net.runelite.client.plugins.bomgetracker.HttpDataClient;
import net.runelite.client.plugins.bomgetracker.model.AlchData;
import net.runelite.client.plugins.bomgetracker.ui.AlchTabPanel;
import net.runelite.client.plugins.bomgetracker.ui.BomGETrackerPanel;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * GE Filters integration for BomGE alchables.
 * Shows top alch profit items from the server in the GE search interface.
 */
@Slf4j
@Singleton
public class AlchablesSearchFilter extends SearchFilter
{
	private static final int SPRITE_ID_MAIN = 1369; // Fire rune sprite for alching
	private static final String TITLE_ALL_ALCHABLES = "All Alchables";
	private static final String TITLE_F2P_ALCHABLES = "F2P Alchables";
	private static final String SEARCH_BASE_ALL_ALCHABLES = "bomge-all-alchables";
	private static final String SEARCH_BASE_F2P_ALCHABLES = "bomge-f2p-alchables";
	private static final int MAX_ALCH_RESULTS = 50; // Show top 50 alch items
	private static final String BOMGE_PLUGIN_NAME = "BomGE Tracker";

	@Inject
	private PluginManager pluginManager;

	private FilterOption allAlchables;
	private FilterOption f2pAlchables;
	private List<Short> allAlchablesItemIds = new ArrayList<>();
	private List<Short> f2pAlchablesItemIds = new ArrayList<>();
	private List<AlchData> fullAlchDataList = new ArrayList<>(); // Keep full data for overlay
	private AlchablesOverlay overlay; // Store reference to overlay

	@Override
	protected void onFilterInitialising()
	{
		allAlchables = new FilterOption(TITLE_ALL_ALCHABLES, SEARCH_BASE_ALL_ALCHABLES);
		f2pAlchables = new FilterOption(TITLE_F2P_ALCHABLES, SEARCH_BASE_F2P_ALCHABLES);
		setFilterOptions(allAlchables, f2pAlchables);
		setIconSprite(SPRITE_ID_MAIN, 0);

		log.info("[BomGE] Alchables filter initialized with All/F2P options");
	}

	@Override
	protected void onFilterStarted()
	{
		// Refresh alch data when GE is opened
		refreshAlchData();
	}

	@Override
	protected void onFilterEnabled(FilterOption option)
	{
		// Ensure we have fresh data before showing
		if (allAlchablesItemIds.isEmpty())
		{
			refreshAlchData();
			// Wait a bit for async data to load
			try { Thread.sleep(500); } catch (InterruptedException e) {}
		}

		if (option == allAlchables)
		{
			addItemFilterResults(allAlchablesItemIds);
			log.info("[BomGE] Showing {} all alchable items", allAlchablesItemIds.size());
		}
		else if (option == f2pAlchables)
		{
			addItemFilterResults(f2pAlchablesItemIds);
			log.info("[BomGE] Showing {} F2P alchable items", f2pAlchablesItemIds.size());
		}
	}

	/**
	 * Fetch top alch items from server and update the filter lists.
	 */
	private void refreshAlchData()
	{
		// Run async to avoid blocking the game thread
		new Thread(() -> {
			try
			{
				HttpDataClient dataClient = getHttpDataClient();
				if (dataClient == null)
				{
					log.warn("[BomGE] BomGE Tracker plugin not enabled or HttpDataClient not available");
					return;
				}

				log.info("[BomGE] Fetching alch data from server...");
				List<AlchData> alchList = dataClient.fetchAlchProfits();

				if (alchList == null || alchList.isEmpty())
				{
					log.warn("[BomGE] No alch data received from server");
					return;
				}

				// Store full list for overlay
				fullAlchDataList = alchList;

				// Update overlay with full data
				if (overlay != null)
				{
					overlay.updateAlchData(alchList);
				}

				// Get filter settings from panel
				AlchTabPanel.FilterSettings settings = getPanelFilterSettings();

				// Apply filters and sorting
				List<AlchData> filteredList = filterAndSortAlchData(alchList, settings);

				// Separate into all alchables and F2P alchables
				List<Short> allItems = new ArrayList<>();
				List<Short> f2pItems = new ArrayList<>();

				for (AlchData alch : filteredList)
				{
					// Add to all alchables list
					if (allItems.size() < MAX_ALCH_RESULTS)
					{
						allItems.add((short) alch.getId());
					}

					// Add to F2P list if not members-only
					if (!alch.isMembers() && f2pItems.size() < MAX_ALCH_RESULTS)
					{
						f2pItems.add((short) alch.getId());
					}
				}

				allAlchablesItemIds = allItems;
				f2pAlchablesItemIds = f2pItems;
				log.info("[BomGE] Loaded {} all alchables, {} F2P alchables (filtered from {} total)",
					allAlchablesItemIds.size(), f2pAlchablesItemIds.size(), alchList.size());
			}
			catch (Exception e)
			{
				log.error("[BomGE] Failed to fetch alch data", e);
			}
		}).start();
	}

	/**
	 * Get HttpDataClient from BomGE Tracker plugin using reflection.
	 */
	private HttpDataClient getHttpDataClient()
	{
		try
		{
			Collection<Plugin> plugins = pluginManager.getPlugins();
			for (Plugin plugin : plugins)
			{
				if (BOMGE_PLUGIN_NAME.equals(plugin.getName()))
				{
					if (!pluginManager.isPluginEnabled(plugin))
					{
						return null;
					}

					// Use reflection to access the getHttpDataClient() method
					Method method = plugin.getClass().getMethod("getHttpDataClient");
					return (HttpDataClient) method.invoke(plugin);
				}
			}
		}
		catch (Exception e)
		{
			log.error("[BomGE] Failed to get HttpDataClient from BomGE Tracker", e);
		}

		return null;
	}

	/**
	 * Filter and sort alch data based on panel filter settings.
	 */
	private List<AlchData> filterAndSortAlchData(List<AlchData> alchList, AlchTabPanel.FilterSettings settings)
	{
		if (settings == null)
		{
			return alchList; // No settings, return all
		}

		// Get limit tracker and character for hide @ limit filter
		Object[] limitData = getLimitTrackerAndCharacter();
		Object limitTracker = limitData[0];
		String currentCharacter = (String) limitData[1];

		List<AlchData> filtered = new ArrayList<>();

		for (AlchData alch : alchList)
		{
			// Skip unprofitable items
			if (alch.getProfit() <= 0)
			{
				continue;
			}

			// Apply min profit filter
			if (alch.getProfit() < settings.minProfit)
			{
				continue;
			}

			// Apply max buy price filter
			if (alch.getBuyPrice() > settings.maxBuyPrice)
			{
				continue;
			}

			// Apply min volume filter
			if (alch.getVolume24h() < settings.minVolume)
			{
				continue;
			}

			// Apply hide @ limit filter
			if (settings.hideAtLimit && limitTracker != null && currentCharacter != null)
			{
				try
				{
					Method getPurchasedMethod = limitTracker.getClass().getMethod("getPurchased", String.class, int.class);
					int purchased = (int) getPurchasedMethod.invoke(limitTracker, currentCharacter, alch.getId());
					Integer limit = alch.getLimit();

					if (limit != null && limit > 0 && purchased >= limit)
					{
						continue; // Skip items at limit
					}
				}
				catch (Exception e)
				{
					log.debug("[BomGE] Could not check limit for item {}", alch.getId());
				}
			}

			filtered.add(alch);
		}

		// Sort based on panel setting
		String sortMode = settings.sortMode;
		Comparator<AlchData> comparator;

		if ("By ROI%".equals(sortMode))
		{
			comparator = Comparator.comparingDouble(AlchData::getRoi).reversed();
		}
		else if ("By GP/Hr".equals(sortMode))
		{
			comparator = Comparator.comparingInt(AlchData::getProfitPerHour).reversed();
		}
		else // "By Profit"
		{
			comparator = Comparator.comparingInt(AlchData::getProfit).reversed();
		}

		filtered.sort(comparator);
		return filtered;
	}

	/**
	 * Get filter settings from AlchTabPanel via reflection.
	 */
	private AlchTabPanel.FilterSettings getPanelFilterSettings()
	{
		try
		{
			Collection<Plugin> plugins = pluginManager.getPlugins();
			for (Plugin plugin : plugins)
			{
				if (BOMGE_PLUGIN_NAME.equals(plugin.getName()))
				{
					if (!pluginManager.isPluginEnabled(plugin))
					{
						return null;
					}

					// Use reflection to get panel and filter settings
					Method getPanelMethod = plugin.getClass().getMethod("getPanel");
					BomGETrackerPanel panel = (BomGETrackerPanel) getPanelMethod.invoke(plugin);

					if (panel != null)
					{
						AlchTabPanel alchTab = panel.getAlchTab();
						if (alchTab != null)
						{
							return alchTab.getFilterSettings();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.error("[BomGE] Failed to get filter settings from panel", e);
		}

		return null;
	}

	/**
	 * Get GELimitTracker and current character from BomGE plugin.
	 * Returns [limitTracker, characterName]
	 */
	private Object[] getLimitTrackerAndCharacter()
	{
		try
		{
			Collection<Plugin> plugins = pluginManager.getPlugins();
			for (Plugin plugin : plugins)
			{
				if (BOMGE_PLUGIN_NAME.equals(plugin.getName()))
				{
					if (!pluginManager.isPluginEnabled(plugin))
					{
						return new Object[]{null, null};
					}

					// Get panel
					Method getPanelMethod = plugin.getClass().getMethod("getPanel");
					BomGETrackerPanel panel = (BomGETrackerPanel) getPanelMethod.invoke(plugin);

					if (panel != null)
					{
						AlchTabPanel alchTab = panel.getAlchTab();
						if (alchTab != null)
						{
							// Get limit tracker via reflection
							java.lang.reflect.Field limitTrackerField = alchTab.getClass().getDeclaredField("limitTracker");
							limitTrackerField.setAccessible(true);
							Object limitTracker = limitTrackerField.get(alchTab);

							// Get current character via reflection
							java.lang.reflect.Field characterField = alchTab.getClass().getDeclaredField("currentCharacter");
							characterField.setAccessible(true);
							String currentCharacter = (String) characterField.get(alchTab);

							return new Object[]{limitTracker, currentCharacter};
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.debug("[BomGE] Could not get limit tracker/character", e);
		}

		return new Object[]{null, null};
	}

	/**
	 * Set overlay reference so we can update it with alch data.
	 */
	public void setOverlay(AlchablesOverlay overlay)
	{
		this.overlay = overlay;
	}

	/**
	 * Convert item ID list to GE search results.
	 */
	private void addItemFilterResults(List<Short> items)
	{
		if (items == null || items.isEmpty())
		{
			setGESearchResults(new short[0]);
			return;
		}

		final short[] itemIds = FilterUtility.getPrimitiveShortArray(items);
		setGESearchResults(itemIds);
	}
}
