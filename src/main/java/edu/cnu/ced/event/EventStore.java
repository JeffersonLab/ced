package edu.cnu.ced.event;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jlab.io.base.DataEvent;

/** Atomically publishes the current event snapshot to all CED consumers. */
public final class EventStore {

	private final AtomicReference<EventSnapshot> current =
			new AtomicReference<>(EventSnapshot.empty());
	private final CopyOnWriteArrayList<Consumer<EventSnapshot>> listeners =
			new CopyOnWriteArrayList<>();

	/** @return the complete current snapshot */
	public EventSnapshot current() {
		return current.get();
	}

	/** Build and atomically publish a snapshot for the supplied event. */
	public EventSnapshot publish(DataEvent event) {
		EventSnapshot snapshot = EventSnapshot.of(Objects.requireNonNull(event, "event"));
		current.set(snapshot);
		notifyListeners(snapshot);
		return snapshot;
	}

	/** Atomically remove the current event. */
	public void clear() {
		EventSnapshot empty = EventSnapshot.empty();
		current.set(empty);
		notifyListeners(empty);
	}

	/**
	 * Subscribe to complete snapshots. The listener immediately receives the
	 * current snapshot and then every subsequent atomic publication.
	 */
	public void addListener(Consumer<EventSnapshot> listener) {
		Consumer<EventSnapshot> checked = Objects.requireNonNull(listener, "listener");
		listeners.addIfAbsent(checked);
		checked.accept(current());
	}

	/** Stop receiving event snapshots. */
	public void removeListener(Consumer<EventSnapshot> listener) {
		listeners.remove(listener);
	}

	private void notifyListeners(EventSnapshot snapshot) {
		listeners.forEach(listener -> listener.accept(snapshot));
	}
}
