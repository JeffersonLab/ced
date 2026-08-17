package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

class EventNavigatorTest {

	@Test
	void publishesOneBasedNavigationStatesAndClosesOldSource() {
		EventStore store = new EventStore();
		EventNavigator navigator = new EventNavigator(store);
		FakeSource source = new FakeSource(3);
		List<Integer> observed = new ArrayList<>();
		navigator.addListener(state -> observed.add(state.sequenceNumber()));

		navigator.open(source);
		assertEquals(1, navigator.state().sequenceNumber());
		assertFalse(navigator.state().canGoPrevious());
		assertTrue(navigator.state().canGoNext());

		assertTrue(navigator.next());
		assertEquals(2, navigator.state().sequenceNumber());
		assertTrue(navigator.previous());
		assertEquals(1, navigator.state().sequenceNumber());
		assertTrue(navigator.goToSequence(3));
		assertEquals(3, navigator.state().sequenceNumber());
		assertFalse(navigator.state().canGoNext());
		assertFalse(navigator.goToSequence(4));

		navigator.close();
		assertTrue(source.closed);
		assertFalse(store.current().hasEvent());
		assertEquals(List.of(1, 2, 1, 3, 0), observed);
	}

	@Test
	void scanPublishesOnlyItsFinalEvent() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		navigator.open(new FakeSource(6));
		List<Integer> observed = new ArrayList<>();
		navigator.addListener(state -> observed.add(state.sequenceNumber()));
		List<EventSnapshot> accumulated = new ArrayList<>();

		assertEquals(4, navigator.scanNext(4, accumulated::add));
		assertEquals(4, accumulated.size());
		assertEquals(5, navigator.state().sequenceNumber());
		assertEquals(List.of(5), observed);
	}

	@Test
	void scanReportsProgressAndHonorsCancellation() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		navigator.open(new FakeSource(6));
		List<Integer> progress = new ArrayList<>();
		int processed = navigator.scanNext(5, snapshot -> { }, progress::add,
				() -> progress.size() >= 2);
		assertEquals(2, processed);
		assertEquals(List.of(1, 2), progress);
		assertEquals(3, navigator.state().sequenceNumber());
	}

	@Test
	void announcesEachNewSourceBeforePublishingItsFirstEvent() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		List<String> notifications = new ArrayList<>();
		navigator.addSourceListener(() -> notifications.add("source"));
		navigator.addListener(state -> notifications.add("event-" + state.sequenceNumber()));

		FakeSource first = new FakeSource(2);
		navigator.open(first);
		navigator.next();
		navigator.open(new FakeSource(1));

		assertTrue(first.closed);
		assertEquals(List.of("source", "event-1", "event-2", "source", "event-1"),
				notifications);
	}

	private static final class FakeSource implements EventSource {
		private final List<DataEvent> events;
		private int index = -1;
		private boolean closed;

		FakeSource(int count) {
			events = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				events.add((DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
						new Class<?>[] { DataEvent.class }, (object, method, args) ->
								"getBankList".equals(method.getName()) ? new String[0] : null));
			}
		}

		@Override public String description() { return "fake.hipo"; }
		@Override public int size() { return events.size(); }
		@Override public int index() { return index; }
		@Override public boolean hasNext() { return index + 1 < events.size(); }
		@Override public DataEvent next() { return events.get(++index); }
		@Override public DataEvent previous() { return events.get(--index); }
		@Override public DataEvent goTo(int target) { index = target; return events.get(index); }
		@Override public void close() { closed = true; }
	}
}
