package net.runelite.client.plugins.bomgetracker.model;

import lombok.Data;

@Data
public class FlipStats {
    private int totalFlips;
    private double successRate;
    private long totalProfit;
    private double avgProfit;
    private double avgRoi;
    private int avgHoldTime;
}
