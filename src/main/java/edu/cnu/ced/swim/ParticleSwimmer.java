package edu.cnu.ced.swim;

import java.util.ArrayList;
import java.util.List;

import cnuphys.CLAS12Swim.CLAS12SwimResult;
import cnuphys.CLAS12Swim.CLAS12Swimmer;
import cnuphys.CLAS12Swim.CLAS12Trajectory;
import cnuphys.CLAS12Swim.geometry.Plane;
import cnuphys.magfield.FieldProbe;

import edu.cnu.ced.geometry.Point3;

/**
 * Swims a reconstructed particle through the CLAS12 magnetic field, producing
 * a lab-frame trajectory suitable for drawing.
 * <p>
 * Backed by {@link CLAS12Swimmer}, the Apache Commons Math ODE-based
 * successor to the older, hand-rolled {@code cnuphys.swim}/{@code
 * cnuphys.adaptiveSwim} integrators. {@code CLAS12Swimmer} is cheap to
 * construct (it just wraps a {@link FieldProbe} reference), so this creates
 * one per call rather than caching it, which also means a caller that swaps
 * its {@code FieldProbe} (e.g. after a field-map change) never swims through
 * a stale probe.
 * </p>
 * <p>
 * Alongside the plain "swim to a maximum path length" {@link #swim} used by
 * the production 2D/3D event views, this also exposes {@code
 * CLAS12Swimmer}'s own surface-stopping variants -- {@link #swimToFixedZ},
 * {@link #swimToFixedRho}, {@link #swimToPlane}, {@link #swimToCylinder} --
 * for callers (currently just {@code edu.cnu.ced.view.swim}'s standalone
 * swim-test view) that want a track stopped at a specific reference surface
 * rather than run out to a fixed path length.
 * </p>
 * <p>
 * Every method above returns an empty trajectory on failure (target missed,
 * momentum too low, integration error), which is exactly what a production
 * event view wants -- nothing to draw. The swim-test view's own randomized
 * batch tester wants the opposite for a failed swim: legacy CED's own
 * {@code SwimResultDrawer} still draws the partial path a failed swim
 * reached before giving up, in black, so a developer can see how close (or
 * not) it got. {@link #swimOutcome}/{@link #swimToFixedZOutcome}/{@link
 * #swimToFixedRhoOutcome}/{@link #swimToPlaneOutcome}/{@link
 * #swimToCylinderOutcome} are the same five swims, but returning an {@link
 * Outcome} that keeps the trajectory (confirmed non-null via {@code
 * CLAS12SwimResult.getTrajectory()}, empirically, even when {@code
 * isSuccess()} is {@code false}) alongside a success flag, rather than
 * discarding it.
 * </p>
 */
public final class ParticleSwimmer {

	/**
	 * Default maximum path length to swim, in cm. Comfortably spans the
	 * CLAS12 forward detector stack out to FTOF/the calorimeters; a particle
	 * that curls tightly at low momentum will simply loop within a smaller
	 * region well before reaching this. Kept in sync with {@link
	 * edu.cnu.ced.swim.SwimRequestPolicy#FORWARD_MAX_PATH_CM}, the value the
	 * app actually uses; this one is the fallback for calling {@link
	 * #swim(SwimmableParticle, FieldProbe)} directly.
	 */
	public static final double DEFAULT_MAX_PATH_LENGTH_CM = 1000.0;

	/** Default "reached the surface" accuracy, in cm, for the surface-stopping swims. */
	public static final double DEFAULT_SURFACE_ACCURACY_CM = 0.01;

	private static final double INITIAL_STEP_CM = 1.0;
	// CLAS12Swimmer uses this directly as the adaptive integrator's absolute
	// position tolerance, in cm, on every one of x/y/z at every step. 1e-4
	// (1 micron) is display-quality overkill by several orders of
	// magnitude -- a full screen pixel represents multiple cm at any normal
	// zoom -- and forces far more accepted steps than a visually smooth
	// curve needs, directly costing wall-clock time on every event change,
	// especially now that SwimRequestPolicy correctly gives every forward
	// particle the full 700cm path instead of the 150cm many were wrongly
	// truncated to before. 1e-2 (100 microns) is still two orders of
	// magnitude tighter than anything perceptible on screen.
	private static final double TOLERANCE_CM = 1.0e-2;

	private ParticleSwimmer() { }

	/**
	 * A swim's trajectory (lab-frame points, oldest first -- the partial
	 * path reached so far if {@code success} is {@code false}) alongside
	 * whether it actually succeeded. {@link #NONE} for a swim that never
	 * started at all (null probe/particle, non-positive momentum).
	 */
	public record Outcome(List<Point3> trajectory, boolean success) {
		public static final Outcome NONE = new Outcome(List.of(), false);
	}

	/**
	 * Swim a particle from its reconstruction vertex through the field, out
	 * to {@link #DEFAULT_MAX_PATH_LENGTH_CM}.
	 *
	 * @param particle the particle to swim (charge, momentum, vertex)
	 * @param probe    the magnetic field probe to swim through
	 * @return the swum trajectory as lab-frame points, oldest first; empty if
	 *         the swim didn't produce a usable trajectory (e.g. momentum
	 *         below the swimmer's internal threshold, or integration failure)
	 */
	public static List<Point3> swim(SwimmableParticle particle, FieldProbe probe) {
		return swim(particle, probe, DEFAULT_MAX_PATH_LENGTH_CM);
	}

	/**
	 * Swim a particle from its reconstruction vertex through the field.
	 *
	 * @param particle       the particle to swim (charge, momentum, vertex)
	 * @param probe          the magnetic field probe to swim through
	 * @param maxPathLengthCm maximum path length to swim, in cm
	 * @return the swum trajectory as lab-frame points, oldest first; empty if
	 *         the swim didn't produce a usable trajectory
	 */
	public static List<Point3> swim(SwimmableParticle particle, FieldProbe probe, double maxPathLengthCm) {
		if (particle == null || probe == null) return List.of();
		double p = particle.p();
		if (!(p > 0.0)) return List.of();

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swim(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return trajectoryOf(result);
	}

	/**
	 * Swim a particle until it reaches a fixed lab-frame z, or {@code
	 * maxPathLengthCm} is exhausted first.
	 *
	 * @param targetZCm    the target z, in cm
	 * @param accuracyCm   how close to {@code targetZCm} counts as "reached", in cm
	 * @param maxPathLengthCm maximum path length to swim, in cm
	 */
	public static List<Point3> swimToFixedZ(SwimmableParticle particle, FieldProbe probe,
			double targetZCm, double accuracyCm, double maxPathLengthCm) {
		if (particle == null || probe == null) return List.of();
		double p = particle.p();
		if (!(p > 0.0)) return List.of();

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swimZ(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				targetZCm, accuracyCm, maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return trajectoryOf(result);
	}

	/**
	 * Swim a particle until it reaches a fixed cylindrical radius (rho)
	 * about the z axis, or {@code maxPathLengthCm} is exhausted first.
	 *
	 * @param targetRhoCm  the target rho, in cm
	 * @param accuracyCm   how close to {@code targetRhoCm} counts as "reached", in cm
	 * @param maxPathLengthCm maximum path length to swim, in cm
	 */
	public static List<Point3> swimToFixedRho(SwimmableParticle particle, FieldProbe probe,
			double targetRhoCm, double accuracyCm, double maxPathLengthCm) {
		if (particle == null || probe == null) return List.of();
		double p = particle.p();
		if (!(p > 0.0)) return List.of();

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swimRho(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				targetRhoCm, accuracyCm, maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return trajectoryOf(result);
	}

	/**
	 * Swim a particle until it reaches an arbitrary plane, or {@code
	 * maxPathLengthCm} is exhausted first.
	 *
	 * @param normal       the plane's normal vector, {@code [nx, ny, nz]}
	 * @param point        a point in the plane, {@code [x, y, z]} cm
	 * @param accuracyCm   how close to the plane counts as "reached", in cm
	 * @param maxPathLengthCm maximum path length to swim, in cm
	 */
	public static List<Point3> swimToPlane(SwimmableParticle particle, FieldProbe probe,
			double[] normal, double[] point, double accuracyCm, double maxPathLengthCm) {
		if (particle == null || probe == null) return List.of();
		double p = particle.p();
		if (!(p > 0.0)) return List.of();

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		Plane plane = new Plane(normal, point);
		CLAS12SwimResult result = swimmer.swimPlane(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				plane, accuracyCm, maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return trajectoryOf(result);
	}

	/**
	 * Swim a particle until it reaches an arbitrary cylinder (defined by
	 * its center line's two endpoints and a radius), or {@code
	 * maxPathLengthCm} is exhausted first.
	 *
	 * @param centerLineP1 one endpoint of the cylinder's center line, {@code [x, y, z]} cm
	 * @param centerLineP2 the other endpoint, {@code [x, y, z]} cm
	 * @param radiusCm     the cylinder's radius, in cm
	 * @param accuracyCm   how close to the cylinder counts as "reached", in cm
	 * @param maxPathLengthCm maximum path length to swim, in cm
	 */
	public static List<Point3> swimToCylinder(SwimmableParticle particle, FieldProbe probe,
			double[] centerLineP1, double[] centerLineP2, double radiusCm,
			double accuracyCm, double maxPathLengthCm) {
		if (particle == null || probe == null) return List.of();
		double p = particle.p();
		if (!(p > 0.0)) return List.of();

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swimCylinder(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				centerLineP1, centerLineP2, radiusCm,
				accuracyCm, maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return trajectoryOf(result);
	}

	private static List<Point3> trajectoryOf(CLAS12SwimResult result) {
		if (result == null || !result.isSuccess()) return List.of();
		return pointsOf(result.getTrajectory());
	}

	private static Outcome outcomeOf(CLAS12SwimResult result) {
		if (result == null) return Outcome.NONE;
		return new Outcome(pointsOf(result.getTrajectory()), result.isSuccess());
	}

	private static List<Point3> pointsOf(CLAS12Trajectory trajectory) {
		if (trajectory == null || trajectory.size() < 2) return List.of();

		List<Point3> points = new ArrayList<>(trajectory.size());
		for (int i = 0; i < trajectory.size(); i++) {
			double[] u = trajectory.get(i);
			points.add(new Point3(u[0], u[1], u[2]));
		}
		return points;
	}

	/** Same as {@link #swim(SwimmableParticle, FieldProbe, double)}, but keeping the partial trajectory on failure. */
	public static Outcome swimOutcome(SwimmableParticle particle, FieldProbe probe, double maxPathLengthCm) {
		if (particle == null || probe == null) return Outcome.NONE;
		double p = particle.p();
		if (!(p > 0.0)) return Outcome.NONE;

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swim(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return outcomeOf(result);
	}

	/** Same as {@link #swimToFixedZ}, but keeping the partial trajectory on failure. */
	public static Outcome swimToFixedZOutcome(SwimmableParticle particle, FieldProbe probe,
			double targetZCm, double accuracyCm, double maxPathLengthCm) {
		if (particle == null || probe == null) return Outcome.NONE;
		double p = particle.p();
		if (!(p > 0.0)) return Outcome.NONE;

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swimZ(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				targetZCm, accuracyCm, maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return outcomeOf(result);
	}

	/** Same as {@link #swimToFixedRho}, but keeping the partial trajectory on failure. */
	public static Outcome swimToFixedRhoOutcome(SwimmableParticle particle, FieldProbe probe,
			double targetRhoCm, double accuracyCm, double maxPathLengthCm) {
		if (particle == null || probe == null) return Outcome.NONE;
		double p = particle.p();
		if (!(p > 0.0)) return Outcome.NONE;

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swimRho(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				targetRhoCm, accuracyCm, maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return outcomeOf(result);
	}

	/** Same as {@link #swimToPlane}, but keeping the partial trajectory on failure. */
	public static Outcome swimToPlaneOutcome(SwimmableParticle particle, FieldProbe probe,
			double[] normal, double[] point, double accuracyCm, double maxPathLengthCm) {
		if (particle == null || probe == null) return Outcome.NONE;
		double p = particle.p();
		if (!(p > 0.0)) return Outcome.NONE;

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		Plane plane = new Plane(normal, point);
		CLAS12SwimResult result = swimmer.swimPlane(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				plane, accuracyCm, maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return outcomeOf(result);
	}

	/** Same as {@link #swimToCylinder}, but keeping the partial trajectory on failure. */
	public static Outcome swimToCylinderOutcome(SwimmableParticle particle, FieldProbe probe,
			double[] centerLineP1, double[] centerLineP2, double radiusCm,
			double accuracyCm, double maxPathLengthCm) {
		if (particle == null || probe == null) return Outcome.NONE;
		double p = particle.p();
		if (!(p > 0.0)) return Outcome.NONE;

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swimCylinder(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				particle.thetaDeg(), particle.phiDeg(),
				centerLineP1, centerLineP2, radiusCm,
				accuracyCm, maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);
		return outcomeOf(result);
	}
}
