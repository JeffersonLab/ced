package edu.cnu.ced.style;

import java.awt.Color;

import edu.cnu.ced.data.DCEventData.ReconKind;
import edu.cnu.ced.data.ParticleId;

/** Shared semantic colors used by CED detector and reconstruction drawings. */
public final class CedDrawingStyle {

	public static final Color RAW_HIT = new Color(225, 35, 25);
	public static final Color HIT_BASED = Color.YELLOW;
	public static final Color TIME_BASED = new Color(255, 140, 0);
	public static final Color AI_HIT_BASED = new Color(0, 255, 127);
	public static final Color AI_TIME_BASED = Color.MAGENTA;

	public static final Color RECON_HIT = RAW_HIT;
	public static final Color RECON_CLUSTER = new Color(205, 0, 205);
	public static final Color RECON_CROSS = new Color(20, 145, 35);

	// Reconstructed-particle colors, grouped by species family. There is no
	// universal convention here; this palette just keeps families visually
	// distinct from each other and from the hit/cluster/cross colors above.
	public static final Color PARTICLE_LEPTON = new Color(30, 120, 255);
	public static final Color PARTICLE_PHOTON = new Color(255, 215, 0);
	public static final Color PARTICLE_NUCLEON = new Color(220, 30, 30);
	public static final Color PARTICLE_NEUTRAL_BARYON = new Color(120, 120, 120);
	public static final Color PARTICLE_PION = new Color(0, 160, 60);
	public static final Color PARTICLE_KAON = new Color(160, 30, 200);
	public static final Color PARTICLE_NUCLEUS = new Color(255, 120, 0);
	public static final Color PARTICLE_UNKNOWN = new Color(90, 90, 90);

	private CedDrawingStyle() {
	}

	/**
	 * Returns the display color for a reconstructed particle, grouped by
	 * species family (see the {@code PARTICLE_*} constants).
	 *
	 * @param pid    PDG/Lund particle id; {@code 0} means reconstruction could
	 *               not assign one
	 * @param charge the particle's charge, used only for unrecognized PIDs
	 * @return the family color, or {@link #PARTICLE_UNKNOWN} for an
	 *         unrecognized PID
	 */
	public static Color particleColor(int pid, int charge) {
		if (!ParticleId.isKnown(pid)) return PARTICLE_UNKNOWN;
		return switch (pid) {
		case 11, -11, 13, -13 -> PARTICLE_LEPTON;
		case 22 -> PARTICLE_PHOTON;
		case 2212, -2212 -> PARTICLE_NUCLEON;
		case 2112, -2112 -> PARTICLE_NEUTRAL_BARYON;
		case 211, -211, 111 -> PARTICLE_PION;
		case 321, -321, 311, 310, 130 -> PARTICLE_KAON;
		case 45 -> PARTICLE_NUCLEUS;
		default -> PARTICLE_UNKNOWN;
		};
	}

	public static Color reconstructionColor(ReconKind kind) {
		return switch (kind) {
		case HB -> HIT_BASED;
		case TB -> TIME_BASED;
		case AI_HB -> AI_HIT_BASED;
		case AI_TB -> AI_TIME_BASED;
		};
	}

	public static Color outline(Color color) {
		return color.darker();
	}

	public static Color translucent(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}
