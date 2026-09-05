package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import cnuphys.lund.LundId;
import cnuphys.lund.LundSupport;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.style.CedDrawingStyle;

/**
 * Reconstructed track candidates and particles for one event, gathered from
 * every source legacy CED's {@code ClasIoReconEventView} does, in the same
 * order: DC hit-based, DC time-based, {@code REC::Particle}, DC AI hit-based,
 * DC AI time-based, then CVT reconstructed and CVT pass1. A DC or CVT track
 * carries a synthetic placeholder species (see {@link TrackRow#isSyntheticPid()})
 * since those banks have no real PID, only a fit charge.
 */
public record ReconstructedTracks(List<TrackRow> tracks) {

	private static final ReconstructedTracks EMPTY = new ReconstructedTracks(List.of());

	public ReconstructedTracks {
		tracks = List.copyOf(tracks);
	}

	public static final String HB_TRACK_BANK = "HitBasedTrkg::HBTracks";
	public static final String TB_TRACK_BANK = "TimeBasedTrkg::TBTracks";

	public static ReconstructedTracks from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return EMPTY;
		}
		List<TrackRow> rows = new ArrayList<>();
		addDcTracks(snapshot, HB_TRACK_BANK, true, rows);
		addDcTracks(snapshot, TB_TRACK_BANK, false, rows);
		addRecParticles(snapshot, rows);
		addDcTracks(snapshot, "HitBasedTrkg::AITracks", true, rows);
		addDcTracks(snapshot, "TimeBasedTrkg::AITracks", false, rows);
		addCvtTracks(snapshot, "CVTRec::Tracks", rows);
		addCvtTracks(snapshot, "CVT::Tracks", rows);
		return rows.isEmpty() ? EMPTY : new ReconstructedTracks(rows);
	}

	/**
	 * Just the DC hit-based track candidates ({@value #HB_TRACK_BANK}), for a
	 * detector view that wants to draw them on their own rather than as part
	 * of the full {@link #from} aggregation -- e.g. a per-view "HB Tracks"
	 * display toggle, drawn in {@code LundSupport}'s customary hit-based
	 * yellow (see {@code CedDrawingStyle#particleColor}).
	 */
	public static List<TrackRow> hbTracks(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return List.of();
		}
		List<TrackRow> rows = new ArrayList<>();
		addDcTracks(snapshot, HB_TRACK_BANK, true, rows);
		return rows;
	}

	/**
	 * Just the DC time-based track candidates ({@value #TB_TRACK_BANK}), the
	 * time-based counterpart to {@link #hbTracks} -- customary dark-orange.
	 */
	public static List<TrackRow> tbTracks(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return List.of();
		}
		List<TrackRow> rows = new ArrayList<>();
		addDcTracks(snapshot, TB_TRACK_BANK, false, rows);
		return rows;
	}

	private static void addDcTracks(EventSnapshot snapshot, String bankName, boolean hitBased,
			List<TrackRow> rows) {
		DataBank bank = snapshot.bank(bankName).orElse(null);
		if (!hasColumns(bank, "Vtx0_x", "Vtx0_y", "Vtx0_z", "p0_x", "p0_y", "p0_z", "q", "status", "id")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			int charge = bank.getByte("q", row);
			LundId particle = hitBased ? LundSupport.getHitbased(charge) : LundSupport.getTrackbased(charge);
			TrackRow.fromMomentum(bank.getShort("id", row), particle, bank.getFloat("Vtx0_x", row),
					bank.getFloat("Vtx0_y", row), bank.getFloat("Vtx0_z", row), bank.getFloat("p0_x", row),
					bank.getFloat("p0_y", row), bank.getFloat("p0_z", row), bank.getShort("status", row),
					bankName).ifPresent(rows::add);
		}
	}

	private static void addRecParticles(EventSnapshot snapshot, List<TrackRow> rows) {
		for (RecEventData.Particle particle : RecEventData.from(snapshot).particles()) {
			LundId lundId = CedDrawingStyle.lundId(particle.pid(), particle.charge());
			TrackRow.fromMomentum(0, lundId, particle.vx(), particle.vy(), particle.vz(), particle.px(),
					particle.py(), particle.pz(), particle.status(), RecEventData.RECON_BANK)
					.ifPresent(rows::add);
		}
	}

	private static void addCvtTracks(EventSnapshot snapshot, String bankName, List<TrackRow> rows) {
		DataBank bank = snapshot.bank(bankName).orElse(null);
		if (!hasColumns(bank, "q", "pt", "phi0", "d0", "z0", "tandip", "ID")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			int charge = bank.getByte("q", row);
			double phi0 = bank.getFloat("phi0", row);
			double pt = bank.getFloat("pt", row);
			double d0 = bank.getFloat("d0", row);
			// CVT tracks carry a helix's circle parameters, not a vertex/momentum
			// vector directly -- the transverse vertex and momentum are derived
			// from (d0, phi0, pt, tanDip), matching legacy CED exactly.
			double x0 = -d0 * Math.sin(phi0);
			double y0 = d0 * Math.cos(phi0);
			double px = pt * Math.cos(phi0);
			double py = pt * Math.sin(phi0);
			double pz = pt * bank.getFloat("tandip", row);
			LundId particle = LundSupport.getCVTbased(charge);
			TrackRow.fromMomentum(bank.getShort("ID", row), particle, x0, y0, bank.getFloat("z0", row), px,
					py, pz, 0, bankName).ifPresent(rows::add);
		}
	}

	private static boolean hasColumns(DataBank bank, String... names) {
		if (bank == null) {
			return false;
		}
		for (String name : names) {
			if (!BankAccess.hasColumn(bank, name)) {
				return false;
			}
		}
		return true;
	}
}
