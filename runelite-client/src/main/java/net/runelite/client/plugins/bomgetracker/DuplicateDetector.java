package net.runelite.client.plugins.bomgetracker;

import net.runelite.api.GrandExchangeOfferState;

/**
 * Tracks per-slot GE offer state to filter out duplicate events.
 * RuneLite fires onGrandExchangeOfferChanged multiple times for the same
 * logical state transition — this class ensures we only emit once.
 */
public class DuplicateDetector
{
	private static final int NUM_SLOTS = 8;

	private final int[] prevQuantity = new int[NUM_SLOTS];
	private final GrandExchangeOfferState[] prevState = new GrandExchangeOfferState[NUM_SLOTS];

	public DuplicateDetector()
	{
		reset();
	}

	/**
	 * Returns true if this event should be skipped (it's a duplicate).
	 */
	public boolean isDuplicate(int slot, GrandExchangeOfferState state, int quantitySold)
	{
		if (slot < 0 || slot >= NUM_SLOTS)
		{
			return true;
		}

		if (state == GrandExchangeOfferState.EMPTY)
		{
			// Reset tracking for this slot
			prevQuantity[slot] = 0;
			prevState[slot] = null;
			return true; // We don't report EMPTY events
		}

		boolean isDup;

		switch (state)
		{
			case BUYING:
			case SELLING:
				// In-progress states: duplicate if qty hasn't changed
				isDup = (state == prevState[slot] && quantitySold == prevQuantity[slot]);
				break;

			case BOUGHT:
			case SOLD:
				// Completion states: duplicate if same state fires again with same qty
				isDup = (state == prevState[slot] && quantitySold == prevQuantity[slot]);
				break;

			case CANCELLED_BUY:
			case CANCELLED_SELL:
				// Cancel states: duplicate if same cancel fires again
				isDup = (state == prevState[slot]);
				break;

			default:
				isDup = true;
				break;
		}

		// Update tracking
		prevState[slot] = state;
		prevQuantity[slot] = quantitySold;

		return isDup;
	}

	public void reset()
	{
		for (int i = 0; i < NUM_SLOTS; i++)
		{
			prevQuantity[i] = 0;
			prevState[i] = null;
		}
	}
}
