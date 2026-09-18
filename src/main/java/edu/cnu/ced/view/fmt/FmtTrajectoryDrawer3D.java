package edu.cnu.ced.view.fmt;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.FMTEventData.Trajectory;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every {@code FMT::Trajectory} point for the current event: where
 * each DC track crosses each FMT layer, as a discrete point -- not a
 * continuous polyline, unlike {@link edu.cnu.ced.view3d.TrackTrajectoryDrawer3D}'s
 * swum tracks -- matching legacy CED's own {@code
 * cnuphys.ced.ced3d.fmt.FMTTrajectoryDrawer3D} exactly, including its
 * color convention: {@link #ORIGINAL_DC_COLOR} (light gray) for a track
 * whose {@code FMT::Tracks} status says it's still just its original
 * DC-only fit, {@link #TRAJECTORY_COLOR} (cyan) for one FMT actually
 * refit.
 *
 * <p>
 * {@code FMT::Trajectory}'s own world position ({@code x, y, z}) is
 * {@code (0, 0, 0)} when the reconstruction didn't compute one for that
 * row; this drawer then falls back to resolving the DC track's own
 * local-frame position at that layer ({@code dx, dy, dz}) through {@link
 * edu.cnu.ced.geometry.FMTGeometry#localToGlobal}, matching legacy's own
 * fallback exactly.
 * </p>
 */
final class FmtTrajectoryDrawer3D extends Item3D {

	private static final Color TRAJECTORY_COLOR = Color.cyan;
	private static final Color ORIGINAL_DC_COLOR = Color.lightGray;
	private static final float POINT_SIZE = 7f;
	private static final float POINT_OUTLINE_SIZE = 9f;

	private final FMTPanel3D panel;

	FmtTrajectoryDrawer3D(FMTPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.FMT_TRAJECTORIES)) {
			return;
		}
		for (Trajectory point : panel.trajectories()) {
			float x = point.x();
			float y = point.y();
			float z = point.z();
			if (x == 0f && y == 0f && z == 0f) {
				Point3 global = panel.geometry().localToGlobal(point.layer(),
						new Point3(point.dx(), point.dy(), point.dz()));
				x = (float) global.x();
				y = (float) global.y();
				z = (float) global.z();
			}
			if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				continue;
			}
			Color color = panel.isOriginalDcTrack(point.trackIndex()) ? ORIGINAL_DC_COLOR : TRAJECTORY_COLOR;
			Support3D.drawPoint(drawable, x, y, z, Color.black, POINT_OUTLINE_SIZE, true);
			Support3D.drawPoint(drawable, x, y, z, color, POINT_SIZE, true);
		}
	}
}
