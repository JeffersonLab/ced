package edu.cnu.ced.swim;

import java.util.ArrayList;
import java.util.List;

import cnuphys.magfield.FieldProbe;

import edu.cnu.ced.geometry.Point3;

/**
 * Cumulative magnetic-field integral along an already-swum trajectory --
 * matches legacy CED's own {@code TrajectoryIntegralPlotter}, a diagnostic
 * for a track's actual bending power (and so, indirectly, its momentum
 * resolution) that a swum trajectory's shape alone doesn't make obvious.
 * <p>
 * Each segment between consecutive trajectory points contributes
 * |B &times; dL|, evaluated at the segment's midpoint and accumulated as a
 * running sum -- the same Riemann-sum approach legacy uses, not a higher-
 * order quadrature, since the trajectory is already densely sampled by the
 * swimmer itself.
 * </p>
 * <p>
 * Units: trajectory points (and so path length) are in cm, matching every
 * other geometry quantity in this codebase; {@link FieldProbe#field} returns
 * kG (confirmed against {@code SectorView}'s own field-magnitude display,
 * which divides by 10 to get Tesla) -- so the accumulated integral is in
 * kG&middot;cm, not legacy's kG-m (this codebase's own cm convention is kept
 * rather than introducing a second unit system just for this one plot).
 * </p>
 */
public final class FieldIntegral {

	private FieldIntegral() { }

	/**
	 * One sample point of the integral curve.
	 *
	 * @param pathLengthCm         cumulative path length from the trajectory's start, cm
	 * @param cumulativeIntegralKgCm cumulative &int;|B &times; dL|, kG&middot;cm
	 */
	public record Sample(double pathLengthCm, double cumulativeIntegralKgCm) { }

	/**
	 * Computes the cumulative field integral along a trajectory.
	 *
	 * @param trajectory lab-frame trajectory points, oldest first (as returned
	 *                   by {@link SwimTrajectoryCache#trajectory})
	 * @param probe      the magnetic field probe to sample; must not be {@code null}
	 * @return one sample per trajectory point (path length 0 at the first),
	 *         or empty if {@code trajectory} has fewer than two points
	 */
	public static List<Sample> compute(List<Point3> trajectory, FieldProbe probe) {
		if (trajectory == null || trajectory.size() < 2) return List.of();
		List<Sample> samples = new ArrayList<>(trajectory.size());
		samples.add(new Sample(0, 0));
		double pathLength = 0;
		double integral = 0;
		float[] field = new float[3];
		for (int i = 1; i < trajectory.size(); i++) {
			Point3 previous = trajectory.get(i - 1);
			Point3 current = trajectory.get(i);
			double dx = current.x() - previous.x();
			double dy = current.y() - previous.y();
			double dz = current.z() - previous.z();
			pathLength += Math.sqrt(dx * dx + dy * dy + dz * dz);

			float midX = (float) ((previous.x() + current.x()) / 2);
			float midY = (float) ((previous.y() + current.y()) / 2);
			float midZ = (float) ((previous.z() + current.z()) / 2);
			probe.field(midX, midY, midZ, field);

			// (B x dL): field[0..2] = (Bx, By, Bz) -- matches legacy's own
			// TrajectoryIntegralPlotter.fieldIntegralSamples exactly.
			double bx = field[1] * dz - field[2] * dy;
			double by = field[2] * dx - field[0] * dz;
			double bz = field[0] * dy - field[1] * dx;
			integral += Math.sqrt(bx * bx + by * by + bz * bz);

			samples.add(new Sample(pathLength, integral));
		}
		return List.copyOf(samples);
	}
}
