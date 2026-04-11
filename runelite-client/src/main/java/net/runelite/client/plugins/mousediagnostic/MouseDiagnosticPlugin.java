package net.runelite.client.plugins.mousediagnostic;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Canvas;
import java.awt.Dimension;

/**
 * Mouse Diagnostic Plugin
 * Logs canvas dimensions and mouse coordinates to help debug mouse offset issues
 */
@Slf4j
@PluginDescriptor(
	name = "Mouse Diagnostic",
	description = "Logs mouse and canvas dimensions for debugging coordinate issues",
	tags = {"mouse", "debug", "diagnostic", "coordinates"},
	enabledByDefault = true
)
public class MouseDiagnosticPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseDiagnosticOverlay overlay;

	private long lastLogTime = 0;
	private int tickCounter = 0;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		log.info("[MouseDiag] Mouse Diagnostic Plugin started - will log every 2 seconds");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		log.info("[MouseDiag] Mouse Diagnostic Plugin stopped");
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		tickCounter++;

		// Log every 2 seconds (roughly 3 game ticks at 600ms/tick, but check time too)
		long now = System.currentTimeMillis();
		if (tickCounter >= 3 && now - lastLogTime >= 2000)
		{
			logDimensions();
			tickCounter = 0;
			lastLogTime = now;
		}
	}

	private void logDimensions()
	{
		try
		{
			// Get canvas
			Canvas canvas = client.getCanvas();
			if (canvas == null)
			{
				log.info("[MouseDiag] Canvas is null");
				return;
			}

			// Get dimensions
			int canvasWidth = canvas.getWidth();
			int canvasHeight = canvas.getHeight();
			Dimension stretchedDim = client.getStretchedDimensions();
			Dimension realDim = client.getRealDimensions();

			// Get mouse position
			int mouseX = client.getMouseCanvasPosition().getX();
			int mouseY = client.getMouseCanvasPosition().getY();

			// Get viewport size (actual game viewport)
			int viewportWidth = client.getViewportWidth();
			int viewportHeight = client.getViewportHeight();

			log.info("[MouseDiag] ======================================");
			log.info("[MouseDiag] Canvas Size: {}x{}", canvasWidth, canvasHeight);
			log.info("[MouseDiag] Stretched Dim: {}x{}", stretchedDim.width, stretchedDim.height);
			log.info("[MouseDiag] Real Dim: {}x{}", realDim.width, realDim.height);
			log.info("[MouseDiag] Viewport Size: {}x{}", viewportWidth, viewportHeight);
			log.info("[MouseDiag] Mouse Position: ({}, {})", mouseX, mouseY);
			log.info("[MouseDiag] Resizable Mode: {}", client.isResized());
			log.info("[MouseDiag] Stretched Enabled: {}", client.isStretchedEnabled());
			log.info("[MouseDiag] ======================================");
		}
		catch (Exception e)
		{
			log.error("[MouseDiag] Error logging dimensions", e);
		}
	}
}
