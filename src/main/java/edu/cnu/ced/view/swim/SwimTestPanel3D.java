package edu.cnu.ced.view.swim;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import cnuphys.CLAS12Swim.geometry.Plane;
import cnuphys.magfield.FieldProbe;

import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.swim.ParticleSwimmer;
import edu.cnu.ced.swim.SwimmableParticle;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cylinder;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.item3D.PolyLine3D;
import edu.cnu.mdi.mdi3D.item3D.Quad3D;
import edu.cnu.mdi.mdi3D.item3D.Sphere;
import edu.cnu.mdi.mdi3D.panel.Panel3D;

/**
 * The 3D scene for {@link SwimTestView3D}: an axis set, an optional
 * reference-surface visual aid, a manually-swum hypothetical particle's
 * trajectory and vertex, and every accumulated result from the
 * randomized batch tester ({@link SwimBatchDrawer3D}).
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
 * volumes (DC/FTOF/PCAL/ECAL, all unchecked by default there too) are
 * still deferred as a follow-up.
 * </p>
 */
final class SwimTestPanel3D extends Panel3D {

	private static final float XY_MAX = 600f;
	private static final float Z_MIN = -100f;
	private static final float Z_MAX = 600f;

	private static final Color TRAJECTORY_COLOR = Color.red;
	private static final Color VERTEX_COLOR = Color.blue;
	private static final Color SURFACE_COLOR = new Color(0, 0, 0, 24);
	private static final float TRAJECTORY_LINE_WIDTH = 2f;
	private static final float VERTEX_RADIUS = 3f;
	private static final float SURFACE_QUAD_SIZE = 800f;

	// None of these three are touched from addWest() (called from the
	// Panel3D superclass constructor, before this class's own fields
	// exist -- see CedPanel3D's own comment on the same hazard); all are
	// only ever created or updated from swim()/clearTrajectory(), called
	// well after full construction, from the control panel's button
	// listeners.
	private PolyLine3D trajectoryItem;
	private Sphere vertexItem;
	private Item3D surfaceItem;

	// Batch-tester state (see SwimBatchControlPanel/SwimBatchDrawer3D):
	// copy-on-write, matching this codebase's own convention for state a
	// GL-thread draw() reads while an EDT button listener writes it (e.g.
	// ForwardPanel3D's own event-data fields).
	private volatile List<SwimBatchResult> batchResults = List.of();
	private volatile SwimBatchShowMode batchShowMode = SwimBatchShowMode.ALL;

	SwimTestPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	@Override
	public void createInitialItems() {
		addItem(new Axes3D(this, -XY_MAX, XY_MAX, -XY_MAX, XY_MAX, Z_MIN, Z_MAX,
				new String[] { "x", "y", "z" }, Color.darkGray, 1f, 7, 7, 8,
				Color.black, new Color(0, 100, 0), new Font("SansSerif", Font.PLAIN, 10), 0));
		addItem(new SwimBatchDrawer3D(this));
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	@Override
	protected JComponent addWest() {
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Manual", new SwimTestControlPanel(this));
		tabs.addTab("Batch", new SwimBatchControlPanel(this));
		return tabs;
	}

	@Override
	protected JComponent addNorth() {
		JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
		north.add(new JLabel("Stand-alone swim test -- independent of any physics event."));
		return north;
	}

	/** A hypothetical particle swum straight out to the default max path length, with no reference surface. */
	boolean swim(int charge, double vx, double vy, double vz, double p, double thetaDeg, double phiDeg) {
		return swim(charge, vx, vy, vz, p, thetaDeg, phiDeg, SurfaceChoice.fullPath());
	}

	/**
	 * Swims a hypothetical particle with the given charge/vertex/momentum
	 * through the current magnetic field -- stopping at {@code surface} if
	 * it isn't {@link SurfaceType#FULL_PATH} -- and displays the result,
	 * along with a translucent visual aid for the surface itself.
	 *
	 * @return {@code true} if the swim produced a usable trajectory
	 */
	boolean swim(int charge, double vx, double vy, double vz, double p, double thetaDeg, double phiDeg,
			SurfaceChoice surface) {
		SwimmableParticle particle = new SwimmableParticle(0, charge, vx, vy, vz, p, thetaDeg, phiDeg, 0);
		FieldProbe probe = FieldProbe.factory();
		double maxPath = ParticleSwimmer.DEFAULT_MAX_PATH_LENGTH_CM;
		List<Point3> trajectory = switch (surface.type()) {
		case FULL_PATH -> ParticleSwimmer.swim(particle, probe, maxPath);
		case FIXED_Z -> ParticleSwimmer.swimToFixedZ(particle, probe, surface.fixedZCm(), surface.accuracyCm(), maxPath);
		case FIXED_RHO -> ParticleSwimmer.swimToFixedRho(particle, probe, surface.fixedRhoCm(), surface.accuracyCm(), maxPath);
		case PLANE -> ParticleSwimmer.swimToPlane(particle, probe, surface.planeNormal(), surface.planePoint(),
				surface.accuracyCm(), maxPath);
		case CYLINDER -> ParticleSwimmer.swimToCylinder(particle, probe, surface.cylinderP1(), surface.cylinderP2(),
				surface.cylinderRadiusCm(), surface.accuracyCm(), maxPath);
		};
		if (trajectory.isEmpty()) {
			return false;
		}

		updateSurfaceItem(surface);

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

	/**
	 * Replaces the reference-surface visual aid to match {@code surface},
	 * removing it entirely for {@link SurfaceType#FULL_PATH}. Rebuilt on
	 * every swim (not just when the selection changes), matching legacy
	 * CED's own {@code SwimmerControlPanel.setDisplayItem()}.
	 */
	private void updateSurfaceItem(SurfaceChoice surface) {
		if (surfaceItem != null) {
			removeItem(surfaceItem);
			surfaceItem = null;
		}
		switch (surface.type()) {
		case FULL_PATH -> {
		}
		case FIXED_Z -> surfaceItem = Quad3D.constantZQuad(this, (float) surface.fixedZCm(), SURFACE_QUAD_SIZE,
				SURFACE_COLOR, 1f, true);
		case FIXED_RHO -> surfaceItem = new Cylinder(this, 0f, 0f, Z_MIN, 0f, 0f, Z_MAX,
				(float) surface.fixedRhoCm(), SURFACE_COLOR);
		case PLANE -> {
			Plane plane = new Plane(surface.planeNormal(), surface.planePoint());
			surfaceItem = new Quad3D(this, plane.planeQuadCoordinates(SURFACE_QUAD_SIZE), SURFACE_COLOR, 1f, true);
		}
		case CYLINDER -> {
			double[] p1 = surface.cylinderP1();
			double[] p2 = surface.cylinderP2();
			surfaceItem = new Cylinder(this, (float) p1[0], (float) p1[1], (float) p1[2],
					(float) p2[0], (float) p2[1], (float) p2[2], (float) surface.cylinderRadiusCm(), SURFACE_COLOR);
		}
		}
		if (surfaceItem != null) {
			addItem(surfaceItem);
		}
	}

	/** Removes the current trajectory and vertex marker, if any. Leaves the reference-surface visual aid alone. */
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

	/**
	 * Swims a whole batch of hypothetical particles at once -- each
	 * stopped at {@code surface}, same as {@link #swim} -- appending
	 * every one that produced a usable (even if unsuccessful) trajectory
	 * to the accumulated batch results, updating the reference-surface
	 * visual aid once, and refreshing once at the end. Matches legacy
	 * CED's own {@code SwimmerControlPanel.handleSwim()}: one {@code
	 * setDisplayItem()} and one {@code refresh()} per "Swim Trajectories"
	 * click, not per individual swim.
	 *
	 * @return how many of {@code specs} actually reached {@code surface}
	 *         (or completed the full path, for {@link SurfaceType#FULL_PATH})
	 */
	int runBatch(List<SwimSpec> specs, SurfaceChoice surface) {
		updateSurfaceItem(surface);

		FieldProbe probe = FieldProbe.factory();
		double maxPath = ParticleSwimmer.DEFAULT_MAX_PATH_LENGTH_CM;
		List<SwimBatchResult> appended = new ArrayList<>(batchResults);
		int successes = 0;
		for (SwimSpec spec : specs) {
			SwimmableParticle particle = new SwimmableParticle(0, spec.charge(), spec.vx(), spec.vy(), spec.vz(),
					spec.p(), spec.thetaDeg(), spec.phiDeg(), 0);
			ParticleSwimmer.Outcome outcome = switch (surface.type()) {
			case FULL_PATH -> ParticleSwimmer.swimOutcome(particle, probe, maxPath);
			case FIXED_Z -> ParticleSwimmer.swimToFixedZOutcome(particle, probe, surface.fixedZCm(), surface.accuracyCm(), maxPath);
			case FIXED_RHO -> ParticleSwimmer.swimToFixedRhoOutcome(particle, probe, surface.fixedRhoCm(), surface.accuracyCm(), maxPath);
			case PLANE -> ParticleSwimmer.swimToPlaneOutcome(particle, probe, surface.planeNormal(), surface.planePoint(),
					surface.accuracyCm(), maxPath);
			case CYLINDER -> ParticleSwimmer.swimToCylinderOutcome(particle, probe, surface.cylinderP1(), surface.cylinderP2(),
					surface.cylinderRadiusCm(), surface.accuracyCm(), maxPath);
			};
			if (outcome.success()) {
				successes++;
			}
			if (!outcome.trajectory().isEmpty()) {
				appended.add(new SwimBatchResult(outcome.trajectory(), spec.charge(), outcome.success()));
			}
		}
		batchResults = List.copyOf(appended);
		refresh();
		return successes;
	}

	/** Empties the accumulated batch results. Leaves the reference-surface visual aid and the manual trajectory alone. */
	void clearBatch() {
		batchResults = List.of();
		refresh();
	}

	/** Re-filters which accumulated batch results {@link SwimBatchDrawer3D} draws, without re-swimming. */
	void setBatchShowMode(SwimBatchShowMode mode) {
		this.batchShowMode = mode;
		refresh();
	}

	List<SwimBatchResult> batchResults() {
		return batchResults;
	}

	SwimBatchShowMode batchShowMode() {
		return batchShowMode;
	}

	/** Which stopping condition a swim uses. */
	enum SurfaceType {
		FULL_PATH, FIXED_Z, FIXED_RHO, PLANE, CYLINDER
	}

	/**
	 * Everything needed to swim to (and draw a visual aid for) one
	 * reference surface. Fields irrelevant to {@link #type} are ignored.
	 */
	record SurfaceChoice(SurfaceType type, double fixedZCm, double fixedRhoCm,
			double[] planeNormal, double[] planePoint,
			double[] cylinderP1, double[] cylinderP2, double cylinderRadiusCm,
			double accuracyCm) {

		static SurfaceChoice fullPath() {
			return new SurfaceChoice(SurfaceType.FULL_PATH, 0, 0, null, null, null, null, 0, 0);
		}
	}
}
