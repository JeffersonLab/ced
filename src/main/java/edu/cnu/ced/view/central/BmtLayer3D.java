package edu.cnu.ced.view.central;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.BMTGeometry;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * One BMT (Barrel Micromegas Tracker) layer: a fixed-radius cylindrical
 * band per sector, approximated as {@value #SEGMENTS_PER_SECTOR} flat
 * quads spanning the sector's phi range -- all three sectors in one
 * {@code drawShape()} call, matching {@link CndLayer3D}'s reasoning.
 *
 * <p>
 * Legacy CED's own {@code BMTLayer3D} only draws the "Z" (as opposed to
 * "C") strip layers as static shape, apparently because it had no way to
 * derive a panel outline for the other axis type. {@code mdi_ced}'s
 * {@code BMTGeometry.Layer} carries radius/z/phi bounds for every layer
 * regardless of axis, so this draws all six -- a deliberate improvement
 * over legacy, not a gap.
 * </p>
 */
final class BmtLayer3D extends DetectorItem3D {

	private static final int SEGMENTS_PER_SECTOR = 8;
	private static final double SECTOR_SPACING_DEG = 120.0;
	// Raw (1-based) hit-bank sector for each geometric sector index (0..2);
	// matches the 2D CentralXYView's own bmtSector(int) inverse mapping.
	private static final int[] RAW_SECTOR = { 2, 1, 3 };

	private static final Color Z_AXIS_COLOR = new Color(211, 211, 211); // light gray
	private static final Color C_AXIS_COLOR = new Color(144, 238, 144); // light green

	private final CentralPanel3D panel;
	private final int layer;
	private final CedDisplayOption layerOption;

	BmtLayer3D(CentralPanel3D panel, int layer, CedDisplayOption layerOption) {
		super(panel);
		this.panel = panel;
		this.layer = layer;
		this.layerOption = layerOption;
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		BMTGeometry geometry = panel.bmtGeometry();
		if (geometry == null) {
			return;
		}
		BMTGeometry.Layer info = geometry.layer(layer);
		int alpha = getFillAlpha();
		Color defaultColor = info.axis() == 1 ? Z_AXIS_COLOR : C_AXIS_COLOR;
		Color base = new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), alpha);
		float radiusCm = (float) (info.radiusMm() / 10.0);
		float zMinCm = (float) (info.zMinMm() / 10.0);
		float zMaxCm = (float) (info.zMaxMm() / 10.0);
		double extent = info.phiMaxDeg() - info.phiMinDeg();

		for (int geomSector = 0; geomSector < 3; geomSector++) {
			double sectorStart = info.phiMinDeg() + SECTOR_SPACING_DEG * geomSector;
			Color color = hitColor(geomSector, alpha, base);
			float[] coords = sectorQuads(sectorStart, extent, radiusCm, zMinCm, zMaxCm);
			Support3D.drawQuads(drawable, coords, color, 1f, true);
		}
	}

	private static float[] sectorQuads(double sectorStart, double extent, float radius, float zMin, float zMax) {
		float[] coords = new float[SEGMENTS_PER_SECTOR * 12];
		double step = extent / SEGMENTS_PER_SECTOR;
		for (int segment = 0; segment < SEGMENTS_PER_SECTOR; segment++) {
			double phi1 = Math.toRadians(sectorStart + segment * step);
			double phi2 = Math.toRadians(sectorStart + (segment + 1) * step);
			float x1 = (float) (radius * Math.cos(phi1));
			float y1 = (float) (radius * Math.sin(phi1));
			float x2 = (float) (radius * Math.cos(phi2));
			float y2 = (float) (radius * Math.sin(phi2));
			int i = segment * 12;
			coords[i] = x1; coords[i + 1] = y1; coords[i + 2] = zMin;
			coords[i + 3] = x2; coords[i + 4] = y2; coords[i + 5] = zMin;
			coords[i + 6] = x2; coords[i + 7] = y2; coords[i + 8] = zMax;
			coords[i + 9] = x1; coords[i + 10] = y1; coords[i + 11] = zMax;
		}
		return coords;
	}

	private Color hitColor(int geomSector, int alpha, Color fallback) {
		Integer adc = panel.bmtAdc(RAW_SECTOR[geomSector], layer);
		if (adc == null) {
			return fallback;
		}
		int maximum = panel.bmtMaximumAdc();
		double fraction = maximum == 0 ? 0.0 : (double) adc / maximum;
		Color hit = ScientificColorMap.TURBO.colorAt(fraction);
		return new Color(hit.getRed(), hit.getGreen(), hit.getBlue(), alpha);
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.BMT) && panel.isDisplayed(layerOption);
	}
}
