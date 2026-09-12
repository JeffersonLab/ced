package edu.cnu.ced.view.swim;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cnuphys.magfield.FieldProbe;

import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.swim.ParticleSwimmer;
import edu.cnu.ced.swim.SwimmableParticle;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.PolyLine3D;
import edu.cnu.mdi.mdi3D.item3D.Sphere;
import edu.cnu.mdi.mdi3D.panel.Panel3D;

/**
 * The 3D scene for {@link SwimTestView3D}: an axis set, and -- once the
 * user swims a hypothetical particle -- its trajectory and vertex.
 *
 * <p>
 * Deliberately not a {@link edu.cnu.ced.view3d.CedPanel3D}: this view has
 * no live event data at all (matching legacy CED's own {@code
 * SwimmerPanel3D}, itself a plain {@code PlainPanel3D}, not a {@code
 * CedPanel3D}), so there is no volume-alpha slider, PID legend, or
 * display-option checkbox array here -- just the swim controls and the
 * result.
 * </p>
 *
 * <p>
 * Scoped to the swim itself: legacy's optional background detector
 * volumes (DC/FTOF/PCAL/ECAL, all unchecked by default there too) and
 * reference-surface picker (constant-z plane, constant-rho or arbitrary
 * cylinder, arbitrary plane) are deferred as a follow-up.
 * </p>
 */
final class SwimTestPanel3D extends Panel3D {

	private static final float XY_MAX = 600f;
	private static final float Z_MIN = -100f;
	private static final float Z_MAX = 600f;

	private static final Color TRAJECTORY_COLOR = Color.red;
	private static final Color VERTEX_COLOR = Color.blue;
	private static final float TRAJECTORY_LINE_WIDTH = 2f;
	private static final float VERTEX_RADIUS = 3f;

	// Neither is touched from addWest() (called from the Panel3D
	// superclass constructor, before this class's own fields exist --
	// see CedPanel3D's own comment on the same hazard); both are only
	// ever created or updated from swim()/clearTrajectory(), called well
	// after full construction, from the control panel's button listeners.
	private PolyLine3D trajectoryItem;
	private Sphere vertexItem;

	SwimTestPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	@Override
	public void createInitialItems() {
		addItem(new Axes3D(this, -XY_MAX, XY_MAX, -XY_MAX, XY_MAX, Z_MIN, Z_MAX,
				new String[] { "x", "y", "z" }, Color.darkGray, 1f, 7, 7, 8,
				Color.black, new Color(0, 100, 0), new Font("SansSerif", Font.PLAIN, 10), 0));
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	@Override
	protected JComponent addWest() {
		return new SwimTestControlPanel(this);
	}

	@Override
	protected JComponent addNorth() {
		JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
		north.add(new JLabel("Stand-alone swim test -- independent of any physics event."));
		return north;
	}

	/**
	 * Swims a hypothetical particle with the given charge/vertex/momentum
	 * through the current magnetic field and displays the result.
	 *
	 * @return {@code true} if the swim produced a usable trajectory
	 */
	boolean swim(int charge, double vx, double vy, double vz, double p, double thetaDeg, double phiDeg) {
		SwimmableParticle particle = new SwimmableParticle(0, charge, vx, vy, vz, p, thetaDeg, phiDeg, 0);
		List<Point3> trajectory = ParticleSwimmer.swim(particle, FieldProbe.factory());
		if (trajectory.isEmpty()) {
			return false;
		}

		float[] coords = new float[trajectory.size() * 3];
		for (int i = 0; i < trajectory.size(); i++) {
			Point3 point = trajectory.get(i);
			coords[3 * i] = (float) point.x();
			coords[3 * i + 1] = (float) point.y();
			coords[3 * i + 2] = (float) point.z();
		}

		if (trajectoryItem == null) {
			trajectoryItem = new PolyLine3D(this, coords, TRAJECTORY_COLOR, TRAJECTORY_LINE_WIDTH);
			addItem(trajectoryItem);
		} else {
			trajectoryItem.setCoords(coords);
		}

		if (vertexItem != null) {
			removeItem(vertexItem);
		}
		vertexItem = new Sphere(this, (float) vx, (float) vy, (float) vz, VERTEX_RADIUS, VERTEX_COLOR);
		addItem(vertexItem);

		refresh();
		return true;
	}

	/** Removes the current trajectory and vertex marker, if any. */
	void clearTrajectory() {
		if (trajectoryItem != null) {
			removeItem(trajectoryItem);
			trajectoryItem = null;
		}
		if (vertexItem != null) {
			removeItem(vertexItem);
			vertexItem = null;
		}
		refresh();
	}
}
