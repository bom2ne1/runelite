package net.runelite.client.plugins.bomgetracker.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.bomgetracker.GELimitTracker;
import net.runelite.client.plugins.bomgetracker.HttpDataClient;
import net.runelite.client.plugins.bomgetracker.model.AlchData;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class AlchTabPanel extends JPanel
{
	private static final int TOP_N = 20; // Show top 20 alch candidates

	private final HttpDataClient dataClient;
	private final GELimitTracker limitTracker;
	private String currentCharacter;
	private List<AlchData> fullAlchList = new ArrayList<>();

	private final JTable table;
	private final DefaultTableModel tableModel;
	private final JLabel statusLabel;
	private final JButton refreshButton;
	private final JComboBox<String> sortComboBox;
	private final JCheckBox f2pOnlyCheckbox;
	private final JTextField minProfitField;
	private final JTextField maxBuyPriceField;
	private final JTextField minVolumeField;
	private final JCheckBox hideAtLimitCheckbox;

	private final String[] columnNames = {
		"Item", "Profit", "HA Val", "Limit", "ROI%", "GP/Hr"
	};

	public AlchTabPanel(HttpDataClient dataClient, GELimitTracker limitTracker)
	{
		log.info("[AlchTabPanel] Constructor called");
		this.dataClient = dataClient;
		this.limitTracker = limitTracker;

		setLayout(new BorderLayout(0, 2));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 5, 5, 5));

		log.info("[AlchTabPanel] Layout and background set");

		// Top controls - three rows
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		controlPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Smaller font for controls
		Font smallFont = FontManager.getRunescapeSmallFont();

		// Row 1: Sort and Refresh
		JPanel row1 = new JPanel(new BorderLayout(3, 0));
		row1.setBackground(ColorScheme.DARK_GRAY_COLOR);

		sortComboBox = new JComboBox<>(new String[]{
			"By Profit",
			"By ROI%",
			"By GP/Hr"
		});
		sortComboBox.setPreferredSize(new Dimension(100, 20));
		sortComboBox.setFont(smallFont);
		sortComboBox.addActionListener(e -> applyFiltersAndSort());

		refreshButton = new JButton("Refresh");
		refreshButton.setFont(smallFont);
		refreshButton.setMargin(new Insets(2, 8, 2, 8));
		refreshButton.addActionListener(e -> refresh());

		JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		leftControls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel sortLabel = new JLabel("Sort:");
		sortLabel.setFont(smallFont);
		sortLabel.setForeground(Color.WHITE);
		leftControls.add(sortLabel);
		leftControls.add(sortComboBox);

		JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		rightControls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		rightControls.add(refreshButton);

		row1.add(leftControls, BorderLayout.WEST);
		row1.add(rightControls, BorderLayout.EAST);

		// Row 2: Checkboxes
		JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		row2.setBackground(ColorScheme.DARK_GRAY_COLOR);

		f2pOnlyCheckbox = new JCheckBox("F2P");
		f2pOnlyCheckbox.setFont(smallFont);
		f2pOnlyCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		f2pOnlyCheckbox.setForeground(Color.WHITE);
		f2pOnlyCheckbox.setToolTipText("Show only Free-to-Play items");
		f2pOnlyCheckbox.addActionListener(e -> applyFiltersAndSort());

		hideAtLimitCheckbox = new JCheckBox("Hide @ Limit");
		hideAtLimitCheckbox.setFont(smallFont);
		hideAtLimitCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		hideAtLimitCheckbox.setForeground(Color.WHITE);
		hideAtLimitCheckbox.setToolTipText("Hide items you've hit the GE limit on");
		hideAtLimitCheckbox.addActionListener(e -> applyFiltersAndSort());

		row2.add(f2pOnlyCheckbox);
		row2.add(hideAtLimitCheckbox);

		// Row 3: Text field filters
		JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		row3.setBackground(ColorScheme.DARK_GRAY_COLOR);

		minProfitField = new JTextField("0");
		minProfitField.setPreferredSize(new Dimension(55, 20));
		minProfitField.setFont(smallFont);
		minProfitField.setToolTipText("Minimum profit per alch");
		minProfitField.setHorizontalAlignment(JTextField.RIGHT);
		minProfitField.addActionListener(e -> applyFiltersAndSort());
		minProfitField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent e) {
				applyFiltersAndSort();
			}
		});

		maxBuyPriceField = new JTextField("999999");
		maxBuyPriceField.setPreferredSize(new Dimension(60, 20));
		maxBuyPriceField.setFont(smallFont);
		maxBuyPriceField.setToolTipText("Maximum buy price (gp)");
		maxBuyPriceField.setHorizontalAlignment(JTextField.RIGHT);
		maxBuyPriceField.addActionListener(e -> applyFiltersAndSort());
		maxBuyPriceField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent e) {
				applyFiltersAndSort();
			}
		});

		minVolumeField = new JTextField("0");
		minVolumeField.setPreferredSize(new Dimension(55, 20));
		minVolumeField.setFont(smallFont);
		minVolumeField.setToolTipText("Minimum 24h volume");
		minVolumeField.setHorizontalAlignment(JTextField.RIGHT);
		minVolumeField.addActionListener(e -> applyFiltersAndSort());
		minVolumeField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent e) {
				applyFiltersAndSort();
			}
		});

		JLabel minLabel = new JLabel("Min:");
		minLabel.setFont(smallFont);
		minLabel.setForeground(Color.WHITE);
		JLabel maxPriceLabel = new JLabel("Max:");
		maxPriceLabel.setFont(smallFont);
		maxPriceLabel.setForeground(Color.WHITE);

		row3.add(minLabel);
		row3.add(minProfitField);
		row3.add(maxPriceLabel);
		row3.add(maxBuyPriceField);

		// Row 4: Volume filter
		JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		row4.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel minVolLabel = new JLabel("Min Vol:");
		minVolLabel.setFont(smallFont);
		minVolLabel.setForeground(Color.WHITE);

		row4.add(minVolLabel);
		row4.add(minVolumeField);

		controlPanel.add(row1);
		controlPanel.add(Box.createVerticalStrut(2));
		controlPanel.add(row2);
		controlPanel.add(Box.createVerticalStrut(2));
		controlPanel.add(row3);
		controlPanel.add(Box.createVerticalStrut(2));
		controlPanel.add(row4);

		// Status label
		statusLabel = new JLabel("Click Refresh to load alch data");
		statusLabel.setFont(smallFont);
		statusLabel.setForeground(Color.LIGHT_GRAY);
		statusLabel.setBorder(new EmptyBorder(0, 0, 2, 0));

		// Table
		tableModel = new DefaultTableModel(columnNames, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};

		table = new JTable(tableModel);
		table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.setForeground(Color.WHITE);
		table.setFont(smallFont);
		table.setRowHeight(16); // Smaller row height for more rows visible
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setFont(smallFont);

		// Column widths - compact to fit panel
		table.getColumnModel().getColumn(0).setPreferredWidth(120); // Item
		table.getColumnModel().getColumn(1).setPreferredWidth(55);  // Profit
		table.getColumnModel().getColumn(2).setPreferredWidth(55);  // HA Val
		table.getColumnModel().getColumn(3).setPreferredWidth(50);  // Limit
		table.getColumnModel().getColumn(4).setPreferredWidth(45);  // ROI%
		table.getColumnModel().getColumn(5).setPreferredWidth(60);  // GP/Hr

		// Right-align numeric columns
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		for (int i = 1; i < columnNames.length; i++)
		{
			table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
		}

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Status + content wrapper
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		centerPanel.add(statusLabel, BorderLayout.NORTH);
		centerPanel.add(scrollPane, BorderLayout.CENTER);

		// Layout
		add(controlPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);

		log.info("[AlchTabPanel] All components added to panel");
		log.info("[AlchTabPanel] Panel size: " + getPreferredSize());
	}

	public void setCharacter(String character)
	{
		this.currentCharacter = character;
	}

	/**
	 * Refresh the limit column display without reloading from server
	 */
	public void refreshLimits()
	{
		SwingUtilities.invokeLater(() -> {
			applyFiltersAndSort();
		});
	}

	public void refresh()
	{
		log.info("[AlchTabPanel] Refresh called");
		statusLabel.setText("Loading alch data...");
		refreshButton.setEnabled(false);

		SwingUtilities.invokeLater(() -> {
			try
			{
				log.info("[AlchTabPanel] Fetching alch profits...");
				fullAlchList = dataClient.fetchAlchProfits();
				log.info("[AlchTabPanel] Received {} items", fullAlchList != null ? fullAlchList.size() : 0);

				if (fullAlchList == null || fullAlchList.isEmpty())
				{
					log.warn("[AlchTabPanel] No alch data received");
					statusLabel.setText("Server offline or no data available");
					refreshButton.setEnabled(true);
					return;
				}

				applyFiltersAndSort();
				refreshButton.setEnabled(true);
			}
			catch (Exception e)
			{
				log.error("Failed to refresh alch data", e);
				statusLabel.setText("Error loading data: " + e.getMessage());
				refreshButton.setEnabled(true);
			}
		});
	}

	private void applyFiltersAndSort()
	{
		if (fullAlchList == null || fullAlchList.isEmpty())
		{
			return;
		}

		// Parse filter values
		boolean f2pOnly = f2pOnlyCheckbox.isSelected();
		boolean hideAtLimit = hideAtLimitCheckbox.isSelected();

		int minProfit = 0;
		try {
			minProfit = Integer.parseInt(minProfitField.getText().trim());
		} catch (NumberFormatException e) {
			minProfit = 0;
		}

		int maxBuyPrice = Integer.MAX_VALUE;
		try {
			maxBuyPrice = Integer.parseInt(maxBuyPriceField.getText().trim());
		} catch (NumberFormatException e) {
			maxBuyPrice = Integer.MAX_VALUE;
		}

		int minVolume = 0;
		try {
			minVolume = Integer.parseInt(minVolumeField.getText().trim());
		} catch (NumberFormatException e) {
			minVolume = 0;
		}

		List<AlchData> filtered = new ArrayList<>();
		for (AlchData alch : fullAlchList)
		{
			// F2P filter
			if (f2pOnly && alch.isMembers())
			{
				continue;
			}

			// Min profit filter
			if (alch.getProfit() < minProfit)
			{
				continue;
			}

			// Max buy price filter
			if (alch.getBuyPrice() > maxBuyPrice)
			{
				continue;
			}

			// Min volume filter
			if (alch.getVolume24h() < minVolume)
			{
				continue;
			}

			// Hide at limit filter
			if (hideAtLimit)
			{
				int purchased = limitTracker.getPurchased(currentCharacter, alch.getId());
				Integer limit = alch.getLimit();
				if (limit != null && limit > 0 && purchased >= limit)
				{
					continue; // Skip items we've hit the limit on
				}
			}

			filtered.add(alch);
		}

		// Sort based on selection
		String sortMode = (String) sortComboBox.getSelectedItem();
		if ("By ROI%".equals(sortMode))
		{
			filtered.sort((a, b) -> Double.compare(b.getRoi(), a.getRoi()));
		}
		else if ("By GP/Hr".equals(sortMode))
		{
			filtered.sort((a, b) -> Integer.compare(b.getProfitPerHour(), a.getProfitPerHour()));
		}
		else
		{
			// Default: By Profit
			filtered.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));
		}

		// Take top N
		List<AlchData> topItems = filtered.subList(0, Math.min(TOP_N, filtered.size()));

		// Update table
		tableModel.setRowCount(0);
		for (AlchData alch : topItems)
		{
			int purchased = limitTracker.getPurchased(currentCharacter, alch.getId());
			Integer limit = alch.getLimit();

			// Debug logging for Rune kiteshield specifically
			if (alch.getName().contains("Rune kite"))
			{
				log.info("[AlchTabPanel] {} - purchased: {}, limit: {}, character: {}",
					alch.getName(), purchased, limit, currentCharacter);
			}

			String limitStr;
			if (limit != null && limit > 0)
			{
				limitStr = purchased + "/" + limit;
			}
			else
			{
				limitStr = purchased > 0 ? String.valueOf(purchased) : "-";
			}

			tableModel.addRow(new Object[]{
				alch.getName(),
				formatGP(alch.getProfit()),
				formatGP(alch.getHighAlch()),
				limitStr,
				String.format("%.1f%%", alch.getRoi()),
				formatGP(alch.getProfitPerHour())
			});
		}

		statusLabel.setText(String.format("Showing top %d of %d alch opportunities", topItems.size(), filtered.size()));
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
	 * Get current filter settings for use by GE search filter.
	 */
	public FilterSettings getFilterSettings()
	{
		FilterSettings settings = new FilterSettings();
		settings.f2pOnly = f2pOnlyCheckbox.isSelected();
		settings.hideAtLimit = hideAtLimitCheckbox.isSelected();

		try {
			settings.minProfit = Integer.parseInt(minProfitField.getText().trim());
		} catch (NumberFormatException e) {
			settings.minProfit = 0;
		}

		try {
			settings.maxBuyPrice = Integer.parseInt(maxBuyPriceField.getText().trim());
		} catch (NumberFormatException e) {
			settings.maxBuyPrice = Integer.MAX_VALUE;
		}

		try {
			settings.minVolume = Integer.parseInt(minVolumeField.getText().trim());
		} catch (NumberFormatException e) {
			settings.minVolume = 0;
		}

		settings.sortMode = (String) sortComboBox.getSelectedItem();
		return settings;
	}

	/**
	 * Get the full (unfiltered) alch data list.
	 */
	public List<AlchData> getFullAlchList()
	{
		return fullAlchList;
	}

	/**
	 * Filter settings data class.
	 */
	public static class FilterSettings
	{
		public boolean f2pOnly;
		public boolean hideAtLimit;
		public int minProfit;
		public int maxBuyPrice;
		public int minVolume;
		public String sortMode;
	}
}
