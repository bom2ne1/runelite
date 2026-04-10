package net.runelite.client.plugins.bomgetracker.ui;

import net.runelite.client.plugins.bomgetracker.HttpDataClient;
import net.runelite.client.plugins.bomgetracker.model.FlipStats;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import java.awt.*;

public class StatsTabPanel extends JPanel {
    private final HttpDataClient dataClient;
    private final JLabel statusLabel;
    private final JPanel statsPanel;
    private String currentCharacter;

    public StatsTabPanel(HttpDataClient dataClient) {
        this.dataClient = dataClient;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Status label
        statusLabel = new JLabel("Loading...");
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.NORTH);

        // Stats panel
        statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(0, 2, 10, 10));
        statsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(statsPanel);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(null);
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
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Loading stats...");
            statsPanel.removeAll();
        });

        new Thread(() -> {
            FlipStats stats = dataClient.fetchStats(7);

            SwingUtilities.invokeLater(() -> {
                statsPanel.removeAll();

                if (stats == null) {
                    statusLabel.setText("Stats unavailable (server may be offline)");
                } else {
                    String charSuffix = currentCharacter != null ? " for " + currentCharacter : "";
                    statusLabel.setText("Statistics (last 7 days)" + charSuffix);

                    // Total Flips
                    addStatCard("Total Flips", String.valueOf(stats.getTotalFlips()));

                    // Success Rate
                    addStatCard("Success Rate", String.format("%.1f%%", stats.getSuccessRate()));

                    // Total Profit
                    addStatCard("Total Profit",
                        QuantityFormatter.quantityToStackSize(stats.getTotalProfit()) + " gp");

                    // Average Profit
                    addStatCard("Avg Profit",
                        QuantityFormatter.quantityToStackSize((long) stats.getAvgProfit()) + " gp");

                    // Average ROI
                    addStatCard("Avg ROI", String.format("%.1f%%", stats.getAvgRoi()));

                    // Average Hold Time
                    addStatCard("Avg Hold Time", formatDuration(stats.getAvgHoldTime()));
                }

                statsPanel.revalidate();
                statsPanel.repaint();
            });
        }).start();
    }

    private void addStatCard(String label, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(FontManager.getRunescapeSmallFont());
        titleLabel.setForeground(Color.LIGHT_GRAY);
        card.add(titleLabel, BorderLayout.NORTH);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(FontManager.getRunescapeBoldFont());
        valueLabel.setForeground(Color.WHITE);
        card.add(valueLabel, BorderLayout.CENTER);

        statsPanel.add(card);
    }

    private String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            int minutes = seconds / 60;
            return minutes + "m";
        } else if (seconds < 86400) {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        } else {
            int days = seconds / 86400;
            int hours = (seconds % 86400) / 3600;
            return days + "d " + hours + "h";
        }
    }
}
