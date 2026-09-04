package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.jlab.io.base.DataBank;
import org.junit.jupiter.api.Test;

class RunTriggerTest {

	@Test
	void readsIdAndWidensTheTriggerWordAsUnsigned() {
		String[] columns = { "id", "trigger" };
		DataBank bank = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (object, method, args) -> switch (method.getName()) {
					case "rows" -> 1;
					case "getColumnList" -> columns;
					// 0x80000000 as a raw int is negative; the unsigned word is bit 31 alone
					case "getInt" -> "id".equals(args[0]) ? 7 : 0x80000000;
					default -> null;
				});

		RunTrigger trigger = RunTrigger.fromBank(bank).orElseThrow();
		assertEquals(7, trigger.id());
		assertEquals(0x80000000L, trigger.trigger());
		assertTrue(trigger.bit(31));
		assertFalse(trigger.bit(0));
	}

	@Test
	void bitTestsEveryBitOfAKnownWord() {
		// 8385 = 2^13 + 2^7 + 2^6 + 2^0
		RunTrigger trigger = new RunTrigger(0, 8385L);
		assertTrue(trigger.bit(13));
		assertTrue(trigger.bit(7));
		assertTrue(trigger.bit(6));
		assertTrue(trigger.bit(0));
		for (int i = 0; i < 32; i++) {
			if (i != 13 && i != 7 && i != 6 && i != 0) {
				assertFalse(trigger.bit(i), "bit " + i + " should be unset");
			}
		}
	}

	@Test
	void missingBankOrColumnYieldsEmpty() {
		assertTrue(RunTrigger.from(null).isEmpty());
		assertTrue(RunTrigger.from(EventSnapshot.empty()).isEmpty());

		DataBank incomplete = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (object, method, args) -> switch (method.getName()) {
					case "rows" -> 1;
					case "getColumnList" -> new String[] { "id" };
					default -> null;
				});
		assertTrue(RunTrigger.fromBank(incomplete).isEmpty());

		DataBank empty = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (object, method, args) -> switch (method.getName()) {
					case "rows" -> 0;
					case "getColumnList" -> new String[] { "id", "trigger" };
					default -> null;
				});
		assertTrue(RunTrigger.fromBank(empty).isEmpty());
	}

	@Test
	void idDefaultsToZeroWhenColumnIsAbsent() {
		DataBank bank = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (object, method, args) -> switch (method.getName()) {
					case "rows" -> 1;
					case "getColumnList" -> new String[] { "trigger" };
					case "getInt" -> 8385;
					default -> null;
				});
		RunTrigger trigger = RunTrigger.fromBank(bank).orElseThrow();
		assertEquals(0, trigger.id());
		assertEquals(8385L, trigger.trigger());
	}
}
