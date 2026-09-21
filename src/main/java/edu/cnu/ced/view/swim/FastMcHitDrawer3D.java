package edu.cnu.ced.view.swim;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import org.jlab.geom.DetectorHit;
import org.jlab.geom.prim.Point3D;

import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every {@link FastMcHitFinder}-computed DC hit -- the manual swim's
 * own hits, and every accumulated batch swim's -- as a small point marker,
 * gated on {@link SwimTestPanel3D#showFastMcHits()}. Matches the spirit of
 * {@code ~/bCNU/fastmCED}'s own hit display: where a trajectory actually
 * crossed a real sensing component, not just the trajectory line itself.
 */
final class FastMcHitDrawer3D extends Item3D {

	private static final Color HIT_COLOR = new Color(255, 215, 0); // gold, distinct from every other color in this view
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
		drawHits(drawable, panel.manualDcHits());
		for (List<DetectorHit> hits : panel.batchDcHits()) {
			drawHits(drawable, hits);
		}
	}

	private static void drawHits(GLAutoDrawable drawable, List<DetectorHit> hits) {
		for (DetectorHit hit : hits) {
			Point3D position = hit.getPosition();
			Support3D.drawPoint(drawable, (float) position.x(), (float) position.y(), (float) position.z(),
					HIT_COLOR, POINT_SIZE, true);
		}
	}
}
