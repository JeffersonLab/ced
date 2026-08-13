package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

class EventSnapshotTest {

	@Test
	void capturesSortedDistinctBankMembershipWithoutMutatingSource() {
		String[] names = { "REC::Particle", "BMT::adc", "REC::Particle", null, "" };
		DataEvent event = event(names, null);

		EventSnapshot snapshot = EventSnapshot.of(event);

		assertEquals(List.of("BMT::adc", "REC::Particle"), snapshot.bankNames());
		assertEquals("REC::Particle", names[0]);
		assertTrue(snapshot.hasEvent());
		assertTrue(snapshot.hasBank("BMT::adc"));
		assertFalse(snapshot.hasBank("FMT::adc"));
	}

	@Test
	void resolvesOnlyBanksCapturedInSnapshot() {
		DataBank bank = proxy(DataBank.class);
		EventSnapshot snapshot = EventSnapshot.of(event(new String[] { "REC::Particle" }, bank));

		assertSame(bank, snapshot.bank("REC::Particle").orElseThrow());
		assertTrue(snapshot.bank("MISSING::Bank").isEmpty());
	}

	private static DataEvent event(String[] names, DataBank bank) {
		return (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class }, (instance, method, args) -> switch (method.getName()) {
					case "getBankList" -> names;
					case "hasBank" -> Arrays.asList(names).contains(args[0]);
					case "getBank" -> bank;
					default -> null;
				});
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
				(instance, method, args) -> null);
	}
}
