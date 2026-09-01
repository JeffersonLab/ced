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
 * {@code REC::Particle.status}'s <em>sign</em> does NOT mean detector
 * region -- an earlier version of this class assumed it did (negative =
 * Central), which was wrong and, for a forward-going trigger particle
 * (typically the electron), silently truncated its swim to
 * {@link #CENTRAL_MAX_PATH_CM} well short of the actual forward detector
 * stack. Per coatjava's own
 * {@code org.jlab.clas.detector.DetectorParticleStatus} (the class that
 * actually assembles this field): "Negative means it's the one used to
 * determine start time" -- i.e. the trigger particle, unrelated to detector
 * region. The region is instead a bitmask on {@code abs(status) / 1000}:
 * {@code FORWARD = 2}, {@code CENTRAL = 4} (lower bits below 1000 encode
 * scintillator/calorimeter/Cherenkov counts and aren't needed here). A
 * particle can carry both bits (e.g. a combined CVT+Forward track); this
 * treats it as needing the longer forward swim, since forward hits are
 * farther from the vertex than central ones. A particle with neither bit
 * set (unassigned, or FT-tagger-only) also defaults to the forward path,
 * matching the previous non-negative-status fallback.
 * </p>
 */
public final class SwimRequestPolicy {

	/** Maximum path length for Forward Detector tracks, cm; spans the forward detector stack. */
	public static final double FORWARD_MAX_PATH_CM = 700.0;

	/** Maximum path length for Central Detector tracks, cm. */
	public static final double CENTRAL_MAX_PATH_CM = 150.0;

	// Mirrors org.jlab.clas.detector.DetectorParticleStatus's REGION/FORWARD/CENTRAL
	// constants (coatjava's common-tools/clas-reco module).
	private static final int REGION_DIVISOR = 1000;
	private static final int FORWARD_BIT = 2;
	private static final int CENTRAL_BIT = 4;

	private SwimRequestPolicy() { }

	/**
	 * @param particle the particle to swim, or {@code null}
	 * @return the maximum path length to swim this particle, in cm
	 */
	public static double maxPathLengthCm(RecEventData.Particle particle) {
		if (particle == null) return FORWARD_MAX_PATH_CM;
		int region = Math.abs(particle.status()) / REGION_DIVISOR;
		boolean forward = (region & FORWARD_BIT) != 0;
		boolean central = (region & CENTRAL_BIT) != 0;
		return (central && !forward) ? CENTRAL_MAX_PATH_CM : FORWARD_MAX_PATH_CM;
	}
}
