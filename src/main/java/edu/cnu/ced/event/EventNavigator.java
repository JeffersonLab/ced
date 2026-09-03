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

	public boolean next() {
		if (source == null || !source.hasNext()) {
			return false;
		}
		publish(source.next());
		return true;
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

	public boolean previous() {
		if (source == null || source.index() <= 0) {
			return false;
		}
		publish(source.previous());
		return true;
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
