package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/**
 * Immutable reconstructed final-state particle list, extracted directly from
 * one atomic event snapshot's {@code REC::Particle} bank.
 * <p>
 * Unlike the per-detector event data classes, particles are not tied to a
 * sector/panel/paddle geometry: each row is one reconstructed final-state
 * particle with a PID, charge, momentum, and vertex.
 * </p>
 */
public record RecEventData(List<Particle> particles) {

	public static final String RECON_BANK = "REC::Particle";

	private static final RecEventData EMPTY = new RecEventData(List.of());

	public RecEventData {
		particles = List.copyOf(particles);
	}

	public static RecEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) return EMPTY;
		ArrayList<Particle> particles = new ArrayList<>();
		readParticles(snapshot.bank(RECON_BANK).orElse(null), particles);
		return particles.isEmpty() ? EMPTY : new RecEventData(particles);
	}

	private static void readParticles(DataBank bank, List<Particle> destination) {
		if (!hasColumns(bank, "pid", "charge", "px", "py", "pz", "vx", "vy", "vz", "status")) return;
		boolean hasVt = BankAccess.hasColumn(bank, "vt");
		boolean hasBeta = BankAccess.hasColumn(bank, "beta");
		boolean hasChi2Pid = BankAccess.hasColumn(bank, "chi2pid");
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new Particle(row, bank.getInt("pid", row), bank.getByte("charge", row),
					bank.getFloat("px", row), bank.getFloat("py", row), bank.getFloat("pz", row),
					bank.getFloat("vx", row), bank.getFloat("vy", row), bank.getFloat("vz", row),
					hasVt ? bank.getFloat("vt", row) : 0f,
					hasBeta ? bank.getFloat("beta", row) : 0f,
					hasChi2Pid ? bank.getFloat("chi2pid", row) : 0f,
					bank.getShort("status", row)));
		}
	}

	private static boolean hasColumns(DataBank bank, String... names) {
		if (bank == null) return false;
		for (String name : names) if (!BankAccess.hasColumn(bank, name)) return false;
		return true;
	}

	/**
	 * One reconstructed final-state particle.
	 *
	 * @param row     row index in the {@code REC::Particle} bank
	 * @param pid     PDG/Lund particle id; {@code 0} if the reconstruction
	 *                could not assign one (see {@link ParticleId})
	 * @param charge  particle charge in units of {@code e}
	 * @param px      x momentum component, GeV/c
	 * @param py      y momentum component, GeV/c
	 * @param pz      z momentum component, GeV/c
	 * @param vx      vertex x, cm
	 * @param vy      vertex y, cm
	 * @param vz      vertex z, cm
	 * @param vt      vertex time, ns; {@code 0} if the bank has no {@code vt} column
	 * @param beta    reconstructed velocity as a fraction of {@code c};
	 *                {@code 0} if the bank has no {@code beta} column
	 * @param chi2pid PID hypothesis chi-square; {@code 0} if the bank has no
	 *                {@code chi2pid} column
	 * @param status  reconstruction status word (detector-origin encoded in sign/magnitude)
	 */
	public record Particle(int row, int pid, int charge, float px, float py, float pz,
			float vx, float vy, float vz, float vt, float beta, float chi2pid, int status) {

		/** @return total momentum magnitude, GeV/c */
		public float p() {
			return (float) Math.sqrt(px * (double) px + py * (double) py + pz * (double) pz);
		}

		/** @return polar angle from the beam (+z) axis, radians in [0, pi] */
		public float theta() {
			return (float) Math.acos(clampCosTheta());
		}

		/** @return azimuthal angle, radians in (-pi, pi], measured from +x toward +y */
		public float phi() {
			return (float) Math.atan2(py, px);
		}

		private double clampCosTheta() {
			float p = p();
			if (p <= 0f) return 1.0;
			double cosTheta = pz / (double) p;
			return Math.max(-1.0, Math.min(1.0, cosTheta));
		}

		/** @return a short display name for this particle, e.g. {@code "p"}, {@code "pi+"} */
		public String displayName() {
			return ParticleId.name(pid, charge);
		}

		/**
		 * The CLAS12 sector [1, 6] this particle's momentum direction points
		 * into, using the standard 60-degree-wide sector convention: sector 1
		 * is centered on {@code phi = 0} and spans {@code (-30, 30]} degrees,
		 * with sectors 2 through 6 following counterclockwise in 60-degree
		 * steps. Momentum direction is used rather than vertex position,
		 * since the vertex sits near the beamline (radius near zero) and
		 * carries no useful azimuthal information; this matches how bCNU
		 * CED's {@code GeometryManager.getSector(phi)} determines sector
		 * membership.
		 *
		 * @return the sector number, 1 through 6
		 */
		public int sector() {
			double degrees = Math.toDegrees(phi());
			degrees = ((degrees % 360.0) + 360.0) % 360.0;
			if (degrees > 30.0 && degrees <= 90.0) return 2;
			if (degrees > 90.0 && degrees <= 150.0) return 3;
			if (degrees > 150.0 && degrees <= 210.0) return 4;
			if (degrees > 210.0 && degrees <= 270.0) return 5;
			if (degrees > 270.0 && degrees <= 330.0) return 6;
			return 1;
		}
	}
}
