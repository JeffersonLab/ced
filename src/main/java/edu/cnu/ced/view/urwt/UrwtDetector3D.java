package edu.cnu.ced.view.urwt;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.URWTEventData;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.geometry.URWTGeometry;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * One μrWT (sector, layer) detector, drawn as a single filled polygon
 * tracing the swept area of its ordered strips, plus a highlighted line
 * per strip with a hit this event.
 *
 * <p>
 * Legacy CED's own {@code UrWTDetectorItem3D} fills the convex hull of
 * every strip endpoint. {@code mdi_ced}'s {@link URWTGeometry} exposes
 * only the ordered strip list itself, with no precomputed hull -- but for
 * an <em>ordered</em> strip array (parallel or fan-shaped alike), walking
 * every strip's own real start point in order, then every strip's own
 * real end point in reverse order, traces the exact boundary of the
 * strips' swept area as a simple polygon: no hull algorithm needed, and
 * unlike approximating from just the first/last strip (as {@code
 * edu.cnu.ced.view.central.BstLayer3D} does for BST panels), this uses
 * every strip's real geometry, so it is exact even where the true shape
 * is a curve rather than a straight edge. The polygon is fan-triangulated
 * from its first vertex for filling, which is correct as long as the
 * boundary is star-shaped from that vertex -- true for both a parallel
 * strip stack and a fan -- and its outline is drawn as a closed line
 * strip for a clean boundary.
 * </p>
 */
final class UrwtDetector3D extends DetectorItem3D {

	private static final Color[] LAYER_COLORS = {
			new Color(0, 200, 200), // layer 1: teal
			new Color(200, 0, 200), // layer 2: magenta
			new Color(230, 140, 0), // layer 3: orange
			new Color(120, 200, 0), // layer 4: chartreuse
	};
	private static final Color HIT_COLOR = Color.red;
	private static final float HIT_LINE_WIDTH = 2f;

	private final UrwtPanel3D panel;
	private final int sector;
	private final int layer;
	private final Color layerColor;
	private final float[] boundary; // closed polygon outline, (2N + 1) points
	private final float[] fan; // fan-triangulated fill, (2N - 2) triangles

	UrwtDetector3D(UrwtPanel3D panel, int sector, int layer) {
		super(panel);
		this.panel = panel;
		this.sector = sector;
		this.layer = layer;
		this.layerColor = LAYER_COLORS[layer - 1];

		List<Segment3> strips = panel.geometry().detector(sector, layer).strips();
		int n = strips.size();
		Point3[] loop = new Point3[2 * n];
		for (int i = 0; i < n; i++) {
			loop[i] = strips.get(i).start();
			loop[2 * n - 1 - i] = strips.get(i).end();
		}

		boundary = new float[(2 * n + 1) * 3];
		for (int i = 0; i < 2 * n; i++) {
			set(boundary, i, loop[i]);
		}
		set(boundary, 2 * n, loop[0]); // close the loop

		fan = new float[Math.max(0, 2 * n - 2) * 9];
		for (int i = 1; i < 2 * n - 1; i++) {
			int t = (i - 1) * 9;
			putPoint(fan, t, loop[0]);
			putPoint(fan, t + 3, loop[i]);
			putPoint(fan, t + 6, loop[i + 1]);
		}
	}

	private static void set(float[] coords, int index, Point3 point) {
		putPoint(coords, index * 3, point);
	}

	private static void putPoint(float[] coords, int offset, Point3 point) {
		coords[offset] = (float) point.x();
		coords[offset + 1] = (float) point.y();
		coords[offset + 2] = (float) point.z();
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		int alpha = getFillAlpha();
		Color fill = new Color(layerColor.getRed(), layerColor.getGreen(), layerColor.getBlue(), alpha);
		if (fan.length > 0) {
			Support3D.drawTriangles(drawable, fan, fill, 1f, false);
		}
		Support3D.drawPolyLine(drawable, boundary, fill.darker(), 1.5f);
	}

	@Override
	protected void drawData(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.RAW_DATA)) {
			return;
		}
		for (URWTEventData.Hit hit : panel.hits(sector, layer)) {
			Segment3 strip = panel.geometry().strip(sector, layer, hit.strip());
			Support3D.drawLine(drawable, strip.start().x(), strip.start().y(), strip.start().z(),
					strip.end().x(), strip.end().y(), strip.end().z(), HIT_COLOR, HIT_LINE_WIDTH);
		}
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(layerOption(layer));
	}

	private static CedDisplayOption layerOption(int layer) {
		return switch (layer) {
		case 1 -> CedDisplayOption.URWT_LAYER_1;
		case 2 -> CedDisplayOption.URWT_LAYER_2;
		case 3 -> CedDisplayOption.URWT_LAYER_3;
		default -> CedDisplayOption.URWT_LAYER_4;
		};
	}
}
