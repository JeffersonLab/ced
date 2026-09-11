package edu.cnu.ced.view.alert;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.AlertGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * One ALERT time-of-flight (ATOF) (sector, superlayer, layer) group, all
 * its paddles (1 for superlayer 0, 10 for superlayer 1) drawn as boxes in
 * a single {@code drawShape()} call -- matching {@link
 * edu.cnu.ced.view.central.CndLayer3D}'s reasoning. Legacy CED's own
 * {@code AlertPaddle3D} gives each individual paddle its own item (up to
 * 660 of them); {@link AlertGeometry#tofPaddles} already groups them by
 * address, so this collapses each group into one item instead.
 *
 * <p>
 * ATOF hits ({@code ATOF::hits}) carry the paddle's own resolved world
 * position but an address that {@code edu.cnu.ced.data.AlertEventData}
 * documents as "not resolved via geometry" -- unlike AHDC's ADC hits,
 * there is no confirmed mapping back to a specific paddle box here, so
 * (consistent with never fabricating an unconfirmed numbering) paddles
 * draw in their plain default color only; no hit highlighting.
 * </p>
 */
final class AlertTofGroup3D extends DetectorItem3D {

	private static final int[][] FACES = {
			{ 0, 1, 2, 3 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
			{ 0, 4, 5, 1 }, { 1, 5, 6, 2 }, { 4, 5, 6, 7 } };

	private static final Color SUPERLAYER_0_COLOR = new Color(224, 255, 255); // light cyan
	private static final Color EVEN_LAYER_COLOR = new Color(255, 255, 224); // light yellow
	private static final Color ODD_LAYER_COLOR = new Color(144, 238, 144); // light green

	private final AlertPanel3D panel;
	private final int sector;
	private final int superlayer;
	private final int layer;
	private final Color defaultColor;

	AlertTofGroup3D(AlertPanel3D panel, int sector, int superlayer, int layer) {
		super(panel);
		this.panel = panel;
		this.sector = sector;
		this.superlayer = superlayer;
		this.layer = layer;
		this.defaultColor = superlayer == 0 ? SUPERLAYER_0_COLOR : ((layer % 2 == 0) ? EVEN_LAYER_COLOR : ODD_LAYER_COLOR);
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		int alpha = getFillAlpha();
		Color color = new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), alpha);
		float[] coords = new float[24];
		for (AlertGeometry.Paddle paddle : panel.geometry().tofPaddles(sector, superlayer, layer)) {
			toFloats(paddle.vertices(), coords);
			for (int[] face : FACES) {
				Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3], color, 1f, true);
			}
		}
	}

	private static void toFloats(List<Point3> points, float[] coords) {
		for (int i = 0; i < 8; i++) {
			Point3 point = points.get(i);
			coords[3 * i] = (float) point.x();
			coords[3 * i + 1] = (float) point.y();
			coords[3 * i + 2] = (float) point.z();
		}
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.ALERT_TOF)
				&& panel.isDisplayed(AlertPanel3D.sectorOption(sector));
	}
}
