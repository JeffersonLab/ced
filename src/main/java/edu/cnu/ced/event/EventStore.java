package edu.cnu.ced.event;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.jlab.io.base.DataEvent;

/** Atomically publishes the current event snapshot to all CED consumers. */
public final class EventStore {

	private final AtomicReference<EventSnapshot> current =
			new AtomicReference<>(EventSnapshot.empty());

	/** @return the complete current snapshot */
	public EventSnapshot current() {
		return current.get();
	}

	/** Build and atomically publish a snapshot for the supplied event. */
	public EventSnapshot publish(DataEvent event) {
		EventSnapshot snapshot = EventSnapshot.of(Objects.requireNonNull(event, "event"));
		current.set(snapshot);
		return snapshot;
	}

	/** Atomically remove the current event. */
	public void clear() {
		current.set(EventSnapshot.empty());
	}
}
