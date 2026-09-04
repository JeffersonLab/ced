package edu.cnu.ced.swim;

import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.TrackRow;

/**
 * Everything {@link ParticleSwimmer} and {@link SwimRequestPolicy} need to
 * swim one track through the field, independent of which bank it came from.
 * <p>
 * Both a reconstructed {@link RecEventData.Particle} and a Monte Carlo
 * {@link TrackRow} carry a charge, a vertex, and a momentum -- the only
 * inputs {@code CLAS12Swimmer} itself needs -- so a single shared shape lets
 * the swim pipeline (cache, swimmer, path-length policy) work for both
 * without caring which one it was handed. Momentum is stored as
 * (magnitude, theta-degrees, phi-degrees), matching both {@code
 * CLAS12Swimmer.swim}'s own parameter shape and how {@link TrackRow} already
 * stores its kinematics, so building one from a {@code TrackRow} needs only
 * a unit conversion, never a lossy trig round-trip through Cartesian
 * momentum.
 * </p>
 *
 * @param pid      PDG/Lund particle id
 * @param charge   charge, in units of {@code e}
 * @param vx       vertex x, cm
 * @param vy       vertex y, cm
 * @param vz       vertex z, cm
 * @param p        total momentum magnitude, GeV/c
 * @param thetaDeg polar angle from the beam axis, degrees
 * @param phiDeg   azimuthal angle, degrees
 * @param status   reconstruction/tracking status word, for {@link
 *                 SwimRequestPolicy}; {@code 0} where the source has none
 *                 (e.g. Monte Carlo truth), which already falls through to
 *                 that policy's "unassigned" default
 */
public record SwimmableParticle(int pid, int charge, double vx, double vy, double vz,
		double p, double thetaDeg, double phiDeg, int status) {

	public static SwimmableParticle of(RecEventData.Particle particle) {
		return new SwimmableParticle(particle.pid(), particle.charge(),
				particle.vx(), particle.vy(), particle.vz(),
				particle.p(), Math.toDegrees(particle.theta()), Math.toDegrees(particle.phi()),
				particle.status());
	}

	public static SwimmableParticle of(TrackRow track) {
		return new SwimmableParticle(track.pid(), track.charge(), track.x0(), track.y0(), track.z0(),
				track.momentumMeV() / 1000.0, track.thetaDeg(), track.phiDeg(), track.status());
	}
}
