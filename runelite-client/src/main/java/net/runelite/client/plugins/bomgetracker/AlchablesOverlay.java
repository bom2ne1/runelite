package net.runelite.client.plugins.bomgetracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.plugins.bomgetracker.model.AlchData;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Overlay that displays alch profit info on GE search results.
 */
@Slf4j
public class AlchablesOverlay extends WidgetItemOverlay
{
	private final Client client;
	private final BomGETrackerConfig config;
	private final GELimitTracker limitTracker;

	// Cache alch data by item ID
	private Map<Integer, AlchData> alchDataCache = new HashMap<>();
	private String currentCharacter;

	@Inject
	public AlchablesOverlay(Client client, BomGETrackerConfig config, GELimitTracker limitTracker)
	{
		this.client = client;
		this.config = config;
		this.limitTracker = limitTracker;

		// Show overlay on GE search results (chatbox message layer)
		showOnInterfaces(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		// Disabled until overlay is working properly
		// TODO: Implement tooltips instead of overlay
		return;

		// AlchData alchData = alchDataCache.get(itemId);
		// if (alchData == null)
		// {
		// 	return; // No alch data for this item
		// }

		// graphics.setFont(FontManager.getRunescapeSmallFont());
		// Rectangle bounds = widgetItem.getCanvasBounds();

		// // Get current character for limit tracking
		// if (client.getLocalPlayer() != null)
		// {
		// 	currentCharacter = client.getLocalPlayer().getName();
		// }

		// // Render overlay text
		// int yOffset = bounds.y + 10; // Start from top

		// String profitText = formatGP(alchData.getProfit());
		// Color color = alchData.getProfit() > 0 ? Color.GREEN : Color.RED;
		// renderText(graphics, bounds.x + 2, yOffset, profitText, color);
		// yOffset += 12;

		// String haText = "HA:" + formatGP(alchData.getHighAlch());
		// renderText(graphics, bounds.x + 2, yOffset, haText, Color.YELLOW);
		// yOffset += 12;

		// if (alchData.getLimit() != null && alchData.getLimit() > 0)
		// {
		// 	int purchased = limitTracker.getPurchased(currentCharacter, alchData.getId());
		// 	String limitText = purchased + "/" + alchData.getLimit();
		// 	Color limitColor = purchased >= alchData.getLimit() ? Color.RED : Color.WHITE;
		// 	renderText(graphics, bounds.x + 2, yOffset, limitText, limitColor);
		// }
	}

	private void renderText(Graphics2D graphics, int x, int y, String text, Color color)
	{
		final TextComponent textComponent = new TextComponent();
		textComponent.setPosition(new Point(x, y));
		textComponent.setColor(color);
		textComponent.setText(text);
		textComponent.render(graphics);
	}

	private String formatGP(int amount)
	{
		if (amount >= 1_000_000)
		{
			return String.format("%.1fM", amount / 1_000_000.0);
		}
		else if (amount >= 1_000)
		{
			return String.format("%.1fK", amount / 1_000.0);
		}
		else
		{
			return String.valueOf(amount);
		}
	}

	/**
	 * Update the alch data cache from server data.
	 * Called by AlchablesSearchFilter when data is fetched.
	 */
	public void updateAlchData(List<AlchData> alchList)
	{
		alchDataCache.clear();
		if (alchList != null)
		{
			for (AlchData alch : alchList)
			{
				alchDataCache.put(alch.getId(), alch);
			}
			log.debug("[BomGE Overlay] Updated cache with {} alch items", alchDataCache.size());
		}
	}
}
