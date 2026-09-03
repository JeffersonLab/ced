package edu.cnu.ced.view.currentevent;

import java.util.prefs.Preferences;

/**
 * Whether double-clicking a present bank opens its data viewer as a free
 * floating window, or as a can't-get-lost MDI-internal view -- the Options
 * menu's "Bank Views are Free Floating" preference in legacy CED. Persisted
 * via {@link Preferences} (as {@code edu.cnu.mdi.io.RecentFiles} already is
 * elsewhere in this codebase) so different users can each keep what they
 * prefer across sessions.
 */
public final class BankViewerDisplayMode {

	private static final String KEY = "floating";

	private final Preferences preferences;

	public BankViewerDisplayMode() {
		this(Preferences.userNodeForPackage(BankViewerDisplayMode.class).node("bank-viewer-display"));
	}

	/** Visible for testing against an isolated, disposable {@link Preferences} node. */
	BankViewerDisplayMode(Preferences preferences) {
		this.preferences = preferences;
	}

	/** @return {@code true} if bank viewers should open as free floating windows (the default) */
	public boolean isFloating() {
		return preferences.getBoolean(KEY, true);
	}

	public void setFloating(boolean floating) {
		preferences.putBoolean(KEY, floating);
	}
}
