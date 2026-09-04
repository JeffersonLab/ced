package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.prefs.Preferences;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.TriggerBitFilter.MatchMode;

class TriggerBitFilterTest {

	@Test void inactiveFilterPassesEverything() throws Exception {
		withFilter(filter -> {
			filter.setPattern(0L);
			filter.setMode(MatchMode.EXACT);
			assertTrue(filter.pass(trigger(8385L)));
		});
	}

	@Test void anyModeAcceptsAtLeastOneSharedBit() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			filter.setMode(MatchMode.ANY);
			filter.setPattern(0b1000L); // bit 3
			assertTrue(filter.pass(trigger(0b1010L)), "bit 3 is shared");
			assertFalse(filter.pass(trigger(0b0110L)), "no shared bit");
		});
	}

	@Test void allModeRequiresEveryPatternBit() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			filter.setMode(MatchMode.ALL);
			filter.setPattern(0b1100L);
			assertTrue(filter.pass(trigger(0b1110L)), "trigger word is a superset of the pattern");
			assertFalse(filter.pass(trigger(0b1000L)), "missing pattern bit 2");
		});
	}

	@Test void exactModeRequiresBitForBitEquality() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			filter.setMode(MatchMode.EXACT);
			filter.setPattern(8385L);
			assertTrue(filter.pass(trigger(8385L)));
			assertFalse(filter.pass(trigger(8384L)));
		});
	}

	@Test void activeFilterRejectsAnEventWithNoTriggerBank() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			filter.setMode(MatchMode.ANY);
			filter.setPattern(0xFFFFFFFFL);
			assertFalse(filter.pass(EventSnapshot.empty()));
		});
	}

	@Test void settingsPersistAcrossInstances() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/trigger-bit-filter-" + System.nanoTime());
		try {
			TriggerBitFilter first = new TriggerBitFilter(preferences);
			first.setActive(true);
			first.setMode(MatchMode.ALL);
			first.setPattern(42L);

			TriggerBitFilter reloaded = new TriggerBitFilter(preferences);
			assertTrue(reloaded.isActive());
			assertEquals(MatchMode.ALL, reloaded.mode());
			assertEquals(42L, reloaded.pattern());
		} finally {
			preferences.removeNode();
		}
	}

	@Test void defaultsAreInactiveAnyEverything() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/trigger-bit-filter-" + System.nanoTime());
		try {
			TriggerBitFilter filter = new TriggerBitFilter(preferences);
			assertFalse(filter.isActive());
			assertEquals(MatchMode.ANY, filter.mode());
			assertEquals(0xFFFFFFFFL, filter.pattern());
		} finally {
			preferences.removeNode();
		}
	}

	private static void withFilter(java.util.function.Consumer<TriggerBitFilter> body) throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/trigger-bit-filter-" + System.nanoTime());
		try {
			body.accept(new TriggerBitFilter(preferences));
		} finally {
			preferences.removeNode();
		}
	}

	private static EventSnapshot trigger(long triggerWord) {
		DataBank bank = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getColumnList" -> new String[] { "id", "trigger" };
					case "rows" -> 1;
					case "getInt" -> "id".equals(args[0]) ? 0 : (int) triggerWord;
					default -> null;
				});
		Map<String, DataBank> banks = Map.of("RUN::trigger", bank);
		DataEvent event = (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getBankList" -> banks.keySet().toArray(String[]::new);
					case "hasBank" -> banks.containsKey(args[0]);
					case "getBank" -> banks.get(args[0]);
					default -> null;
				});
		return EventSnapshot.of(event);
	}
}
