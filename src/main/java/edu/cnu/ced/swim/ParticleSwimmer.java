package edu.cnu.ced.swim;

import java.util.ArrayList;
import java.util.List;

import cnuphys.CLAS12Swim.CLAS12SwimResult;
import cnuphys.CLAS12Swim.CLAS12Swimmer;
import cnuphys.CLAS12Swim.CLAS12Trajectory;
import cnuphys.magfield.FieldProbe;

import edu.cnu.ced.data.RecEventData;
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
 */
public final class ParticleSwimmer {

	/**
	 * Default maximum path length to swim, in cm. Comfortably spans the
	 * CLAS12 forward detector stack out to FTOF/the calorimeters; a particle
	 * that curls tightly at low momentum will simply loop within a smaller
	 * region well before reaching this. Kept in sync with {@link
	 * edu.cnu.ced.swim.SwimRequestPolicy#FORWARD_MAX_PATH_CM}, the value the
	 * app actually uses; this one is the fallback for calling {@link
	 * #swim(RecEventData.Particle, FieldProbe)} directly.
	 */
	public static final double DEFAULT_MAX_PATH_LENGTH_CM = 1000.0;

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
	 * Swim a particle from its reconstruction vertex through the field, out
	 * to {@link #DEFAULT_MAX_PATH_LENGTH_CM}.
	 *
	 * @param particle the particle to swim (charge, momentum, vertex)
	 * @param probe    the magnetic field probe to swim through
	 * @return the swum trajectory as lab-frame points, oldest first; empty if
	 *         the swim didn't produce a usable trajectory (e.g. momentum
	 *         below the swimmer's internal threshold, or integration failure)
	 */
	public static List<Point3> swim(RecEventData.Particle particle, FieldProbe probe) {
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
	public static List<Point3> swim(RecEventData.Particle particle, FieldProbe probe, double maxPathLengthCm) {
		if (particle == null || probe == null) return List.of();
		float p = particle.p();
		if (!(p > 0f)) return List.of();

		CLAS12Swimmer swimmer = new CLAS12Swimmer(probe);
		CLAS12SwimResult result = swimmer.swim(particle.charge(),
				particle.vx(), particle.vy(), particle.vz(), p,
				Math.toDegrees(particle.theta()), Math.toDegrees(particle.phi()),
				maxPathLengthCm, INITIAL_STEP_CM, TOLERANCE_CM);

		if (!result.isSuccess()) return List.of();

		CLAS12Trajectory trajectory = result.getTrajectory();
		if (trajectory == null || trajectory.size() < 2) return List.of();

		List<Point3> points = new ArrayList<>(trajectory.size());
		for (int i = 0; i < trajectory.size(); i++) {
			double[] u = trajectory.get(i);
			points.add(new Point3(u[0], u[1], u[2]));
		}
		return points;
	}
}
