package net.runelite.client.plugins.bomgetracker.model;

import lombok.Data;

@Data
public class AlchData
{
	private int id;
	private String name;
	private String icon;
	private int buyPrice;
	private int highAlch;
	private int lowAlch;
	private int value;
	private int naturePrice;
	private int profit;
	private double roi;
	private int profitPerMin;
	private int profitPerHour;
	private Integer limit;
	private boolean members;
	private String examine;
	private Long buyTime;
	private int volume24h;
	private Integer maxProfit;
	private boolean volCapped;
}
