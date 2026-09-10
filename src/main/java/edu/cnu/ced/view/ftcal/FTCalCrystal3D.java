package edu.cnu.ced.view.ftcal;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.data.FTCalEventData.AdcHit;
import edu.cnu.ced.geometry.FTCALGeometry;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * One FTCAL crystal, drawn as a translucent box from its 8 corners. Direct
 * port of legacy CED's {@code cnuphys.ced.ced3d.ftcal.FTCalPaddle3D}: same
 * 6-quad box decomposition, colored by this event's ADC hit (if any) or a
 * default light-blue "volume" fill otherwise.
 */
final class FTCalCrystal3D extends DetectorItem3D {

	// Face index sets into the 8-corner vertex array, one per box face --
	// identical to legacy's FTCalPaddle3D.
	private static final int[][] FACES = {
			{ 0, 1, 2, 3 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
			{ 0, 4, 5, 1 }, { 1, 5, 6, 2 }, { 4, 5, 6, 7 } };

	private static final Color DEFAULT_COLOR = new Color(173, 216, 230);

	private final int id;
	private final float[] coords;
	private final float[] sortPoint;
	private final FTCalPanel3D panel;

	FTCalCrystal3D(FTCalPanel3D panel, FTCALGeometry geometry, int id) {
		super(panel);
		this.panel = panel;
		this.id = id;
		this.coords = geometry.verticesCm(id);
		this.sortPoint = centroid(coords);
	}

	// Every crystal's default fill inherits from Item3D's own FILLALPHA
	// property (kept current by CedPanel3D.applyVolumeAlphaToItems), so
	// Panel3D's opaque/transparent pass classification and the alpha this
	// draws with never disagree -- see DetectorItem3D's own constructor.
	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		if (coords.length < 24) {
			return;
		}
		Color base = color();
		Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), getFillAlpha());
		for (int[] face : FACES) {
			Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3], color, 1f, true);
		}
	}

	// The 332 crystals are mostly non-overlapping, but the transparent
	// pass still sorts by distance from the camera; the default sort
	// point (the scene origin) would put every crystal at the same
	// distance and defeat that sort. Coordinates are fixed once from
	// geometry, so this is computed once and cached rather than every
	// frame.
	@Override
	public float[] getSortPoint() {
		return sortPoint;
	}

	private static float[] centroid(float[] coords) {
		if (coords.length < 24) {
			return new float[] { 0f, 0f, 0f };
		}
		float cx = 0f, cy = 0f, cz = 0f;
		for (int i = 0; i < 8; i++) {
			cx += coords[3 * i];
			cy += coords[3 * i + 1];
			cz += coords[3 * i + 2];
		}
		return new float[] { cx / 8f, cy / 8f, cz / 8f };
	}

	private Color color() {
		AdcHit hit = panel.adcHit(id);
		if (hit == null) {
			return DEFAULT_COLOR;
		}
		int maximum = panel.maximumAdc();
		double fraction = maximum == 0 ? 0.0 : (double) hit.adc() / maximum;
		return ScientificColorMap.TURBO.colorAt(fraction);
	}
}
