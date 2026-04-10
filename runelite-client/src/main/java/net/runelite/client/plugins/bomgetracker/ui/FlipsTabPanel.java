package net.runelite.client.plugins.bomgetracker.ui;

import net.runelite.client.plugins.bomgetracker.HttpDataClient;
import net.runelite.client.plugins.bomgetracker.model.FlipData;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class FlipsTabPanel extends JPanel {
    private final HttpDataClient dataClient;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JComboBox<String> filterCombo;
    private String currentCharacter;

    public FlipsTabPanel(HttpDataClient dataClient) {
        this.dataClient = dataClient;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Top panel with status and filter
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        statusLabel = new JLabel("Loading...");
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(statusLabel, BorderLayout.NORTH);

        // Filter dropdown
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        filterPanel.add(new JLabel("Show:"));
        filterCombo = new JComboBox<>(new String[]{"Completed", "Pending", "All"});
        filterCombo.addActionListener(e -> refresh());
        filterPanel.add(filterCombo);
        topPanel.add(filterPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"Item", "Buy Price", "Sell Price", "Profit", "ROI %"};
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
        table.getColumnModel().getColumn(1).setCellRenderer(currencyRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(currencyRenderer);

        // Profit column with color coding
        DefaultTableCellRenderer profitRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value instanceof Long) {
                    long profit = (Long) value;
                    setText(QuantityFormatter.quantityToStackSize(profit) + " gp");
                    setForeground(profit >= 0 ? Color.GREEN : Color.RED);
                    setHorizontalAlignment(SwingConstants.RIGHT);
                }

                return this;
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(profitRenderer);

        // ROI column
        DefaultTableCellRenderer roiRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Double) {
                    setText(String.format("%.1f%%", (Double) value));
                } else {
                    super.setValue(value);
                }
            }
        };
        roiRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(roiRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(scrollPane, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setCurrentCharacter(String character) {
        this.currentCharacter = character;
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Loading flips..."));

        new Thread(() -> {
            String filter = (String) filterCombo.getSelectedItem();
            String status = filter.equals("Completed") ? "completed" :
                          filter.equals("Pending") ? "pending" : "all";

            List<FlipData> allFlips = dataClient.fetchFlips(50, 7, status);

            // Note: Server-side flip filtering by character would be better,
            // but for now we'll show all flips (they're already character-specific from pairing)
            List<FlipData> flips = allFlips;

            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);

                if (flips.isEmpty()) {
                    statusLabel.setText("No flips found (server may be offline)");
                } else {
                    String charSuffix = currentCharacter != null ? " for " + currentCharacter : "";
                    statusLabel.setText(String.format("Showing %d %s flips (last 7 days)%s",
                        flips.size(), filter.toLowerCase(), charSuffix));

                    for (FlipData flip : flips) {
                        tableModel.addRow(new Object[]{
                            flip.getItemName(),
                            (long) flip.getBuyPrice(),
                            flip.getSellPrice() > 0 ? (long) flip.getSellPrice() : 0L,
                            flip.getProfitNet(),
                            flip.getRoi()
                        });
                    }
                }
            });
        }).start();
    }
}
