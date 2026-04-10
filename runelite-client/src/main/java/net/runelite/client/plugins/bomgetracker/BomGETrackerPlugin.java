package net.runelite.client.plugins.bomgetracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.bomgetracker.ui.BomGETrackerPanel;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "BomGE Tracker",
	description = "Sends Grand Exchange trade events to BomGE server via HTTP",
	tags = {"grand exchange", "trading", "tracker"},
	enabledByDefault = true
)
public class BomGETrackerPlugin extends Plugin
{
	private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Inject
	private Client client;

	@Inject
	private BomGETrackerConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ItemDataCache itemCache;

	@Inject
	private GEInputListener inputListener;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AlchablesOverlay alchablesOverlay;

	private TradeEventSender sender;
	private FallbackFileLogger fallbackLogger;
	private DuplicateDetector dedup;
	private HttpDataClient dataClient;
	private GELimitTracker limitTracker;
	private NavigationButton navButton;
	private BomGETrackerPanel panel;
	private int highlightedItemId = -1;
	private boolean chatboxOpen = false;

	@Provides
	BomGETrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BomGETrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		fallbackLogger = new FallbackFileLogger();
		sender = new TradeEventSender(fallbackLogger);
		dedup = new DuplicateDetector();
		limitTracker = new GELimitTracker();

		// Register hotkey listener
		keyManager.registerKeyListener(inputListener);

		updateSenderConfig();

		// Initialize data client and panel
		dataClient = new HttpDataClient(sender.getHttpClient(), config.serverUrl());
		panel = new BomGETrackerPanel(this, dataClient, limitTracker);

		// Create navigation button
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/net/runelite/client/plugins/bomgetracker/grand_exchange_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("BomGE Tracker")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		// Register overlay
		overlayManager.add(alchablesOverlay);

		// Load purchase history from server (async)
		loadPurchaseHistory();

		log.info("BomGE Tracker started");
	}

	/**
	 * Load purchase history from server to populate limit tracker.
	 * This runs once on startup to restore limit tracking from previous session.
	 */
	private void loadPurchaseHistory()
	{
		new Thread(() -> {
			try
			{
				log.info("[BomGE] Loading purchase history from server...");
				// Fetch last 500 trades (covers several hours of trading)
				List<net.runelite.client.plugins.bomgetracker.model.TradeData> trades = dataClient.fetchTrades(500);

				if (trades != null && !trades.isEmpty())
				{
					limitTracker.loadFromTrades(trades);
					log.info("[BomGE] Purchase history loaded successfully");

					// Refresh panel limits if panel exists
					if (panel != null)
					{
						panel.refreshAlchTabLimits();
					}
				}
				else
				{
					log.info("[BomGE] No trade history available from server");
				}
			}
			catch (Exception e)
			{
				log.error("[BomGE] Failed to load purchase history", e);
			}
		}).start();
	}

	@Override
	protected void shutDown()
	{
		// Unregister hotkey listener
		keyManager.unregisterKeyListener(inputListener);

		// Remove navigation button
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}

		// Stop panel timers
		if (panel != null)
		{
			panel.onRemove();
		}

		// Remove overlay
		overlayManager.remove(alchablesOverlay);

		if (sender != null)
		{
			sender.shutdown();
			sender = null;
		}
		panel = null;
		dataClient = null;
		dedup = null;
		fallbackLogger = null;
		log.info("BomGE Tracker stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"bomgetracker".equals(event.getGroup()))
		{
			return;
		}
		updateSenderConfig();
	}

	private void updateSenderConfig()
	{
		if (sender != null)
		{
			sender.configure(config.serverUrl(), config.apiKey(), config.alsoLogToFile());
		}

		// Update data client URL if it exists
		if (dataClient != null)
		{
			dataClient = new HttpDataClient(sender.getHttpClient(), config.serverUrl());
			if (panel != null)
			{
				panel.updateServerStatus();
			}
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		// Must be logged in
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		int slot = event.getSlot();
		GrandExchangeOffer offer = event.getOffer();
		GrandExchangeOfferState state = offer.getState();

		// Track highlighted item for hotkey feature
		if (state != GrandExchangeOfferState.EMPTY && offer.getItemId() > 0)
		{
			highlightedItemId = offer.getItemId();
		}

		// Skip empty slots
		if (state == GrandExchangeOfferState.EMPTY)
		{
			dedup.isDuplicate(slot, state, 0); // Reset slot tracking
			return;
		}

		int quantitySold = offer.getQuantitySold();

		// Dedup check
		if (dedup.isDuplicate(slot, state, quantitySold))
		{
			return;
		}

		// Get character name
		String character = null;
		if (client.getLocalPlayer() != null)
		{
			character = client.getLocalPlayer().getName();
		}

		// Build trade event
		TradeEvent tradeEvent = TradeEvent.builder()
			.timestamp(LocalDateTime.now().format(TIMESTAMP_FMT))
			.state(state.name())
			.slot(slot)
			.itemId(offer.getItemId())
			.qty(quantitySold)
			.worth(offer.getSpent())
			.max(offer.getTotalQuantity())
			.offer(offer.getPrice())
			.character(character)
			.build();

		log.debug("GE event: {} slot={} item={} qty={}/{}",
			state, slot, offer.getItemId(), quantitySold, offer.getTotalQuantity());

		// Send async (off game thread)
		sender.send(tradeEvent);

		// Track purchases for GE limit (only for BOUGHT state)
		if (state == GrandExchangeOfferState.BOUGHT && character != null)
		{
			limitTracker.recordPurchase(character, offer.getItemId(), quantitySold);
			log.info("[BomGE] Recorded purchase: {} bought {} x{} (itemId: {})",
				character, offer.getItemId(), quantitySold, offer.getItemId());

			// Update panel character and refresh alch tab limits
			if (panel != null)
			{
				panel.setCurrentCharacter(character);
				panel.refreshAlchTabLimits();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// Update panel with current character
			if (panel != null && client.getLocalPlayer() != null)
			{
				String characterName = client.getLocalPlayer().getName();
				panel.setCurrentCharacter(characterName);
				log.info("[BomGE] Character logged in: {}", characterName);
			}
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// Check if GE price/quantity chatbox opened
		// This fires when the chatbox appears for price/quantity input
		if (client.getVarcIntValue(VarClientInt.INPUT_TYPE) == 7)
		{
			if (!chatboxOpen && config.alchHotkeyEnabled())
			{
				chatboxOpen = true;
				clientThread.invokeLater(this::showAlchPriceButtons);
			}
		}
		else
		{
			chatboxOpen = false;
		}
	}

	/**
	 * Show the alch price buttons in the chatbox.
	 */
	private void showAlchPriceButtons()
	{
		try
		{
			// Get the chatbox parent widget
			Widget chatboxParent = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
			if (chatboxParent == null)
			{
				return;
			}

			// Get the current item
			int itemId = getHighlightedItemId();
			if (itemId <= 0)
			{
				return;
			}

			// Calculate prices
			int haPrice = itemCache.getHighAlchPrice(itemId);
			if (haPrice == 0)
			{
				return; // Not alchable
			}

			int offsetPrice = Math.max(1, haPrice + config.alchHotkeyOffset());
			String itemName = itemCache.getItemName(itemId);

			// Create and show the widget
			AlchPriceWidget alchWidget = new AlchPriceWidget(chatboxParent, client);
			alchWidget.showAlchPrices(haPrice, offsetPrice, itemName);

			log.debug("Showing alch price buttons for {}: HA={}, Offset={}", itemName, haPrice, offsetPrice);
		}
		catch (Exception e)
		{
			log.warn("Failed to show alch price buttons", e);
		}
	}

	/**
	 * Get the currently highlighted/selected GE item ID.
	 * Used by the hotkey listener to determine which item to calculate price for.
	 * Uses VarPlayerID.TRADINGPOST_SEARCH like Flipping Utilities does.
	 */
	public int getHighlightedItemId()
	{
		// Get the item ID from the GE search varplayer (more reliable than tracking offer changes)
		int itemId = client.getVarpValue(VarPlayerID.TRADINGPOST_SEARCH);
		if (itemId > 0)
		{
			return itemId;
		}
		// Fallback to tracked value
		return highlightedItemId;
	}

	/**
	 * Get the HTTP data client for accessing server API.
	 * Used by GE Filters integration to fetch alch data.
	 */
	public HttpDataClient getHttpDataClient()
	{
		return dataClient;
	}

	/**
	 * Get the plugin config.
	 * Used by GE Filters integration for filtering/sorting.
	 */
	public BomGETrackerConfig getConfig()
	{
		return config;
	}

	/**
	 * Get the alchables overlay.
	 * Used by GE Filters integration to update overlay data.
	 */
	public AlchablesOverlay getAlchablesOverlay()
	{
		return alchablesOverlay;
	}

	/**
	 * Get the plugin panel.
	 * Used by GE Filters to access filter settings.
	 */
	public BomGETrackerPanel getPanel()
	{
		return panel;
	}
}
