package edu.cnu.ced.style;

import java.awt.Color;

import edu.cnu.ced.data.DCEventData.ReconKind;

/** Shared semantic colors used by CED detector and reconstruction drawings. */
public final class CedDrawingStyle {

	public static final Color RAW_HIT = new Color(225, 35, 25);
	public static final Color HIT_BASED = Color.YELLOW;
	public static final Color TIME_BASED = new Color(255, 140, 0);
	public static final Color AI_HIT_BASED = new Color(0, 255, 127);
	public static final Color AI_TIME_BASED = Color.MAGENTA;

	public static final Color RECON_HIT = RAW_HIT;
	public static final Color RECON_CLUSTER = new Color(205, 0, 205);
	public static final Color RECON_CROSS = new Color(20, 145, 35);

	private CedDrawingStyle() {
	}

	public static Color reconstructionColor(ReconKind kind) {
		return switch (kind) {
		case HB -> HIT_BASED;
		case TB -> TIME_BASED;
		case AI_HB -> AI_HIT_BASED;
		case AI_TB -> AI_TIME_BASED;
		};
	}

	public static Color outline(Color color) {
		return color.darker();
	}

	public static Color translucent(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}
