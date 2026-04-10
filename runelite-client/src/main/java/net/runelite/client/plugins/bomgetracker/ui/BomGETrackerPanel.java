package net.runelite.client.plugins.bomgetracker.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.bomgetracker.BomGETrackerPlugin;
import net.runelite.client.plugins.bomgetracker.GELimitTracker;
import net.runelite.client.plugins.bomgetracker.HttpDataClient;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Slf4j
public class BomGETrackerPanel extends PluginPanel {
    private final BomGETrackerPlugin plugin;
    private final HttpDataClient dataClient;
    private final GELimitTracker limitTracker;
    private final JLabel serverStatusLabel;
    private final TradesTabPanel tradesTab;
    private final AlchTabPanel alchTab;

    public BomGETrackerPanel(BomGETrackerPlugin plugin, HttpDataClient dataClient, GELimitTracker limitTracker) {
        super(false);
        this.plugin = plugin;
        this.dataClient = dataClient;
        this.limitTracker = limitTracker;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("BomGE Tracker");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        serverStatusLabel = new JLabel("●");
        serverStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        serverStatusLabel.setForeground(Color.GRAY);
        serverStatusLabel.setToolTipText("Server status");
        headerPanel.add(serverStatusLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Create tab panels
        log.info("[BomGETrackerPanel] Creating tab panels...");
        tradesTab = new TradesTabPanel(dataClient);
        log.info("[BomGETrackerPanel] TradesTabPanel created");

        try {
            alchTab = new AlchTabPanel(dataClient, limitTracker);
            log.info("[BomGETrackerPanel] AlchTabPanel created successfully");
        } catch (Exception e) {
            log.error("[BomGETrackerPanel] FAILED to create AlchTabPanel", e);
            throw e;
        }

        // Main content display panel (MaterialTabGroup manages content switching)
        log.info("[BomGETrackerPanel] Creating display panel");
        JPanel display = new JPanel();
        display.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Create Material Tab Group
        log.info("[BomGETrackerPanel] Creating MaterialTabGroup");
        final MaterialTabGroup tabGroup = new MaterialTabGroup(display);
        tabGroup.setLayout(new GridLayout(1, 2, 7, 7));
        tabGroup.setBorder(new EmptyBorder(10, 10, 10, 10));
        tabGroup.setBackground(ColorScheme.DARK_GRAY_COLOR);

        MaterialTab tradesTabButton = new MaterialTab("Trades", tabGroup, tradesTab);
        MaterialTab alchTabButton = new MaterialTab("Alch", tabGroup, alchTab);
        log.info("[BomGETrackerPanel] Created tab buttons");

        tabGroup.addTab(tradesTabButton);
        tabGroup.addTab(alchTabButton);
        log.info("[BomGETrackerPanel] Added tabs to tabGroup");

        tabGroup.select(tradesTabButton);
        log.info("[BomGETrackerPanel] Selected tradesTabButton as default");

        add(tabGroup, BorderLayout.NORTH);
        add(display, BorderLayout.CENTER);
        log.info("[BomGETrackerPanel] Added tabGroup and display to panel");

        // Initial refresh
        updateServerStatus();
        tradesTab.refresh();
    }

    public void setCurrentCharacter(String character) {
        tradesTab.setCurrentCharacter(character);
        alchTab.setCharacter(character);
    }

    public void refreshAlchTabLimits() {
        alchTab.refreshLimits();
    }

    public void updateServerStatus() {
        new Thread(() -> {
            boolean online = dataClient.isServerOnline();

            SwingUtilities.invokeLater(() -> {
                if (online) {
                    serverStatusLabel.setForeground(Color.GREEN);
                    serverStatusLabel.setToolTipText("Server online");
                } else {
                    serverStatusLabel.setForeground(Color.RED);
                    serverStatusLabel.setToolTipText("Server offline");
                }
            });
        }).start();
    }

    public void onRemove() {
        // Stop auto-refresh timer when panel is removed
        tradesTab.stopAutoRefresh();
    }

    /**
     * Get the Alch tab for accessing filter settings.
     */
    public AlchTabPanel getAlchTab() {
        return alchTab;
    }
}
