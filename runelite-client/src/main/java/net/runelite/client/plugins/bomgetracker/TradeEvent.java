package net.runelite.client.plugins.bomgetracker;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TradeEvent
{
	private String timestamp;   // "yyyy-MM-dd HH:mm:ss"
	private String state;       // BUYING, BOUGHT, SELLING, SOLD, CANCELLED_BUY, CANCELLED_SELL
	private int slot;           // 0-7
	private int itemId;         // Wiki item ID
	private int qty;            // getQuantitySold()
	private long worth;         // getSpent()
	private int max;            // getTotalQuantity()
	private int offer;          // getPrice()
	private String character;   // Player name
}
