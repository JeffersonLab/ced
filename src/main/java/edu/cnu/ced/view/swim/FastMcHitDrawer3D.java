package edu.cnu.ced.view.swim;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import org.jlab.geom.DetectorHit;
import org.jlab.geom.prim.Point3D;

import edu.cnu.ced.swim.FastMcHitFinder;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every {@link FastMcHitFinder}-computed hit -- the manual swim's
 * own hits, and every accumulated batch swim's -- as a small point marker
 * colored by detector, gated on {@link SwimTestPanel3D#showFastMcHits()}.
 * Matches the spirit of {@code ~/bCNU/fastmCED}'s own hit display: where a
 * trajectory actually crossed real sensing material, not just the
 * trajectory line itself.
 */
final class FastMcHitDrawer3D extends Item3D {

	private static final Color DC_COLOR = new Color(255, 215, 0); // gold
	private static final Color FTOF_COLOR = new Color(255, 105, 180); // hot pink
	private static final Color PCAL_COLOR = new Color(50, 220, 50); // green
	private static final Color ECAL_COLOR = new Color(60, 180, 255); // sky blue
	private static final float POINT_SIZE = 6f;

	private final SwimTestPanel3D panel;

	FastMcHitDrawer3D(SwimTestPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (!panel.showFastMcHits()) {
			return;
		}
		drawHits(drawable, panel.manualHits());
		for (FastMcHitFinder.Hits hits : panel.batchHits()) {
			drawHits(drawable, hits);
		}
	}

	private static void drawHits(GLAutoDrawable drawable, FastMcHitFinder.Hits hits) {
		drawGroup(drawable, hits.dc(), DC_COLOR);
		drawGroup(drawable, hits.ftof(), FTOF_COLOR);
		drawGroup(drawable, hits.pcal(), PCAL_COLOR);
		drawGroup(drawable, hits.ecal(), ECAL_COLOR);
	}

	private static void drawGroup(GLAutoDrawable drawable, List<DetectorHit> group, Color color) {
		for (DetectorHit hit : group) {
			Point3D position = hit.getPosition();
			Support3D.drawPoint(drawable, (float) position.x(), (float) position.y(), (float) position.z(),
					color, POINT_SIZE, true);
		}
	}
}
