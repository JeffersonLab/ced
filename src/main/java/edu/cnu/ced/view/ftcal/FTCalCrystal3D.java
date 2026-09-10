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
	private final FTCalPanel3D panel;

	FTCalCrystal3D(FTCalPanel3D panel, FTCALGeometry geometry, int id) {
		super(panel);
		this.panel = panel;
		this.id = id;
		this.coords = geometry.verticesCm(id);
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		if (coords.length < 24) {
			return;
		}
		int alpha = volumeAlpha();
		Color base = color();
		Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
		for (int[] face : FACES) {
			Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3], color, 1f, true);
		}
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
