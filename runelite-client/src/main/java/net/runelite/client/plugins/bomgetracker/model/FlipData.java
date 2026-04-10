package net.runelite.client.plugins.bomgetracker.model;

import lombok.Data;

@Data
public class FlipData {
    private String id;
    private int itemId;
    private String itemName;
    private String buyTime;
    private int buyPrice;
    private int buyQty;
    private String sellTime;
    private int sellPrice;
    private int sellQty;
    private int holdTime;
    private long profitGross;
    private long profitNet;
    private double roi;
    private String status;
}
