package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

class EventNavigatorTest {

	@Test
	void goesToTheEventWithAMatchingTrueEventNumber() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		// true event numbers 100..104 at sequence numbers 1..5, one-to-one
		navigator.open(new RunConfigSource(5, 100));

		assertTrue(navigator.goToTrueEventNumber(103));
		assertEquals(4, navigator.state().sequenceNumber());

		// already-built index is reused for a second, different lookup
		assertTrue(navigator.goToTrueEventNumber(100));
		assertEquals(1, navigator.state().sequenceNumber());
	}

	@Test
	void unmatchedTrueEventNumberLeavesPositionUnchanged() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		navigator.open(new RunConfigSource(3, 100));
		navigator.next();
		assertEquals(2, navigator.state().sequenceNumber());

		assertFalse(navigator.goToTrueEventNumber(999));
		assertEquals(2, navigator.state().sequenceNumber());
	}

	@Test
	void refreshRenotifiesListenersWithoutChangingPosition() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		navigator.open(new TaggedSource(3, 0, 1, 2));
		List<Integer> observed = new ArrayList<>();
		navigator.addListener(state -> observed.add(state.sequenceNumber()));

		navigator.refresh();
		navigator.refresh();

		assertEquals(1, navigator.state().sequenceNumber(), "refresh doesn't navigate");
		assertEquals(List.of(1, 1), observed, "one notification per refresh call, same state each time");
	}

	private static final class RunConfigSource implements EventSource {
		private final List<DataEvent> events;
		private int index = -1;

		RunConfigSource(int count, int firstTrueEventNumber) {
			events = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				events.add(runConfigEvent(firstTrueEventNumber + i));
			}
		}

		private static DataEvent runConfigEvent(int trueEventNumber) {
			DataBank runConfig = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
					new Class<?>[] { DataBank.class }, (bankProxy, bankMethod, bankArgs) ->
							switch (bankMethod.getName()) {
								case "getColumnList" -> new String[] { "run", "event", "solenoid", "torus" };
								case "rows" -> 1;
								case "getInt" -> "event".equals(bankArgs[0]) ? trueEventNumber : 11;
								case "getFloat" -> 1.0f;
								default -> null;
							});
			return (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
					new Class<?>[] { DataEvent.class }, (proxy, method, args) -> switch (method.getName()) {
						case "getBankList" -> new String[] { "RUN::config" };
						case "hasBank" -> "RUN::config".equals(args[0]);
						case "getBank" -> "RUN::config".equals(args[0]) ? runConfig : null;
						default -> null;
					});
		}

		@Override public String description() { return "run-config.hipo"; }
		@Override public int size() { return events.size(); }
		@Override public int index() { return index; }
		@Override public boolean hasNext() { return index + 1 < events.size(); }
		@Override public DataEvent next() { return events.get(++index); }
		@Override public DataEvent previous() { return events.get(--index); }
		@Override public DataEvent goTo(int target) { index = target; return events.get(index); }
		@Override public void close() { }
	}

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

	@Test
	void nextSkipsNonMatchingEventsWithoutPublishingThem() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		List<Integer> observed = new ArrayList<>();
		navigator.addListener(state -> observed.add(state.sequenceNumber()));
		// bank "TAG" present only on sequence 1, 3, 5 (0-based index 0, 2, 4)
		navigator.open(new TaggedSource(5, 0, 2, 4));
		navigator.setFilter(snapshot -> snapshot.hasBank("TAG"));

		assertTrue(navigator.next());
		assertEquals(3, navigator.state().sequenceNumber(), "skips seq 2, lands on the next tagged event");
		assertTrue(navigator.next());
		assertEquals(5, navigator.state().sequenceNumber(), "skips seq 4, lands on the last tagged event");
		assertFalse(navigator.next(), "no more tagged events after the last one");
		assertEquals(5, navigator.state().sequenceNumber(), "position unchanged after a failed next");

		assertEquals(List.of(1, 3, 5), observed, "only tagged events were ever published to listeners");
	}

	@Test
	void previousSkipsNonMatchingEventsSymmetricallyWithNext() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		navigator.open(new TaggedSource(5, 0, 2, 4));
		navigator.setFilter(snapshot -> snapshot.hasBank("TAG"));
		navigator.goToSequence(5);

		assertTrue(navigator.previous());
		assertEquals(3, navigator.state().sequenceNumber(), "skips seq 4, lands on the previous tagged event");
		assertTrue(navigator.previous());
		assertEquals(1, navigator.state().sequenceNumber(), "skips seq 2, lands on the first tagged event");
		assertFalse(navigator.previous(), "no earlier tagged events before the first one");
		assertEquals(1, navigator.state().sequenceNumber(), "position unchanged after a failed previous");
	}

	@Test
	void nullFilterClearsBackToUnfiltered() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		navigator.open(new TaggedSource(3, 0));
		navigator.setFilter(snapshot -> snapshot.hasBank("TAG"));
		navigator.setFilter(null);

		assertTrue(navigator.next());
		assertEquals(2, navigator.state().sequenceNumber(), "unfiltered: advances one at a time again");
	}

	private static final class TaggedSource implements EventSource {
		private final List<DataEvent> events;
		private int index = -1;

		TaggedSource(int count, int... taggedIndices) {
			Set<Integer> tagged = new HashSet<>();
			for (int i : taggedIndices) {
				tagged.add(i);
			}
			events = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				String[] banks = tagged.contains(i) ? new String[] { "TAG" } : new String[0];
				events.add((DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
						new Class<?>[] { DataEvent.class }, (object, method, args) -> switch (method.getName()) {
							case "getBankList" -> banks;
							case "hasBank" -> Arrays.asList(banks).contains(args[0]);
							default -> null;
						}));
			}
		}

		@Override public String description() { return "tagged.hipo"; }
		@Override public int size() { return events.size(); }
		@Override public int index() { return index; }
		@Override public boolean hasNext() { return index + 1 < events.size(); }
		@Override public DataEvent next() { return events.get(++index); }
		@Override public DataEvent previous() { return events.get(--index); }
		@Override public DataEvent goTo(int target) { index = target; return events.get(index); }
		@Override public void close() { }
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
