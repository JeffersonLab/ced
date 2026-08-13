package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataSource;
import org.junit.jupiter.api.Test;

class CoatDataSourceAdapterTest {

	@Test
	void ownsNavigationCursorWhenCoatSourceCannotNavigateBackward() {
		List<DataEvent> events = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			events.add((DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
					new Class<?>[] { DataEvent.class }, (object, method, args) -> null));
		}
		int[] sequentialIndex = { -1 };
		DataSource coatSource = (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
				new Class<?>[] { DataSource.class }, (object, method, args) -> switch (method.getName()) {
					case "getSize" -> events.size();
					case "hasEvent" -> sequentialIndex[0] + 1 < events.size();
					case "getNextEvent" -> events.get(++sequentialIndex[0]);
					case "getPreviousEvent" -> null;
					case "gotoEvent" -> events.get((Integer) args[0]);
					case "getCurrentIndex" -> sequentialIndex[0];
					default -> null;
				});

		CoatDataSourceAdapter adapter = new CoatDataSourceAdapter(coatSource, "test.hipo");
		assertSame(events.get(0), adapter.next());
		assertSame(events.get(1), adapter.next());
		assertEquals(1, adapter.index());
		assertSame(events.get(0), adapter.previous());
		assertEquals(0, adapter.index());
		assertSame(events.get(2), adapter.goTo(2));
		assertEquals(2, adapter.index());
	}
}
