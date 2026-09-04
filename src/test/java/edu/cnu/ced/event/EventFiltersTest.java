package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

class EventFiltersTest {

	@Test void passesWhenNoCriterionIsActive() throws Exception {
		withFilters(filters -> assertTrue(filters.pass(EventSnapshot.empty())));
	}

	@Test void combinesActiveCriteriaWithLogicalAnd() throws Exception {
		withFilters(filters -> {
			filters.requiredBanksFilter().setActive(true);
			filters.requiredBanksFilter().setRequiredBanks(Set.of("CND::adc"));
			filters.particleIdFilter().setActive(true);
			filters.particleIdFilter().setPids(Set.of(2212));

			// satisfies the bank requirement but not the PID requirement
			assertFalse(filters.pass(snapshot(Set.of("CND::adc"), new int[0])),
					"AND: one satisfied criterion isn't enough");
			// satisfies the PID requirement but not the bank requirement
			assertFalse(filters.pass(snapshot(Set.of(), new int[] { 2212 })),
					"AND: the other satisfied criterion isn't enough either");
			// satisfies both
			assertTrue(filters.pass(snapshot(Set.of("CND::adc"), new int[] { 2212 })));
		});
	}

	@Test void isAnyActiveReflectsEachCriterionIndependently() throws Exception {
		withFilters(filters -> {
			assertFalse(filters.isAnyActive());
			filters.requiredBanksFilter().setActive(true);
			assertTrue(filters.isAnyActive());
			filters.requiredBanksFilter().setActive(false);
			filters.particleIdFilter().setActive(true);
			assertTrue(filters.isAnyActive());
		});
	}

	@Test void notifyChangedFiresEveryRegisteredListenerAndRemoveStopsIt() throws Exception {
		withFilters(filters -> {
			List<String> fired = new ArrayList<>();
			Runnable listener = () -> fired.add("changed");
			filters.addListener(listener);

			filters.notifyChanged();
			assertEquals(List.of("changed"), fired);

			filters.removeListener(listener);
			filters.notifyChanged();
			assertEquals(List.of("changed"), fired, "removed listener doesn't fire again");
		});
	}

	private static void withFilters(java.util.function.Consumer<EventFilters> body) throws Exception {
		String suffix = System.nanoTime() + "";
		Preferences trigger = Preferences.userRoot().node("edu/cnu/ced/tests/ef-trigger-" + suffix);
		Preferences banks = Preferences.userRoot().node("edu/cnu/ced/tests/ef-banks-" + suffix);
		Preferences pids = Preferences.userRoot().node("edu/cnu/ced/tests/ef-pids-" + suffix);
		try {
			body.accept(new EventFilters(new TriggerBitFilter(trigger), new RequiredBanksFilter(banks),
					new ParticleIdFilter(pids)));
		} finally {
			trigger.removeNode();
			banks.removeNode();
			pids.removeNode();
		}
	}

	private static EventSnapshot snapshot(Set<String> bankNames, int[] particlePids) {
		Map<String, DataBank> banks = new java.util.HashMap<>();
		for (String name : bankNames) {
			banks.put(name, (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
					new Class<?>[] { DataBank.class }, (proxy, method, args) -> null));
		}
		if (particlePids.length > 0) {
			banks.put("REC::Particle", (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
					new Class<?>[] { DataBank.class }, (proxy, method, args) -> switch (method.getName()) {
						case "getColumnList" -> new String[] { "pid" };
						case "rows" -> particlePids.length;
						case "getInt" -> particlePids[(int) args[1]];
						default -> null;
					}));
		}
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
