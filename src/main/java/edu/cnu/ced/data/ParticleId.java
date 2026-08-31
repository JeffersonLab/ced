package edu.cnu.ced.data;

import java.util.Map;

/**
 * Maps a PDG/Lund particle id (as found in {@code REC::Particle.pid}) to a
 * short display name.
 * <p>
 * Covers the final-state species that actually appear in CLAS12
 * reconstruction output: leptons, the photon, and the common mesons and
 * baryons. An unrecognized or zero PID (reconstruction could not assign one)
 * falls back to a generic name based on charge, so every particle still gets
 * a sensible label.
 * </p>
 */
public final class ParticleId {

	private static final Map<Integer, String> NAMES = Map.ofEntries(
			Map.entry(11, "e-"), Map.entry(-11, "e+"),
			Map.entry(13, "mu-"), Map.entry(-13, "mu+"),
			Map.entry(22, "gamma"),
			Map.entry(2212, "p"), Map.entry(-2212, "pbar"),
			Map.entry(2112, "n"), Map.entry(-2112, "nbar"),
			Map.entry(211, "pi+"), Map.entry(-211, "pi-"), Map.entry(111, "pi0"),
			Map.entry(321, "K+"), Map.entry(-321, "K-"),
			Map.entry(311, "K0"), Map.entry(310, "K0S"), Map.entry(130, "K0L"),
			Map.entry(45, "d"));

	private ParticleId() { }

	/**
	 * Returns a short display name for a reconstructed particle.
	 *
	 * @param pid    PDG/Lund particle id; {@code 0} means reconstruction could
	 *               not assign one
	 * @param charge the particle's charge, used for the fallback name when
	 *               {@code pid} is zero or unrecognized
	 * @return a short display name, never {@code null} or blank
	 */
	public static String name(int pid, int charge) {
		String known = NAMES.get(pid);
		if (known != null) return known;
		if (charge > 0) return "unknown+";
		if (charge < 0) return "unknown-";
		return "unknown0";
	}

	/** @return {@code true} if {@code pid} is one of the recognized species */
	public static boolean isKnown(int pid) {
		return NAMES.containsKey(pid);
	}
}
