package edu.cnu.ced.view.currentevent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Persists which columns are shown in a bank's per-bank data viewer, keyed
 * by bank name -- the "Visibility" checkbox row in legacy CED's bank
 * windows, backed there by its own PropertiesManager and here by
 * {@link Preferences}, the same persistence mechanism this codebase already
 * uses for {@code edu.cnu.mdi.io.RecentFiles}.
 */
public final class BankColumnVisibility {

	private final Preferences preferences;

	public BankColumnVisibility() {
		this(Preferences.userNodeForPackage(BankColumnVisibility.class).node("bank-column-visibility"));
	}

	/** Visible for testing against an isolated, disposable {@link Preferences} node. */
	BankColumnVisibility(Preferences preferences) {
		this.preferences = preferences;
	}

	/** @return whether {@code column} should be shown for {@code bankName}; visible by default */
	public boolean isVisible(String bankName, String column) {
		return !hiddenColumns(bankName).contains(column);
	}

	/** Persist whether {@code column} is shown for {@code bankName}. */
	public void setVisible(String bankName, String column, boolean visible) {
		Set<String> hidden = hiddenColumns(bankName);
		boolean changed = visible ? hidden.remove(column) : hidden.add(column);
		if (changed) {
			preferences.put(bankName, String.join(",", hidden));
		}
	}

	private Set<String> hiddenColumns(String bankName) {
		Set<String> hidden = new LinkedHashSet<>();
		String stored = preferences.get(bankName, "");
		for (String column : stored.split(",")) {
			if (!column.isBlank()) {
				hidden.add(column);
			}
		}
		return hidden;
	}
}
