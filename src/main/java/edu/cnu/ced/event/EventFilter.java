package edu.cnu.ced.event;

/**
 * A single event-acceptance criterion, consulted by {@link EventNavigator}'s
 * {@code next()}/{@code previous()} to silently skip non-matching events --
 * the generalization of legacy CED's {@code IEventFilter}/{@code
 * FilterManager} (which only ever had one concrete implementation, a
 * trigger-bit pattern) to any number of independent criteria.
 */
@FunctionalInterface
public interface EventFilter {

	/** A filter that accepts every event; the default when none is installed. */
	EventFilter ALWAYS_PASS = snapshot -> true;

	/** @return {@code true} if {@code snapshot} should be shown to the user */
	boolean pass(EventSnapshot snapshot);
}
