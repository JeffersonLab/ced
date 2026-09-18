package edu.cnu.ced.view.swim;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.geometry.Point3;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every accumulated {@link SwimBatchResult} from {@link
 * SwimTestPanel3D}'s own batch tester, filtered by its current show mode
 * -- matching legacy CED's own {@code SwimResultDrawer} exactly: a failed
 * swim's partial path draws in {@link #FAILURE_COLOR}, and a successful
 * one draws by charge ({@link #POSITIVE_COLOR}/{@link #NEGATIVE_COLOR}/
 * {@link #NEUTRAL_COLOR}), not by species -- this is a synthetic
 * hypothetical particle, not a reconstructed one, so there's no PID to
 * color by.
 */
final class SwimBatchDrawer3D extends Item3D {

	private static final Color FAILURE_COLOR = Color.black;
	private static final Color POSITIVE_COLOR = Color.red;
	private static final Color NEGATIVE_COLOR = Color.blue;
	private static final Color NEUTRAL_COLOR = Color.cyan;
	private static final float LINE_WIDTH = 2f;

	private final SwimTestPanel3D panel;

	SwimBatchDrawer3D(SwimTestPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		for (SwimBatchResult result : panel.batchResults()) {
			if (!panel.batchShowMode().shows(result)) {
				continue;
			}
			List<Point3> trajectory = result.trajectory();
			if (trajectory.size() < 2) {
				continue;
			}
			Color color = result.success() ? colorForCharge(result.charge()) : FAILURE_COLOR;
			float[] coords = new float[trajectory.size() * 3];
			for (int i = 0; i < trajectory.size(); i++) {
				Point3 point = trajectory.get(i);
				coords[3 * i] = (float) point.x();
				coords[3 * i + 1] = (float) point.y();
				coords[3 * i + 2] = (float) point.z();
			}
			Support3D.drawPolyLine(drawable, coords, color, LINE_WIDTH);
		}
	}

	private static Color colorForCharge(int charge) {
		if (charge < 0) {
			return NEGATIVE_COLOR;
		}
		return charge > 0 ? POSITIVE_COLOR : NEUTRAL_COLOR;
	}
}
