package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

class ParticleIdFilterTest {

	@Test void inactiveFilterPassesEvenWithoutAMatchingSpecies() throws Exception {
		withFilter(filter -> {
			filter.setPids(Set.of(2212));
			assertTrue(filter.pass(particles(11, -211)));
		});
	}

	@Test void activeFilterRequiresAtLeastOneMatchingSpecies() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			filter.setPids(Set.of(2212, 11));
			assertTrue(filter.pass(particles(-211, 2212)), "proton present");
			assertFalse(filter.pass(particles(-211, 211)), "neither proton nor electron present");
		});
	}

	@Test void activeFilterWithNoSpeciesChosenPassesEverything() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			assertTrue(filter.pass(particles(211)));
		});
	}

	@Test void activeFilterRejectsAnEventWithNoParticleBank() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			filter.setPids(Set.of(2212));
			assertFalse(filter.pass(EventSnapshot.empty()));
		});
	}

	@Test void settingsPersistAcrossInstances() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/particle-id-filter-" + System.nanoTime());
		try {
			ParticleIdFilter first = new ParticleIdFilter(preferences);
			first.setActive(true);
			first.setPids(Set.of(2212, -11));

			ParticleIdFilter reloaded = new ParticleIdFilter(preferences);
			assertTrue(reloaded.isActive());
			assertEquals(Set.of(2212, -11), reloaded.pids());
		} finally {
			preferences.removeNode();
		}
	}

	private static void withFilter(java.util.function.Consumer<ParticleIdFilter> body) throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/particle-id-filter-" + System.nanoTime());
		try {
			body.accept(new ParticleIdFilter(preferences));
		} finally {
			preferences.removeNode();
		}
	}

	private static EventSnapshot particles(int... pids) {
		DataBank bank = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getColumnList" -> new String[] { "pid" };
					case "rows" -> pids.length;
					case "getInt" -> pids[(int) args[1]];
					default -> null;
				});
		Map<String, DataBank> banks = Map.of("REC::Particle", bank);
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
