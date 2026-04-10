package net.runelite.client.plugins.bomgetracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.VarClientStr;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.*;

/**
 * Creates clickable price buttons in the GE chatbox for alch-based prices.
 * Based on Flipping Utilities OfferEditor pattern.
 */
@Slf4j
public class AlchPriceWidget
{
	private final Client client;
	private Widget haButton;
	private Widget offsetButton;

	public AlchPriceWidget(Widget parent, Client client)
	{
		this.client = client;

		if (parent == null)
		{
			return;
		}

		// Create two text widgets (buttons) for HA price and offset price
		haButton = parent.createChild(-1, WidgetType.TEXT);
		offsetButton = parent.createChild(-1, WidgetType.TEXT);

		// Position them in the chatbox (bottom left to avoid conflict with Flipping Utilities)
		prepareTextWidget(haButton, WidgetTextAlignment.LEFT, WidgetPositionMode.ABSOLUTE_BOTTOM, 35, 10);
		prepareTextWidget(offsetButton, WidgetTextAlignment.LEFT, WidgetPositionMode.ABSOLUTE_BOTTOM, 20, 10);
	}

	/**
	 * Configure a text widget to be a clickable button.
	 */
	private void prepareTextWidget(Widget widget, int xAlignment, int yMode, int yOffset, int xOffset)
	{
		widget.setTextColor(0x800000); // Dark red
		widget.setFontId(FontID.VERDANA_11_BOLD);
		widget.setYPositionMode(yMode);
		widget.setOriginalX(xOffset);
		widget.setOriginalY(yOffset);
		widget.setOriginalHeight(20);
		widget.setXTextAlignment(xAlignment);
		widget.setWidthMode(WidgetSizeMode.MINUS);
		widget.setHasListener(true);

		// Hover effect: white when hovering, dark red when not
		widget.setOnMouseRepeatListener((JavaScriptCallback) ev -> widget.setTextColor(0xFFFFFF));
		widget.setOnMouseLeaveListener((JavaScriptCallback) ev -> widget.setTextColor(0x800000));
		widget.revalidate();
	}

	/**
	 * Show the alch price buttons with calculated values.
	 */
	public void showAlchPrices(int haPrice, int offsetPrice, String itemName)
	{
		// HA price button
		haButton.setText("set to HA price: " + formatGold(haPrice));
		haButton.setAction(0, "Set price");
		haButton.setOnOpListener((JavaScriptCallback) ev ->
		{
			setPriceInChatbox(haPrice);
		});

		// Offset price button (HA + offset)
		offsetButton.setText("set to HA " + getOffsetSign(haPrice, offsetPrice) + ": " + formatGold(offsetPrice));
		offsetButton.setAction(1, "Set price");
		offsetButton.setOnOpListener((JavaScriptCallback) ev ->
		{
			setPriceInChatbox(offsetPrice);
		});
	}

	/**
	 * Set the price value in the chatbox input.
	 */
	private void setPriceInChatbox(int price)
	{
		try
		{
			// Set display text
			Widget chatboxText = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
			if (chatboxText != null)
			{
				chatboxText.setText(price + "*");
			}

			// Set actual input value
			client.setVarcStrValue(VarClientStr.INPUT_TEXT, String.valueOf(price));
		}
		catch (Exception e)
		{
			log.warn("Failed to set chatbox price: {}", e.getMessage());
		}
	}

	/**
	 * Get the offset sign for display (e.g., "-500" or "+1000").
	 */
	private String getOffsetSign(int haPrice, int offsetPrice)
	{
		int diff = offsetPrice - haPrice;
		if (diff > 0)
		{
			return "+" + formatGold(diff);
		}
		else if (diff < 0)
		{
			return formatGold(diff);
		}
		else
		{
			return "±0";
		}
	}

	/**
	 * Format gold value with commas.
	 */
	private String formatGold(int value)
	{
		return String.format("%,d", value) + " gp";
	}

	/**
	 * Hide the alch price buttons.
	 */
	public void hide()
	{
		if (haButton != null)
		{
			haButton.setHidden(true);
		}
		if (offsetButton != null)
		{
			offsetButton.setHidden(true);
		}
	}
}
