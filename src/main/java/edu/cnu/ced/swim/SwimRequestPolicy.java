package edu.cnu.ced.swim;

import edu.cnu.ced.data.RecEventData;

/**
 * Chooses the maximum path length to swim a reconstructed particle.
 * <p>
 * CLAS12's Central Detector (CVT: BST + BMT, plus CTOF/CND) covers a much
 * smaller radius than the Forward Detector stack, so a Central Detector
 * track only needs a short swim to cross it, while a Forward Detector track
 * needs a much longer one to reach FTOF/the calorimeters.
 * </p>
 * <p>
 * {@code REC::Particle.status} encodes which detector group produced the
 * track: by the standard CLAS12 convention, a negative status means Central
 * Detector tracking and a positive status means Forward Detector tracking.
 * This is a widely used convention but hasn't been cross-checked against a
 * live bank in this codebase; if particle trajectories ever look
 * systematically too short or too long for one detector group, this is the
 * first place to check.
 * </p>
 */
public final class SwimRequestPolicy {

	/** Maximum path length for Forward Detector tracks, cm; spans the forward detector stack. */
	public static final double FORWARD_MAX_PATH_CM = 700.0;

	/** Maximum path length for Central Detector tracks, cm. */
	public static final double CENTRAL_MAX_PATH_CM = 150.0;

	private SwimRequestPolicy() { }

	/**
	 * @param particle the particle to swim, or {@code null}
	 * @return the maximum path length to swim this particle, in cm
	 */
	public static double maxPathLengthCm(RecEventData.Particle particle) {
		return (particle != null && particle.status() < 0) ? CENTRAL_MAX_PATH_CM : FORWARD_MAX_PATH_CM;
	}
}
