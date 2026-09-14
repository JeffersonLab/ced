package edu.cnu.ced.view.forward;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.ced.swim.SwimmableParticle;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every swum trajectory for the current event -- Monte Carlo truth,
 * DC hit-based/time-based (plain and AI), {@code REC::Particle}, and CVT
 * -- each gated by its own {@link CedDisplayOption} toggle. Matches legacy
 * CED's own {@code cnuphys.ced.ced3d.TrajectoryDrawer3D} in spirit, but
 * swims on demand through the shared {@link edu.cnu.ced.swim.
 * SwimTrajectoryCache} (the same one every 2D view already uses) rather
 * than reading a pre-populated {@code Swimming} trajectory cache legacy
 * relies on elsewhere.
 *
 * <p>
 * Extends {@link Item3D} directly rather than {@link
 * edu.cnu.ced.view3d.DetectorItem3D}, matching {@link
 * ForwardCrossDrawer3D}'s own reasoning: a trajectory is live event data,
 * not a detector "volume".
 * </p>
 */
final class ForwardTrajectoryDrawer3D extends Item3D {

	private static final float LINE_WIDTH = 2f;

	private final ForwardPanel3D panel;

	ForwardTrajectoryDrawer3D(ForwardPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (panel.isDisplayed(CedDisplayOption.MC_TRACKS)) {
			drawTrackRows(drawable, panel.mcTracks(), null);
		}
		if (panel.isDisplayed(CedDisplayOption.HB_TRACKS)) {
			drawTrackRows(drawable, panel.hbTracks(), null);
		}
		if (panel.isDisplayed(CedDisplayOption.TB_TRACKS)) {
			drawTrackRows(drawable, panel.tbTracks(), null);
		}
		if (panel.isDisplayed(CedDisplayOption.AI_HB_TRACKS)) {
			drawTrackRows(drawable, panel.aiHbTracks(), CedDrawingStyle.AI_HIT_BASED);
		}
		if (panel.isDisplayed(CedDisplayOption.AI_TB_TRACKS)) {
			drawTrackRows(drawable, panel.aiTbTracks(), CedDrawingStyle.AI_TIME_BASED);
		}
		if (panel.isDisplayed(CedDisplayOption.CVT_TRACKS)) {
			drawTrackRows(drawable, panel.cvtTracks(), null);
		}
		if (panel.isDisplayed(CedDisplayOption.RECON_TRACKS)) {
			for (RecEventData.Particle particle : panel.recParticles()) {
				Color color = CedDrawingStyle.particleColor(particle.pid(), particle.charge());
				drawTrajectory(drawable, panel.swimCache().trajectory(SwimmableParticle.of(particle), panel.fieldProbe()), color);
			}
		}
	}

	private void drawTrackRows(GLAutoDrawable drawable, List<TrackRow> rows, Color colorOverride) {
		for (TrackRow row : rows) {
			Color color = colorOverride != null ? colorOverride : CedDrawingStyle.particleColor(row.pid(), row.charge());
			drawTrajectory(drawable, panel.swimCache().trajectory(SwimmableParticle.of(row), panel.fieldProbe()), color);
		}
	}

	private void drawTrajectory(GLAutoDrawable drawable, List<Point3> trajectory, Color color) {
		if (trajectory.size() < 2) {
			return;
		}
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
