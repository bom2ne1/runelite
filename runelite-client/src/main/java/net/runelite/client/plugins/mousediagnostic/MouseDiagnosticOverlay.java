package net.runelite.client.plugins.mousediagnostic;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Displays mouse diagnostic info on screen
 */
public class MouseDiagnosticOverlay extends Overlay
{
	private final Client client;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	private MouseDiagnosticOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();

		Canvas canvas = client.getCanvas();
		if (canvas == null)
		{
			return null;
		}

		// Get dimensions
		int canvasWidth = canvas.getWidth();
		int canvasHeight = canvas.getHeight();
		Dimension stretchedDim = client.getStretchedDimensions();
		Dimension realDim = client.getRealDimensions();

		// Get mouse
		int mouseX = client.getMouseCanvasPosition().getX();
		int mouseY = client.getMouseCanvasPosition().getY();

		// Build overlay
		panelComponent.getChildren().add(LineComponent.builder()
			.left("MOUSE DIAGNOSTIC")
			.leftColor(Color.CYAN)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Canvas:")
			.right(canvasWidth + "x" + canvasHeight)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Stretched:")
			.right(stretchedDim.width + "x" + stretchedDim.height)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Real:")
			.right(realDim.width + "x" + realDim.height)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Mouse:")
			.right("(" + mouseX + ", " + mouseY + ")")
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Resizable:")
			.right(client.isResized() ? "YES" : "NO")
			.rightColor(client.isResized() ? Color.GREEN : Color.RED)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Stretched:")
			.right(client.isStretchedEnabled() ? "YES" : "NO")
			.rightColor(client.isStretchedEnabled() ? Color.GREEN : Color.GRAY)
			.build());

		return panelComponent.render(graphics);
	}
}
