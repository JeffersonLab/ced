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
