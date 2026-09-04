package edu.cnu.ced.style;

import java.awt.Color;
import java.awt.Stroke;

import cnuphys.lund.LundId;
import cnuphys.lund.LundStyle;
import cnuphys.lund.LundSupport;

import edu.cnu.ced.data.DCEventData.ReconKind;

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

	private CedDrawingStyle() {
	}

	/**
	 * Returns the display color for a reconstructed particle, from
	 * coatjava's own {@code cnuphys.lund.LundStyle} -- the same
	 * per-species palette used elsewhere across the cnuphys/CLAS12
	 * ecosystem (e.g. e- red, proton blue, photon deep-sky-blue, pi+/pi-
	 * distinct purples), rather than a locally invented one. Sharing it
	 * avoids exactly the kind of mix-up a locally invented palette risks:
	 * comparing this display against another cnuphys-based tool (or a
	 * screenshot of one) and mistaking one species' track for another's
	 * because the two tools colored them differently.
	 *
	 * @param pid    PDG/Lund particle id; {@code 0} means reconstruction could
	 *               not assign one
	 * @param charge the particle's charge, used only for unrecognized PIDs
	 * @return the species color ({@code LundStyle}'s own line color for an
	 *         unrecognized PID: black/white/gray by charge)
	 */
	public static Color particleColor(int pid, int charge) {
		return lundStyle(pid, charge).getLineColor();
	}

	/**
	 * Returns the display stroke for a reconstructed particle, from
	 * coatjava's own {@code cnuphys.lund.LundStyle}: solid for a charged
	 * particle, dashed for a neutral one -- the same convention used
	 * elsewhere across the cnuphys/CLAS12 ecosystem, so a neutral
	 * particle's track reads as dashed here too.
	 *
	 * @param pid    PDG/Lund particle id; {@code 0} means reconstruction could
	 *               not assign one
	 * @param charge the particle's charge
	 * @return the species stroke
	 */
	public static Stroke particleStroke(int pid, int charge) {
		return lundStyle(pid, charge).getStroke();
	}

	private static LundStyle lundStyle(int pid, int charge) {
		return LundStyle.getStyle(lundId(pid, charge));
	}

	/**
	 * Resolve a reconstructed particle's PDG/Lund id and charge to a {@link
	 * LundId} (name, mass, charge) -- public so other track/particle tables
	 * (e.g. the Monte Carlo Tracks and Reconstructed Tracks views) can share
	 * this exact resolution, not just drawing style.
	 * <p>
	 * {@code LundSupport.get(id, charge)} only falls back to a charge-keyed
	 * "unknown" id when {@code id} isn't found in its registry at all -- but
	 * {@code pid == 0} (this codebase's own "reconstruction couldn't assign
	 * one" convention) IS a registered id there, {@code LundSupport
	 * .unknownPlus}, so every {@code pid == 0} particle would otherwise come
	 * back with that one fixed (positive-charge) style regardless of this
	 * particle's actual charge. Route {@code pid == 0} to the matching
	 * {@code unknownPlus}/{@code unknownMinus}/{@code unknownNeutral}
	 * constant by charge directly instead.
	 */
	public static LundId lundId(int pid, int charge) {
		if (pid == 0) {
			if (charge > 0) return LundSupport.unknownPlus;
			if (charge < 0) return LundSupport.unknownMinus;
			return LundSupport.unknownNeutral;
		}
		return LundSupport.getInstance().get(pid, charge);
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
