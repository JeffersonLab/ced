package edu.cnu.ced.event;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.prefs.Preferences;

import org.jlab.io.base.DataBank;

/**
 * Accepts an event only if at least one {@code REC::Particle} row's PID is
 * one of a persisted, user-chosen set of species -- a criterion legacy CED
 * never had at all (see the investigation behind this feature). Reads
 * {@code REC::Particle} directly rather than depending on {@code
 * edu.cnu.ced.data.RecEventData}, which is itself built on this {@code
 * event} package -- a filter pulling in that higher layer would invert the
 * package's existing dependency direction for the sake of a single column.
 */
public final class ParticleIdFilter implements EventFilter {

	private static final String BANK_NAME = "REC::Particle";
	private static final String COLUMN = "pid";
	private static final String KEY_ACTIVE = "active";
	private static final String KEY_PIDS = "pids";

	private final Preferences preferences;
	private boolean active;
	private Set<Integer> pids;

	public ParticleIdFilter() {
		this(Preferences.userNodeForPackage(ParticleIdFilter.class).node("particle-id-filter"));
	}

	/** Visible for testing against an isolated, disposable {@link Preferences} node. */
	ParticleIdFilter(Preferences preferences) {
		this.preferences = preferences;
		active = preferences.getBoolean(KEY_ACTIVE, false);
		pids = parsePids(preferences.get(KEY_PIDS, ""));
	}

	private static Set<Integer> parsePids(String stored) {
		Set<Integer> parsed = new LinkedHashSet<>();
		for (String token : stored.split(",")) {
			String trimmed = token.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			try {
				parsed.add(Integer.parseInt(trimmed));
			} catch (NumberFormatException corrupted) {
				// skip a corrupted entry rather than fail the whole load
			}
		}
		return parsed;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		preferences.putBoolean(KEY_ACTIVE, active);
	}

	/** @return the accepted PDG/Lund particle ids */
	public Set<Integer> pids() {
		return Set.copyOf(pids);
	}

	public void setPids(Set<Integer> pids) {
		this.pids = new LinkedHashSet<>(pids);
		StringBuilder joined = new StringBuilder();
		for (int pid : this.pids) {
			if (joined.length() > 0) {
				joined.append(',');
			}
			joined.append(pid);
		}
		preferences.put(KEY_PIDS, joined.toString());
	}

	@Override
	public boolean pass(EventSnapshot snapshot) {
		if (!active || pids.isEmpty()) {
			return true;
		}
		DataBank bank = snapshot.bank(BANK_NAME).orElse(null);
		if (bank == null || !BankAccess.hasColumn(bank, COLUMN)) {
			return false;
		}
		for (int row = 0; row < bank.rows(); row++) {
			if (pids.contains(bank.getInt(COLUMN, row))) {
				return true;
			}
		}
		return false;
	}
}
