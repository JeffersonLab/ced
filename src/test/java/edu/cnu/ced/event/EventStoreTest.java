package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

class EventStoreTest {

	@Test
	void publishesAndClearsWholeSnapshots() {
		EventStore store = new EventStore();
		DataEvent event = (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class }, (instance, method, args) ->
						"getBankList".equals(method.getName()) ? new String[0] : null);

		assertFalse(store.current().hasEvent());
		EventSnapshot published = store.publish(event);
		assertSame(published, store.current());
		assertTrue(store.current().hasEvent());

		store.clear();
		assertSame(EventSnapshot.empty(), store.current());
	}

	@Test
	void listenersReceiveInitialAndCompleteSnapshots() {
		EventStore store = new EventStore();
		List<EventSnapshot> observed = new ArrayList<>();
		store.addListener(observed::add);

		DataEvent event = (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class }, (instance, method, args) ->
						"getBankList".equals(method.getName()) ? new String[] { "RUN::config" } : null);
		store.publish(event);
		store.clear();

		assertEquals(3, observed.size());
		assertFalse(observed.get(0).hasEvent());
		assertTrue(observed.get(1).hasBank("RUN::config"));
		assertFalse(observed.get(2).hasEvent());
	}
}
