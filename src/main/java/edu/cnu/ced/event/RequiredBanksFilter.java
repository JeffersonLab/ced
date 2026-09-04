package edu.cnu.ced.event;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Accepts an event only if every bank in a persisted, user-chosen set is
 * present -- a criterion legacy CED never had (its own present-bank panel is
 * a display widget, not a filter; see the investigation behind this
 * feature).
 */
public final class RequiredBanksFilter implements EventFilter {

	private static final String KEY_ACTIVE = "active";
	private static final String KEY_BANKS = "banks";

	private final Preferences preferences;
	private boolean active;
	private Set<String> requiredBanks;

	public RequiredBanksFilter() {
		this(Preferences.userNodeForPackage(RequiredBanksFilter.class).node("required-banks-filter"));
	}

	/** Visible for testing against an isolated, disposable {@link Preferences} node. */
	RequiredBanksFilter(Preferences preferences) {
		this.preferences = preferences;
		active = preferences.getBoolean(KEY_ACTIVE, false);
		requiredBanks = parseBanks(preferences.get(KEY_BANKS, ""));
	}

	private static Set<String> parseBanks(String stored) {
		Set<String> banks = new LinkedHashSet<>();
		for (String bank : stored.split(",")) {
			String trimmed = bank.trim();
			if (!trimmed.isEmpty()) {
				banks.add(trimmed);
			}
		}
		return banks;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		preferences.putBoolean(KEY_ACTIVE, active);
	}

	/** @return the required bank names, in the order they were set */
	public Set<String> requiredBanks() {
		return Set.copyOf(requiredBanks);
	}

	public void setRequiredBanks(Set<String> banks) {
		requiredBanks = new LinkedHashSet<>(banks);
		preferences.put(KEY_BANKS, String.join(",", requiredBanks));
	}

	@Override
	public boolean pass(EventSnapshot snapshot) {
		if (!active || requiredBanks.isEmpty()) {
			return true;
		}
		for (String bank : requiredBanks) {
			if (!snapshot.hasBank(bank)) {
				return false;
			}
		}
		return true;
	}
}
