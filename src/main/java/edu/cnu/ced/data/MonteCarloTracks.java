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
			// the one-arg get(pid) -- unlike CedDrawingStyle.lundId(pid, charge)
			// -- returns null rather than falling back to an unknown-by-charge
			// placeholder for an unregistered id; generator truth should always
			// carry a real, registered PDG id, so a miss here means skip the
			// row rather than invent a charge to fall back on, matching legacy.
			LundId particle = LundSupport.getInstance().get(bank.getInt("pid", row));
			if (particle == null) {
				continue;
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
