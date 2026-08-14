package edu.cnu.ced.view.calorimeter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/** Shared drawing details for the PCAL and ECAL laboratory-XY views. */
public final class CalorimeterDrawingSupport {
	private static final Color RECON_CENTER = new Color(0, 210, 220);
	private static final Color DETECTOR_LABEL = new Color(35, 35, 35, 55);

	private CalorimeterDrawingSupport() { }

	/** Draw a compact center marker over a reconstructed calorimeter footprint. */
	public static void drawReconCenter(Graphics2D graphics, Point center) {
		graphics.setColor(Color.BLACK);
		graphics.drawLine(center.x - 5, center.y - 5, center.x + 5, center.y + 5);
		graphics.drawLine(center.x - 5, center.y + 5, center.x + 5, center.y - 5);
		graphics.setColor(RECON_CENTER);
		graphics.drawLine(center.x - 4, center.y - 4, center.x + 4, center.y + 4);
		graphics.drawLine(center.x - 4, center.y + 4, center.x + 4, center.y - 4);
	}

	/** Draw a quiet detector identifier in the upper-left of the data canvas. */
	public static void drawDetectorLabel(Graphics2D graphics, String label) {
		graphics.setColor(DETECTOR_LABEL);
		graphics.setFont(graphics.getFont().deriveFont(32f));
		graphics.drawString(label, 12, 40);
	}
}
