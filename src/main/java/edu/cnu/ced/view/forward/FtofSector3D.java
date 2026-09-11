package edu.cnu.ced.view.forward;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.FTOFGeometry;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * One FTOF sector's three panels (1A, 1B, 2), all paddles drawn in a single
 * {@code drawShape()} call -- matching {@link
 * edu.cnu.ced.view.central.CndLayer3D}'s reasoning. Legacy CED's own {@code
 * FTOF3D} similarly draws all of one sector's paddles from one item (with
 * per-panel child items only for bookkeeping); this collapses that into
 * one item entirely, since panels within a sector never need independent
 * visibility.
 */
final class FtofSector3D extends DetectorItem3D {

	private static final int[][] FACES = {
			{ 0, 1, 2, 3 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
			{ 0, 4, 5, 1 }, { 1, 5, 6, 2 }, { 4, 5, 6, 7 } };

	private static final Color DEFAULT_COLOR = new Color(135, 206, 250); // light sky blue

	private final ForwardPanel3D panel;
	private final int sector;

	FtofSector3D(ForwardPanel3D panel, int sector) {
		super(panel);
		this.panel = panel;
		this.sector = sector;
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		FTOFGeometry geometry = panel.ftofGeometry();
		if (geometry == null) {
			return;
		}
		int alpha = getFillAlpha();
		Color base = new Color(DEFAULT_COLOR.getRed(), DEFAULT_COLOR.getGreen(), DEFAULT_COLOR.getBlue(), alpha);
		for (int ftofPanel = 0; ftofPanel < FTOFGeometry.PANEL_COUNT; ftofPanel++) {
			int count = geometry.paddleCount(ftofPanel);
			for (int paddle = 1; paddle <= count; paddle++) {
				float[] coords = geometry.verticesCm(sector, ftofPanel, paddle);
				Color color = hitColor(ftofPanel, paddle, alpha, base);
				for (int[] face : FACES) {
					Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3], color, 1f, true);
				}
			}
		}
	}

	private Color hitColor(int ftofPanel, int paddle, int alpha, Color fallback) {
		Integer adc = panel.ftofAdc(sector, ftofPanel, paddle);
		if (adc == null) {
			return fallback;
		}
		int maximum = panel.ftofMaximumAdc();
		double fraction = maximum == 0 ? 0.0 : (double) adc / maximum;
		Color hit = ScientificColorMap.TURBO.colorAt(fraction);
		return new Color(hit.getRed(), hit.getGreen(), hit.getBlue(), alpha);
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.FTOF) && panel.isDisplayed(ForwardPanel3D.sectorOption(sector));
	}
}
