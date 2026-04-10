package net.runelite.client.plugins.bomgetracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarClientInt;
import net.runelite.api.VarClientStr;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.input.KeyListener;

import javax.inject.Inject;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Handles the alch price hotkey. When pressed while GE price chatbox is open,
 * calculates HA price + offset and injects it into the chatbox input.
 *
 * Based on Flipping Utilities plugin approach.
 */
@Slf4j
public class GEInputListener implements KeyListener
{
	@Inject
	private Client client;

	@Inject
	private BomGETrackerConfig config;

	@Inject
	private ItemDataCache itemCache;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BomGETrackerPlugin plugin;

	@Override
	public void keyTyped(KeyEvent e)
	{
		// Not used
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!config.alchHotkeyEnabled())
		{
			return;
		}

		// Check if hotkey matches (this is safe on AWT thread)
		if (!matchesHotkey(e))
		{
			return;
		}

		// Schedule the actual work on the client thread (like Flipping Utilities does)
		clientThread.invoke(() ->
		{
			try
			{
				if (client.getGameState() != GameState.LOGGED_IN)
				{
					return;
				}

				// Check if price/quantity chatbox is open
				if (!isPriceChatboxOpen())
				{
					return;
				}

				// Get the item ID from the currently highlighted GE offer
				int itemId = plugin.getHighlightedItemId();
				if (itemId <= 0)
				{
					log.debug("No highlighted item found");
					return;
				}

				// Calculate HA price (offline capability)
				int haPrice = itemCache.getHighAlchPrice(itemId);
				if (haPrice == 0)
				{
					sendChatMessage("Item is not alchable");
					return;
				}

				// Apply user-configured offset
				int targetPrice = Math.max(1, haPrice + config.alchHotkeyOffset());
				String itemName = itemCache.getItemName(itemId);

				// Inject price into chatbox (Flipping Utilities approach)
				boolean injected = setChatboxInput(targetPrice);

				// Show result in chat
				String message = String.format("%s → %s (HA: %s, Offset: %+d)",
					itemName,
					formatGold(targetPrice),
					formatGold(haPrice),
					config.alchHotkeyOffset());

				if (injected)
				{
					message += " [Price set!]";
				}
				else
				{
					message += " [Failed to set]";
					// Copy to clipboard as fallback
					if (config.copyToClipboard())
					{
						copyToClipboard(String.valueOf(targetPrice));
						sendChatMessage("Price copied to clipboard!");
					}
				}

				sendChatMessage(message);
				e.consume();
			}
			catch (Exception ex)
			{
				log.warn("Exception during alch hotkey press", ex);
			}
		});
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		// Not used
	}

	/**
	 * Check if the price/quantity chatbox is currently open.
	 * Based on Flipping Utilities detection logic.
	 */
	private boolean isPriceChatboxOpen()
	{
		// VarClientInt.INPUT_TYPE == 7 means chatbox number input is active (for GE price/quantity)
		return client.getVarcIntValue(VarClientInt.INPUT_TYPE) == 7;
	}

	/**
	 * Set the chatbox input value (for GE price entry).
	 * Based on Flipping Utilities approach.
	 */
	private boolean setChatboxInput(int value)
	{
		try
		{
			// Set the chatbox display text (what the player sees)
			// Using InterfaceID.Chatbox constants like Flipping Utilities
			Widget chatboxText = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
			if (chatboxText != null)
			{
				chatboxText.setText(value + "*");
			}

			// Set the actual input value
			client.setVarcStrValue(VarClientStr.INPUT_TEXT, String.valueOf(value));

			log.debug("Set chatbox input to: {}", value);
			return true;
		}
		catch (Exception e)
		{
			log.warn("Failed to set chatbox input: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Check if the key event matches the configured hotkey.
	 */
	private boolean matchesHotkey(KeyEvent e)
	{
		String hotkeyStr = config.alchHotkey().toUpperCase();

		// Handle single character hotkeys
		if (hotkeyStr.length() == 1)
		{
			char hotkeyChar = hotkeyStr.charAt(0);
			char pressedChar = Character.toUpperCase(e.getKeyChar());
			return hotkeyChar == pressedChar;
		}

		// For multi-character, use getKeyText
		String keyPressed = KeyEvent.getKeyText(e.getKeyCode()).toLowerCase();
		return hotkeyStr.equalsIgnoreCase(keyPressed);
	}

	/**
	 * Send a message to the game chatbox.
	 */
	private void sendChatMessage(String message)
	{
		try
		{
			String formattedMessage = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append("[BomGE] ")
				.append(ChatColorType.NORMAL)
				.append(message)
				.build();

			chatMessageManager.queue(QueuedMessage.builder()
				.type(net.runelite.api.ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(formattedMessage)
				.build());
		}
		catch (Exception e)
		{
			log.warn("Failed to send chat message: {}", e.getMessage());
		}
	}

	/**
	 * Format gold value with commas (e.g., 1,234,567).
	 */
	private String formatGold(int value)
	{
		return NumberFormat.getNumberInstance(Locale.US).format(value) + " GP";
	}

	/**
	 * Copy text to system clipboard.
	 */
	private void copyToClipboard(String text)
	{
		try
		{
			Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(new StringSelection(text), null);
		}
		catch (Exception e)
		{
			log.warn("Failed to copy to clipboard: {}", e.getMessage());
		}
	}
}
