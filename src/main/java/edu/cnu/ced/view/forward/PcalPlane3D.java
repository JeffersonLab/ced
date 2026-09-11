package edu.cnu.ced.view.forward;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.PCALGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * One PCAL (sector, view) plane: a translucent triangular outline as the
 * static "volume" shape, plus a box per strip with an ADC hit this event.
 * Matches legacy CED's own {@code cnuphys.ced.ced3d.cal.PCALViewPlane3D}:
 * unlike every other detector so far, PCAL/ECAL draw <em>only</em> their
 * boundary outline as static geometry, never all their strips -- the
 * strip boxes appear only where there's actually a hit.
 */
final class PcalPlane3D extends DetectorItem3D {

	private static final int[][] FACES = {
			{ 0, 1, 2, 3 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
			{ 0, 4, 5, 1 }, { 1, 5, 6, 2 }, { 4, 5, 6, 7 } };

	private static final Color DEFAULT_COLOR = new Color(32, 200, 64);

	private final ForwardPanel3D panel;
	private final int sector;
	private final int view; // 0-based [0, 1, 2] == [U, V, W]
	private final float[] triangle;

	PcalPlane3D(ForwardPanel3D panel, int sector, int view) {
		super(panel);
		this.panel = panel;
		this.sector = sector;
		this.view = view;
		this.triangle = toFloats(panel.pcalGeometry().viewTriangle(sector, view), 3);
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		int alpha = getFillAlpha();
		Color color = new Color(DEFAULT_COLOR.getRed(), DEFAULT_COLOR.getGreen(), DEFAULT_COLOR.getBlue(), alpha);
		Support3D.drawTriangles(drawable, triangle, color, 1f, true);
	}

	@Override
	protected void drawData(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.RAW_DATA)) {
			return;
		}
		PCALGeometry geometry = panel.pcalGeometry();
		int maximum = panel.pcalMaximumAdc();
		for (ForwardPanel3D.StripHit hit : panel.pcalHits(sector, view)) {
			float[] coords = toFloats(geometry.stripVertices(sector, view, hit.strip()), 8);
			double fraction = maximum == 0 ? 0.0 : (double) hit.adc() / maximum;
			Color color = ScientificColorMap.TURBO.colorAt(fraction);
			for (int[] face : FACES) {
				Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3], color, 1f, true);
			}
		}
	}

	private static float[] toFloats(List<Point3> points, int count) {
		float[] values = new float[count * 3];
		for (int i = 0; i < count; i++) {
			Point3 point = points.get(i);
			values[3 * i] = (float) point.x();
			values[3 * i + 1] = (float) point.y();
			values[3 * i + 2] = (float) point.z();
		}
		return values;
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.PCAL) && panel.isDisplayed(ForwardPanel3D.sectorOption(sector));
	}
}
