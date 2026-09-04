package edu.cnu.ced.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.BooleanSupplier;

import org.jlab.io.base.DataEvent;

/** Coordinates one event source and publishes complete navigation states. */
public final class EventNavigator implements AutoCloseable {

	private final EventStore store;
	private final CopyOnWriteArrayList<Consumer<EventNavigationState>> listeners =
			new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<Runnable> sourceListeners =
			new CopyOnWriteArrayList<>();

	private EventSource source;
	private volatile EventNavigationState state = EventNavigationState.closed();
	private volatile EventFilter filter = EventFilter.ALWAYS_PASS;

	// RUN::config true-event-number -> one-based sequence number, built lazily
	// (and only once per source) the first time goToTrueEventNumber is called --
	// matching legacy CED's own ScanManager, which also walks the whole file on
	// first use rather than indexing it up front.
	private Map<Integer, Integer> trueEventIndex;
	private EventSource trueEventIndexSource;

	public EventNavigator(EventStore store) {
		this.store = Objects.requireNonNull(store, "store");
	}

	public EventNavigationState state() {
		return state;
	}

	/**
	 * Install the filter that {@link #next()} and {@link #previous()} consult
	 * to silently skip non-matching events -- {@code null} clears back to
	 * unfiltered. Does not affect {@link #goToSequence}, {@link
	 * #goToTrueEventNumber}, or {@link #scanNext}: an explicit jump goes
	 * exactly where asked regardless of the filter, and scanNext must visit
	 * every event unfiltered since {@link #goToTrueEventNumber}'s index build
	 * depends on that.
	 */
	public void setFilter(EventFilter filter) {
		this.filter = (filter == null) ? EventFilter.ALWAYS_PASS : filter;
	}

	/**
	 * Re-notify every listener with the current, unchanged state -- for a
	 * change that doesn't move to a different event but that listeners still
	 * derive something from, e.g. whether any {@link EventFilters} criterion
	 * is now active, so every open view's "Filtering Active" indicator
	 * updates immediately rather than waiting for the next navigation.
	 */
	public void refresh() {
		notifyListeners();
	}

	public void addListener(Consumer<EventNavigationState> listener) {
		listeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
	}

	public void removeListener(Consumer<EventNavigationState> listener) {
		listeners.remove(listener);
	}

	/** Register a callback invoked before the first event from each new source. */
	public void addSourceListener(Runnable listener) {
		sourceListeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
	}

	public void removeSourceListener(Runnable listener) {
		sourceListeners.remove(listener);
	}

	/** Open a source and publish its first event, when present. */
	public void open(EventSource newSource) {
		Objects.requireNonNull(newSource, "newSource");
		closeSource();
		source = newSource;
		store.clear();
		sourceListeners.forEach(Runnable::run);
		if (source.size() > 0 && source.hasNext()) {
			publish(source.next());
		} else {
			updateState(EventSnapshot.empty());
		}
	}

	/**
	 * Advance to the next event that passes the installed filter (every event,
	 * if none is installed), silently skipping ones that don't -- symmetric
	 * with {@link #previous()}. Skipped events are never published: nothing
	 * observing this navigator's state, nor the underlying {@link EventStore},
	 * sees them.
	 *
	 * @return {@code true} if a matching event was found and published
	 */
	public boolean next() {
		if (source == null) {
			return false;
		}
		while (source.hasNext()) {
			DataEvent candidate = source.next();
			if (candidate == null) {
				throw new IllegalStateException("Event source returned no event");
			}
			if (filter.pass(EventSnapshot.of(candidate))) {
				updateState(store.publish(candidate));
				return true;
			}
		}
		return false;
	}

	/**
	 * Consume following events without publishing intermediate navigation states.
	 * Only the final consumed event becomes the current event.
	 *
	 * @param count maximum number of following events to consume
	 * @param accumulator receives a complete snapshot for each consumed event
	 * @return number of events consumed
	 */
	public int scanNext(int count, Consumer<EventSnapshot> accumulator) {
		return scanNext(count, accumulator, processed -> { }, () -> false);
	}

	/** Consume following events silently, with progress and cancellation hooks. */
	public int scanNext(int count, Consumer<EventSnapshot> accumulator,
			IntConsumer progress, BooleanSupplier cancelled) {
		Objects.requireNonNull(accumulator, "accumulator");
		Objects.requireNonNull(progress, "progress");
		Objects.requireNonNull(cancelled, "cancelled");
		if (source == null || count < 1) return 0;
		int processed = 0;
		DataEvent last = null;
		while (processed < count && source.hasNext() && !cancelled.getAsBoolean()) {
			last = source.next();
			if (last == null) break;
			accumulator.accept(EventSnapshot.of(last));
			processed++;
			progress.accept(processed);
		}
		if (last != null) publish(last);
		return processed;
	}

	/**
	 * Step back to the previous event that passes the installed filter,
	 * silently skipping ones that don't -- symmetric with {@link #next()}.
	 *
	 * @return {@code true} if a matching event was found and published
	 */
	public boolean previous() {
		if (source == null) {
			return false;
		}
		while (source.index() > 0) {
			DataEvent candidate = source.previous();
			if (candidate == null) {
				throw new IllegalStateException("Event source returned no event");
			}
			if (filter.pass(EventSnapshot.of(candidate))) {
				updateState(store.publish(candidate));
				return true;
			}
		}
		return false;
	}

	/** Navigate using CED's one-based sequence numbering. */
	public boolean goToSequence(int sequenceNumber) {
		if (source == null || sequenceNumber < 1 || sequenceNumber > source.size()) {
			return false;
		}
		publish(source.goTo(sequenceNumber - 1));
		return true;
	}

	/**
	 * Navigate to the event whose {@code RUN::config} true event number is
	 * {@code trueEventNumber}. Builds (and caches, per source) a full
	 * true-number-to-sequence index on first use, which requires walking every
	 * event in the source once -- this can be slow for a large file, exactly as
	 * it is in legacy CED.
	 *
	 * @return {@code true} if an event with that true number was found
	 */
	public boolean goToTrueEventNumber(int trueEventNumber) {
		if (source == null) {
			return false;
		}
		ensureTrueEventIndex();
		Integer sequence = trueEventIndex.get(trueEventNumber);
		return sequence != null && goToSequence(sequence);
	}

	private void ensureTrueEventIndex() {
		if (trueEventIndex != null && trueEventIndexSource == source) {
			return;
		}
		Map<Integer, Integer> index = new HashMap<>();
		int savedSequence = state.sequenceNumber();
		if (goToSequence(1)) {
			recordTrueEventNumber(index, state.snapshot(), 1);
			int[] nextSequence = { 2 };
			scanNext(Math.max(0, source.size() - 1), snapshot -> {
				recordTrueEventNumber(index, snapshot, nextSequence[0]);
				nextSequence[0]++;
			});
			if (savedSequence >= 1) {
				goToSequence(savedSequence);
			}
		}
		trueEventIndex = index;
		trueEventIndexSource = source;
	}

	private static void recordTrueEventNumber(Map<Integer, Integer> index, EventSnapshot snapshot,
			int sequenceNumber) {
		RunConfig.from(snapshot).ifPresent(config -> index.put(config.event(), sequenceNumber));
	}

	@Override
	public void close() {
		closeSource();
		state = EventNavigationState.closed();
		store.clear();
		notifyListeners();
	}

	private void publish(org.jlab.io.base.DataEvent event) {
		if (event == null) {
			throw new IllegalStateException("Event source returned no event");
		}
		updateState(store.publish(event));
	}

	private void updateState(EventSnapshot snapshot) {
		int index = source.index();
		int size = Math.max(0, source.size());
		state = new EventNavigationState(source.description(), index + 1, size,
				index > 0, index + 1 < size, snapshot);
		notifyListeners();
	}

	private void notifyListeners() {
		EventNavigationState published = state;
		listeners.forEach(listener -> listener.accept(published));
	}

	private void closeSource() {
		if (source != null) {
			source.close();
			source = null;
		}
	}
}
