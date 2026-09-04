package edu.cnu.ced.data;

import java.util.Optional;
import java.util.Set;

import cnuphys.lund.LundId;

/**
 * One row of a trajectory/track table -- Monte Carlo Tracks or Reconstructed
 * Tracks -- with every kinematic quantity already converted to the units
 * legacy CED's own trajectory table showed them in (MeV, cm, degrees), so
 * the table itself only formats, never computes.
 *
 * @param trackId a bank-specific row/track identifier; not unique across sources
 * @param particle the resolved species (name/mass/charge); for a track with
 *        no real PID (a DC or CVT candidate track), one of {@code
 *        LundSupport}'s synthetic hit/time/CVT-based placeholders -- see
 *        {@link #isSyntheticPid()}
 * @param x0 vertex x, cm
 * @param y0 vertex y, cm
 * @param z0 vertex z, cm
 * @param momentumMeV total momentum magnitude, MeV/c
 * @param thetaDeg polar angle from the beam axis, degrees
 * @param phiDeg azimuthal angle, degrees
 * @param status reconstruction/tracking status word; {@code 0} where the source has none
 * @param source the bank name this row came from, e.g. {@code "REC::Particle"}
 */
public record TrackRow(int trackId, LundId particle, double x0, double y0, double z0,
		double momentumMeV, double thetaDeg, double phiDeg, int status, String source) {

	// Placeholder ids for track candidates with no real PID -- LundSupport's
	// own getTrackbased/getHitbased/getCVTbased(charge) -- plus the pid==0
	// "reconstruction couldn't identify" sentinels CedDrawingStyle.lundId
	// resolves to unknownPlus/Minus/Neutral (ids 0, -1, -2). None of these
	// name a real particle, so a table should show "---" for PID rather than
	// a number that looks meaningful but isn't.
	private static final Set<Integer> SYNTHETIC_PIDS =
			Set.of(0, -1, -2, -99, -100, -101, -199, -200, -201, -299, -300, -301);

	/**
	 * Build a row from Cartesian momentum (GeV/c) and vertex (cm), matching
	 * legacy CED's {@code TrackKinematics.fromMomentum}: empty if the
	 * momentum is non-finite or non-positive (can't derive a direction),
	 * otherwise MeV/degrees as this record stores them.
	 */
	public static Optional<TrackRow> fromMomentum(int trackId, LundId particle, double x0, double y0, double z0,
			double px, double py, double pz, int status, String source) {
		if (!Double.isFinite(px) || !Double.isFinite(py) || !Double.isFinite(pz)) {
			return Optional.empty();
		}
		double momentum = Math.sqrt(px * px + py * py + pz * pz);
		if (!Double.isFinite(momentum) || momentum <= 0.0) {
			return Optional.empty();
		}
		double cosTheta = Math.max(-1.0, Math.min(1.0, pz / momentum));
		double thetaDeg = Math.toDegrees(Math.acos(cosTheta));
		double phiDeg = Math.toDegrees(Math.atan2(py, px));
		return Optional.of(new TrackRow(trackId, particle, x0, y0, z0, 1000.0 * momentum, thetaDeg, phiDeg,
				status, source));
	}

	/** @return the resolved PDG/Lund id */
	public int pid() {
		return particle.getId();
	}

	/** @return whether {@link #pid()} is a placeholder rather than a real, identified species */
	public boolean isSyntheticPid() {
		return SYNTHETIC_PIDS.contains(pid());
	}

	/** @return the resolved species' short display name, e.g. {@code "pi+"} */
	public String name() {
		return particle.getName();
	}

	/** @return the resolved species' mass, MeV/c^2 */
	public double massMeV() {
		return particle.getMass() * 1000.0;
	}

	/** @return the resolved species' charge, in units of e */
	public int charge() {
		return particle.getCharge();
	}

	/** @return total energy, MeV -- sqrt(p^2 + m^2) */
	public double totalEnergyMeV() {
		return Math.sqrt(momentumMeV * momentumMeV + massMeV() * massMeV());
	}

	/** @return kinetic energy, MeV -- total energy minus rest mass */
	public double kineticEnergyMeV() {
		return totalEnergyMeV() - massMeV();
	}
}
