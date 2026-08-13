package edu.cnu.ced.event;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Coordinates one event source and publishes complete navigation states. */
public final class EventNavigator implements AutoCloseable {

	private final EventStore store;
	private final CopyOnWriteArrayList<Consumer<EventNavigationState>> listeners =
			new CopyOnWriteArrayList<>();

	private EventSource source;
	private volatile EventNavigationState state = EventNavigationState.closed();

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

	/** Open a source and publish its first event, when present. */
	public void open(EventSource newSource) {
		Objects.requireNonNull(newSource, "newSource");
		closeSource();
		source = newSource;
		if (source.size() > 0 && source.hasNext()) {
			publish(source.next());
		} else {
			store.clear();
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
