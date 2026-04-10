package net.runelite.client.plugins.bomgetracker.model;

import lombok.Data;

@Data
public class TradeData {
    private String timestamp;
    private String state;
    private int slot;
    private int itemId;
    private String itemName;
    private int qty;
    private long worth;
    private int offer;
    private String character;
    private Integer actualPrice; // Can be null for pending trades
}
