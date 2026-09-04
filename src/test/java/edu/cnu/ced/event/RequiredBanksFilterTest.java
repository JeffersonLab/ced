package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

class RequiredBanksFilterTest {

	@Test void inactiveFilterPassesEvenWithoutTheRequiredBanks() throws Exception {
		withFilter(filter -> {
			filter.setRequiredBanks(Set.of("CND::adc"));
			assertTrue(filter.pass(snapshot()));
		});
	}

	@Test void activeFilterRequiresEveryListedBank() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			filter.setRequiredBanks(Set.of("CND::adc", "REC::Particle"));
			assertFalse(filter.pass(snapshot("CND::adc")), "missing REC::Particle");
			assertTrue(filter.pass(snapshot("CND::adc", "REC::Particle")));
			assertTrue(filter.pass(snapshot("CND::adc", "REC::Particle", "DC::tdc")), "extra banks are fine");
		});
	}

	@Test void activeFilterWithNoBanksChosenPassesEverything() throws Exception {
		withFilter(filter -> {
			filter.setActive(true);
			assertTrue(filter.pass(snapshot()));
		});
	}

	@Test void settingsPersistAcrossInstances() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/required-banks-filter-" + System.nanoTime());
		try {
			RequiredBanksFilter first = new RequiredBanksFilter(preferences);
			first.setActive(true);
			first.setRequiredBanks(Set.of("CND::adc", "BST::adc"));

			RequiredBanksFilter reloaded = new RequiredBanksFilter(preferences);
			assertTrue(reloaded.isActive());
			assertEquals(Set.of("CND::adc", "BST::adc"), reloaded.requiredBanks());
		} finally {
			preferences.removeNode();
		}
	}

	private static void withFilter(java.util.function.Consumer<RequiredBanksFilter> body) throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/required-banks-filter-" + System.nanoTime());
		try {
			body.accept(new RequiredBanksFilter(preferences));
		} finally {
			preferences.removeNode();
		}
	}

	private static EventSnapshot snapshot(String... bankNames) {
		Map<String, org.jlab.io.base.DataBank> banks = new java.util.HashMap<>();
		for (String name : bankNames) {
			banks.put(name, (org.jlab.io.base.DataBank) Proxy.newProxyInstance(
					org.jlab.io.base.DataBank.class.getClassLoader(),
					new Class<?>[] { org.jlab.io.base.DataBank.class }, (proxy, method, args) -> null));
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
