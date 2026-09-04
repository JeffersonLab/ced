package edu.cnu.ced.event;

import java.util.prefs.Preferences;

/**
 * Accepts an event based on how its {@code RUN::trigger} word (see
 * {@link RunTrigger}) compares against a persisted bit pattern -- the
 * generalized, actually-persisted version of legacy CED's {@code
 * TriggerFilter}/{@code TriggerMatch} (whose own {@code savePreferences()}/
 * {@code readPreferences()} were empty stubs, so its pattern and mode reset
 * on every restart; this one doesn't).
 */
public final class TriggerBitFilter implements EventFilter {

	/** How {@link #pattern()} is compared against the event's trigger word. */
	public enum MatchMode {
		/** The trigger word must equal the pattern bit-for-bit. */
		EXACT,
		/** At least one bit set in the pattern is also set in the trigger word. */
		ANY,
		/** Every bit set in the pattern is also set in the trigger word. */
		ALL
	}

	private static final String KEY_ACTIVE = "active";
	private static final String KEY_PATTERN = "pattern";
	private static final String KEY_MODE = "mode";
	private static final long DEFAULT_PATTERN = 0xFFFFFFFFL;
	private static final MatchMode DEFAULT_MODE = MatchMode.ANY;

	private final Preferences preferences;
	private boolean active;
	private long pattern;
	private MatchMode mode;

	public TriggerBitFilter() {
		this(Preferences.userNodeForPackage(TriggerBitFilter.class).node("trigger-bit-filter"));
	}

	/** Visible for testing against an isolated, disposable {@link Preferences} node. */
	TriggerBitFilter(Preferences preferences) {
		this.preferences = preferences;
		active = preferences.getBoolean(KEY_ACTIVE, false);
		pattern = preferences.getLong(KEY_PATTERN, DEFAULT_PATTERN);
		mode = readMode(preferences);
	}

	private static MatchMode readMode(Preferences preferences) {
		try {
			return MatchMode.valueOf(preferences.get(KEY_MODE, DEFAULT_MODE.name()));
		} catch (IllegalArgumentException corrupted) {
			return DEFAULT_MODE;
		}
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		preferences.putBoolean(KEY_ACTIVE, active);
	}

	public long pattern() {
		return pattern;
	}

	public void setPattern(long pattern) {
		this.pattern = pattern;
		preferences.putLong(KEY_PATTERN, pattern);
	}

	public MatchMode mode() {
		return mode;
	}

	public void setMode(MatchMode mode) {
		this.mode = (mode == null) ? DEFAULT_MODE : mode;
		preferences.put(KEY_MODE, this.mode.name());
	}

	@Override
	public boolean pass(EventSnapshot snapshot) {
		if (!active) {
			return true;
		}
		// row absent while active -> reject, matching legacy's own fail-closed behavior
		return RunTrigger.from(snapshot).map(trigger -> matches(trigger.trigger())).orElse(false);
	}

	private boolean matches(long triggerWord) {
		return switch (mode) {
			case EXACT -> pattern == triggerWord;
			case ANY -> (pattern & triggerWord) != 0;
			case ALL -> (pattern & triggerWord) == pattern;
		};
	}
}
