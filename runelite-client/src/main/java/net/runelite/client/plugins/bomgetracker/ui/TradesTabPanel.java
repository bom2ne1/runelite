package net.runelite.client.plugins.bomgetracker.ui;

import net.runelite.client.plugins.bomgetracker.HttpDataClient;
import net.runelite.client.plugins.bomgetracker.model.TradeData;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TradesTabPanel extends JPanel {
    private final HttpDataClient dataClient;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final Timer autoRefreshTimer;
    private String currentCharacter;
    private static final int AUTO_REFRESH_INTERVAL_MS = 5000; // 5 seconds

    public TradesTabPanel(HttpDataClient dataClient) {
        this.dataClient = dataClient;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Status label
        statusLabel = new JLabel("Loading...");
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"Time", "Item", "Type", "Qty", "Price", "Total"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setFont(FontManager.getRunescapeSmallFont());
        table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        table.setForeground(Color.WHITE);
        table.setRowHeight(20);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);

        // Custom renderer for currency columns
        DefaultTableCellRenderer currencyRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Long) {
                    setText(QuantityFormatter.quantityToStackSize((Long) value) + " gp");
                } else {
                    super.setValue(value);
                }
            }
        };
        currencyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(currencyRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(currencyRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(scrollPane, BorderLayout.CENTER);

        // Refresh button and auto-refresh toggle
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());

        JCheckBox autoRefreshCheckbox = new JCheckBox("Auto-refresh", true);
        autoRefreshCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        autoRefreshCheckbox.setForeground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.add(refreshButton);
        buttonPanel.add(autoRefreshCheckbox);
        add(buttonPanel, BorderLayout.SOUTH);

        // Auto-refresh timer (5 second interval)
        autoRefreshTimer = new Timer(AUTO_REFRESH_INTERVAL_MS, e -> {
            if (autoRefreshCheckbox.isSelected()) {
                refresh();
            }
        });
        autoRefreshTimer.start();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
    }

    public void setCurrentCharacter(String character) {
        this.currentCharacter = character;
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Loading trades..."));

        new Thread(() -> {
            List<TradeData> allTrades = dataClient.fetchTrades(100);

            // Filter trades by current character if set
            List<TradeData> trades = allTrades;
            if (currentCharacter != null && !currentCharacter.isEmpty()) {
                trades = allTrades.stream()
                    .filter(t -> currentCharacter.equals(t.getCharacter()))
                    .collect(Collectors.toList());
            }

            final List<TradeData> filteredTrades = trades;

            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);

                if (filteredTrades.isEmpty()) {
                    if (currentCharacter != null) {
                        statusLabel.setText("No trades found for " + currentCharacter);
                    } else {
                        statusLabel.setText("No trades found (server may be offline)");
                    }
                } else {
                    if (currentCharacter != null) {
                        statusLabel.setText("Last " + filteredTrades.size() + " trades for " + currentCharacter);
                    } else {
                        statusLabel.setText("Last " + filteredTrades.size() + " trades");
                    }

                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss");

                    for (TradeData trade : filteredTrades) {
                        try {
                            // Parse timestamp (format: "2026-04-09 13:31:55")
                            String timeStr;
                            try {
                                Date date = inputFormat.parse(trade.getTimestamp());
                                timeStr = outputFormat.format(date);
                            } catch (ParseException e) {
                                // Fallback: just show the timestamp as-is
                                timeStr = trade.getTimestamp();
                            }

                            // Map state to display type
                            String type;
                            String state = trade.getState().toUpperCase();
                            if (state.contains("BUY")) {
                                type = "Buy";
                            } else if (state.contains("SELL")) {
                                type = "Sell";
                            } else {
                                type = state;
                            }

                            // Calculate total (handle null actualPrice)
                            long actualPrice = trade.getActualPrice() != null ? trade.getActualPrice() : 0;
                            long total = actualPrice * trade.getQty();

                            tableModel.addRow(new Object[]{
                                timeStr,
                                trade.getItemName(),
                                type,
                                trade.getQty(),
                                actualPrice,
                                total
                            });
                        } catch (Exception e) {
                            // Skip malformed trades
                        }
                    }
                }
            });
        }).start();
    }
}
