package edu.cnu.ced.view.central;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * One CND layer (48 paddles), drawn as a single item -- all 48 boxes in
 * one {@code drawShape()} call, matching legacy CED's {@code cnuphys.ced.
 * ced3d.cnd.CNDLayer3D} (one item per layer, not one per paddle: CND's 144
 * paddles would otherwise triple {@link edu.cnu.mdi.mdi3D.panel.Panel3D}'s
 * per-frame opaque/transparent classification and sort overhead for no
 * visual benefit, since paddles within a layer never need independent
 * visibility).
 */
final class CndLayer3D extends DetectorItem3D {

	// Face index sets into an 8-corner vertex array, one per box face --
	// identical to FTCalCrystal3D's (and legacy's Paddle3D.drawPaddle).
	private static final int[][] FACES = {
			{ 0, 1, 2, 3 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
			{ 0, 4, 5, 1 }, { 1, 5, 6, 2 }, { 4, 5, 6, 7 } };

	private static final Color EVEN_LAYER_COLOR = new Color(255, 127, 80); // coral
	private static final Color ODD_LAYER_COLOR = new Color(176, 224, 230); // powder blue

	private final CentralPanel3D panel;
	private final int layer;
	private final CedDisplayOption layerOption;
	private final Color defaultColor;

	CndLayer3D(CentralPanel3D panel, int layer, CedDisplayOption layerOption) {
		super(panel);
		this.panel = panel;
		this.layer = layer;
		this.layerOption = layerOption;
		this.defaultColor = (layer % 2 == 0) ? EVEN_LAYER_COLOR : ODD_LAYER_COLOR;
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		CNDGeometry geometry = panel.cndGeometry();
		if (geometry == null) {
			return;
		}
		int alpha = getFillAlpha();
		Color base = new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), alpha);
		for (int paddle = 1; paddle <= CNDGeometry.PADDLE_COUNT; paddle++) {
			Color color = hitColor(paddle, alpha);
			float[] coords = geometry.verticesCm(layer, paddle);
			for (int[] face : FACES) {
				Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3],
						color != null ? color : base, 1f, true);
			}
		}
	}

	private Color hitColor(int paddle, int alpha) {
		int[] address = CNDGeometry.databaseToDetector(layer, paddle);
		int sector = address[0];
		int order = address[2] - 1;
		Integer adc = panel.cndAdc(sector, layer, order);
		if (adc == null) {
			return null;
		}
		int maximum = panel.cndMaximumAdc();
		double fraction = maximum == 0 ? 0.0 : (double) adc / maximum;
		Color hit = ScientificColorMap.TURBO.colorAt(fraction);
		return new Color(hit.getRed(), hit.getGreen(), hit.getBlue(), alpha);
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.CND) && panel.isDisplayed(layerOption);
	}
}
