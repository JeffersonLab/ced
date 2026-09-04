package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import cnuphys.lund.LundId;
import cnuphys.lund.LundSupport;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/**
 * Generator-truth Monte Carlo particles for one event, from {@code
 * MC::Particle} and {@code MC::Lund} -- matching legacy CED's {@code
 * ClasIoMonteCarloView}, which reads both banks into the same table.
 */
public record MonteCarloTracks(List<TrackRow> tracks) {

	public static final String PARTICLE_BANK = "MC::Particle";
	public static final String LUND_BANK = "MC::Lund";

	private static final MonteCarloTracks EMPTY = new MonteCarloTracks(List.of());

	public MonteCarloTracks {
		tracks = List.copyOf(tracks);
	}

	public static MonteCarloTracks from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return EMPTY;
		}
		List<TrackRow> rows = new ArrayList<>();
		addBank(snapshot, PARTICLE_BANK, rows);
		addBank(snapshot, LUND_BANK, rows);
		return rows.isEmpty() ? EMPTY : new MonteCarloTracks(rows);
	}

	private static void addBank(EventSnapshot snapshot, String bankName, List<TrackRow> rows) {
		DataBank bank = snapshot.bank(bankName).orElse(null);
		if (!hasColumns(bank, "pid", "vx", "vy", "vz", "px", "py", "pz")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			int pid = bank.getInt("pid", row);
			// LundSupport's one-arg get(pid) returns null, with no fallback,
			// for a pid it doesn't recognize -- legacy CED skips that row
			// entirely ("cannot swim without a known charge"), reasonable for
			// a view built around swimming a track through the field, but this
			// is a plain data table: every field it needs (vertex, momentum)
			// is already known regardless of whether cnuphys.lund's curated
			// species list happens to include this generator pid. Falling back
			// to an unresolved placeholder that still carries the real pid
			// (rather than skipping) keeps the row visible -- and keeping the
			// actual pid, rather than 0 or a fixed sentinel, is what makes an
			// unrecognized generator code diagnosable instead of just missing.
			LundId particle = LundSupport.getInstance().get(pid);
			if (particle == null) {
				particle = new LundId("Unknown", "?", pid, 0, 0, 0);
			}
			TrackRow.fromMomentum(row, particle, bank.getFloat("vx", row), bank.getFloat("vy", row),
					bank.getFloat("vz", row), bank.getFloat("px", row), bank.getFloat("py", row),
					bank.getFloat("pz", row), 0, bankName).ifPresent(rows::add);
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
