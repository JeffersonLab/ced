package edu.cnu.ced.view.central;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.CTOFGeometry;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * All 48 CTOF paddles, drawn as a single item (see {@link CndLayer3D}'s
 * javadoc for why one item covers many paddles). Matches legacy CED's
 * {@code cnuphys.ced.ced3d.ctof.CTOF3D}.
 */
final class Ctof3D extends DetectorItem3D {

	private static final int[][] FACES = {
			{ 0, 1, 2, 3 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
			{ 0, 4, 5, 1 }, { 1, 5, 6, 2 }, { 4, 5, 6, 7 } };

	private static final Color DEFAULT_COLOR = new Color(30, 144, 255); // dodger blue

	private final CentralPanel3D panel;

	Ctof3D(CentralPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		CTOFGeometry geometry = panel.ctofGeometry();
		if (geometry == null) {
			return;
		}
		int alpha = getFillAlpha();
		Color base = new Color(DEFAULT_COLOR.getRed(), DEFAULT_COLOR.getGreen(), DEFAULT_COLOR.getBlue(), alpha);
		for (int paddle = 1; paddle <= CTOFGeometry.PADDLE_COUNT; paddle++) {
			Color color = hitColor(paddle, alpha);
			float[] coords = geometry.verticesCm(paddle);
			for (int[] face : FACES) {
				Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3],
						color != null ? color : base, 1f, true);
			}
		}
	}

	private Color hitColor(int paddle, int alpha) {
		Integer adc = panel.ctofAdc(paddle);
		if (adc == null) {
			return null;
		}
		int maximum = panel.ctofMaximumAdc();
		double fraction = maximum == 0 ? 0.0 : (double) adc / maximum;
		Color hit = ScientificColorMap.TURBO.colorAt(fraction);
		return new Color(hit.getRed(), hit.getGreen(), hit.getBlue(), alpha);
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.CTOF);
	}
}
