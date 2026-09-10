package edu.cnu.ced.view.central;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.BSTGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * One BST (Barrel Silicon Tracker) layer, drawn as one flat quad per
 * sector panel -- all panels in one {@code drawShape()} call, matching
 * {@link CndLayer3D}'s reasoning.
 *
 * <p>
 * Each panel's quad is built from the physical end points of its first
 * and last strip ({@code BSTGeometry.strip(sector, layer, 0)} and {@code
 * strip(sector, layer, STRIP_COUNT - 1)}) -- the two real edges of the
 * sensor, rather than a synthesized outline. This simplifies legacy CED's
 * own three-segment panel quads (each panel is actually three silicon
 * sensors end to end along z, with small gaps between them) down to one
 * quad spanning the panel's full extent; the gaps are a cosmetic detail
 * on the order of millimeters against tens-of-centimeters panels.
 * </p>
 */
final class BstLayer3D extends DetectorItem3D {

	private static final Color ODD_LAYER_COLOR = new Color(60, 179, 113); // medium sea green
	private static final Color EVEN_LAYER_COLOR = new Color(216, 191, 216); // thistle

	private final CentralPanel3D panel;
	private final int layer;
	private final CedDisplayOption layerOption;
	private final Color defaultColor;

	BstLayer3D(CentralPanel3D panel, int layer, CedDisplayOption layerOption) {
		super(panel);
		this.panel = panel;
		this.layer = layer;
		this.layerOption = layerOption;
		this.defaultColor = (layer % 2 == 0) ? EVEN_LAYER_COLOR : ODD_LAYER_COLOR;
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		BSTGeometry geometry = panel.bstGeometry();
		if (geometry == null) {
			return;
		}
		int alpha = getFillAlpha();
		Color base = new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), alpha);
		int sectorCount = BSTGeometry.SECTORS_PER_LAYER[layer - 1];
		for (int sector = 1; sector <= sectorCount; sector++) {
			float[] coords = panelQuad(geometry, sector);
			Color color = hitColor(sector, alpha);
			Support3D.drawQuad(drawable, coords, 0, 1, 2, 3, color != null ? color : base, 1f, true);
		}
	}

	private float[] panelQuad(BSTGeometry geometry, int sector) {
		Segment3 first = geometry.strip(sector - 1, layer - 1, 0);
		Segment3 last = geometry.strip(sector - 1, layer - 1, BSTGeometry.STRIP_COUNT - 1);
		float[] coords = new float[12];
		set(coords, 0, first.start());
		set(coords, 1, first.end());
		set(coords, 2, last.end());
		set(coords, 3, last.start());
		return coords;
	}

	private static void set(float[] coords, int corner, Point3 point) {
		int i = corner * 3;
		coords[i] = (float) (point.x() / 10.0);
		coords[i + 1] = (float) (point.y() / 10.0);
		coords[i + 2] = (float) (point.z() / 10.0);
	}

	private Color hitColor(int sector, int alpha) {
		Integer adc = panel.bstAdc(sector, layer);
		if (adc == null) {
			return null;
		}
		int maximum = panel.bstMaximumAdc();
		double fraction = maximum == 0 ? 0.0 : (double) adc / maximum;
		Color hit = ScientificColorMap.TURBO.colorAt(fraction);
		return new Color(hit.getRed(), hit.getGreen(), hit.getBlue(), alpha);
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.BST) && panel.isDisplayed(layerOption);
	}
}
