package net.runelite.client.plugins.bomgetracker;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.bomgetracker.model.TradeData;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks purchased quantities for GE limits (4-hour rolling window)
 * Similar to Flipping Utilities limit tracking
 */
@Slf4j
public class GELimitTracker
{
	private static final long LIMIT_RESET_DURATION_MS = 4 * 60 * 60 * 1000; // 4 hours

	// character -> itemId -> LimitEntry
	private final Map<String, Map<Integer, LimitEntry>> limitData = new HashMap<>();

	@Data
	public static class LimitEntry
	{
		private int itemId;
		private int purchased;
		private long firstPurchaseTime;

		public LimitEntry(int itemId)
		{
			this.itemId = itemId;
			this.purchased = 0;
			this.firstPurchaseTime = 0;
		}

		/**
		 * Check if this entry has expired (4 hours passed)
		 */
		public boolean isExpired()
		{
			if (firstPurchaseTime == 0)
			{
				return false;
			}
			long now = Instant.now().toEpochMilli();
			return (now - firstPurchaseTime) > LIMIT_RESET_DURATION_MS;
		}

		/**
		 * Get remaining time in seconds until reset
		 */
		public long getRemainingSeconds()
		{
			if (firstPurchaseTime == 0)
			{
				return 0;
			}
			long now = Instant.now().toEpochMilli();
			long elapsed = now - firstPurchaseTime;
			long remaining = LIMIT_RESET_DURATION_MS - elapsed;
			return Math.max(0, remaining / 1000);
		}
	}

	/**
	 * Record a purchase (from BOUGHT trade event)
	 */
	public void recordPurchase(String character, int itemId, int quantity)
	{
		if (character == null || character.isEmpty())
		{
			return;
		}

		Map<Integer, LimitEntry> charData = limitData.computeIfAbsent(character, k -> new HashMap<>());
		LimitEntry entry = charData.computeIfAbsent(itemId, LimitEntry::new);

		// Check if expired, reset if so
		if (entry.isExpired())
		{
			entry.setPurchased(0);
			entry.setFirstPurchaseTime(0);
		}

		// Record purchase
		if (entry.getFirstPurchaseTime() == 0)
		{
			entry.setFirstPurchaseTime(Instant.now().toEpochMilli());
		}
		entry.setPurchased(entry.getPurchased() + quantity);

		log.debug("Recorded purchase: {} bought {} x{} (total: {})",
			character, itemId, quantity, entry.getPurchased());
	}

	/**
	 * Get purchased amount for an item
	 */
	public int getPurchased(String character, int itemId)
	{
		if (character == null || character.isEmpty())
		{
			return 0;
		}

		Map<Integer, LimitEntry> charData = limitData.get(character);
		if (charData == null)
		{
			return 0;
		}

		LimitEntry entry = charData.get(itemId);
		if (entry == null)
		{
			return 0;
		}

		// Check expiry
		if (entry.isExpired())
		{
			entry.setPurchased(0);
			entry.setFirstPurchaseTime(0);
			return 0;
		}

		return entry.getPurchased();
	}

	/**
	 * Get limit entry for an item (includes timer info)
	 */
	public LimitEntry getEntry(String character, int itemId)
	{
		if (character == null || character.isEmpty())
		{
			return new LimitEntry(itemId);
		}

		Map<Integer, LimitEntry> charData = limitData.get(character);
		if (charData == null)
		{
			return new LimitEntry(itemId);
		}

		LimitEntry entry = charData.get(itemId);
		if (entry == null)
		{
			return new LimitEntry(itemId);
		}

		// Check expiry
		if (entry.isExpired())
		{
			entry.setPurchased(0);
			entry.setFirstPurchaseTime(0);
		}

		return entry;
	}

	/**
	 * Clear all data for a character
	 */
	public void clearCharacter(String character)
	{
		if (character != null)
		{
			limitData.remove(character);
		}
	}

	/**
	 * Clear all tracked data
	 */
	public void clear()
	{
		limitData.clear();
	}

	/**
	 * Load purchase history from trade data.
	 * Only loads purchases within the last 4 hours.
	 */
	public void loadFromTrades(List<TradeData> trades)
	{
		if (trades == null || trades.isEmpty())
		{
			log.info("[GELimitTracker] No trades to load");
			return;
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		long now = Instant.now().toEpochMilli();
		int loadedCount = 0;

		for (TradeData trade : trades)
		{
			// Only load BOUGHT trades
			if (!"BOUGHT".equals(trade.getState()) && !"BUYING".equals(trade.getState()))
			{
				continue;
			}

			// Parse timestamp
			try
			{
				LocalDateTime tradeTime = LocalDateTime.parse(trade.getTimestamp(), formatter);
				long tradeMillis = tradeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

				// Skip trades older than 4 hours
				if ((now - tradeMillis) > LIMIT_RESET_DURATION_MS)
				{
					continue;
				}

				// Record the purchase
				String character = trade.getCharacter();
				if (character == null || character.isEmpty())
				{
					continue;
				}

				Map<Integer, LimitEntry> charData = limitData.computeIfAbsent(character, k -> new HashMap<>());
				LimitEntry entry = charData.computeIfAbsent(trade.getItemId(), LimitEntry::new);

				// If this is the first purchase, set the timestamp
				if (entry.getFirstPurchaseTime() == 0)
				{
					entry.setFirstPurchaseTime(tradeMillis);
				}
				else
				{
					// Use the earliest timestamp
					entry.setFirstPurchaseTime(Math.min(entry.getFirstPurchaseTime(), tradeMillis));
				}

				// Add to purchased count
				entry.setPurchased(entry.getPurchased() + trade.getQty());
				loadedCount++;
			}
			catch (Exception e)
			{
				log.debug("[GELimitTracker] Failed to parse trade timestamp: {}", trade.getTimestamp());
			}
		}

		log.info("[GELimitTracker] Loaded {} purchases from {} trades", loadedCount, trades.size());
	}
}
