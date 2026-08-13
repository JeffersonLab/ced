package edu.cnu.ced.event;

/** Immutable UI-facing state of event navigation. */
public record EventNavigationState(String source, int sequenceNumber,
		int eventCount, boolean canGoPrevious, boolean canGoNext,
		EventSnapshot snapshot) {

	private static final EventNavigationState CLOSED = new EventNavigationState(
			"", 0, 0, false, false, EventSnapshot.empty());

	public EventNavigationState {
		source = source == null ? "" : source;
		snapshot = snapshot == null ? EventSnapshot.empty() : snapshot;
	}

	public static EventNavigationState closed() {
		return CLOSED;
	}

	public boolean isOpen() {
		return !source.isEmpty();
	}
}
